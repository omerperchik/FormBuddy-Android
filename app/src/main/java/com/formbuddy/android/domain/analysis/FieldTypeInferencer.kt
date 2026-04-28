package com.formbuddy.android.domain.analysis

import android.content.Context
import com.formbuddy.android.R
import com.formbuddy.android.data.model.FieldSubType
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Mirrors iOS `FieldTypeInferencer` (Fillin/Shared/FormAnalysis/FieldTypeInferencer.swift).
 *
 * Multi-stage matching:
 *   1. Normalize the label (lowercase, strip diacritics, collapse whitespace, tokenize).
 *   2. Subtype pass: score each FieldSubType pattern against tokens / trailing segment.
 *   3. Type pass: score each FieldType pattern.
 *   4. Numeric-hint fallback: if any token looks numeric, return NUMBER (low confidence).
 *   5. Default: TEXT (medium confidence).
 *
 * Keyword lists are loaded from `R.string.hint_*` so each locale can override them via
 * `values-XX/strings.xml`. The English defaults always remain as a fallback because the
 * PDF labels we see may be English even in non-English UIs.
 */
@Singleton
class FieldTypeInferencer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class Inference(
        val type: FieldType,
        val subType: FieldSubType?,
        val confidence: Double
    )

    /** Convenience for batch processing — populates a template in place. */
    fun inferFieldTypes(template: FormTemplate): FormTemplate {
        template.allFields.forEach { field ->
            val inference = infer(field.label)
            // Don't override widget-derived types like SIGNATURE / CHECKBOX.
            if (field.fieldType == FieldType.TEXT) {
                field.fieldType = inference.type
            }
            if (field.fieldSubType == null) {
                field.fieldSubType = inference.subType
            }
            if (field.confidence == null) field.confidence = inference.confidence
        }
        return template
    }

    fun infer(rawLabel: String): Inference {
        val label = normalize(rawLabel)
        if (label.isBlank()) return Inference(FieldType.TEXT, null, 0.0)

        val tokens = label.split(NON_ALPHANUMERIC).filter { it.isNotBlank() }
        val trailing = trailingSegment(rawLabel)
            .let { if (it.isNotBlank()) normalize(it) else label }

        // 1. Subtype pass — most specific wins on ties.
        val subtypeMatch = scoreSubtypes(label, tokens, trailing)
        if (subtypeMatch != null) {
            return Inference(forcedType(subtypeMatch.subType), subtypeMatch.subType, subtypeMatch.score)
        }

        // 2. Type pass.
        val typeMatch = scoreTypes(label, tokens)
        if (typeMatch != null) return Inference(typeMatch.type, null, typeMatch.score)

        // 3. Numeric hint fallback.
        if (tokens.any { it.all(Char::isDigit) } || any(label, hint(R.string.hint_number))) {
            return Inference(FieldType.NUMBER, null, 0.4)
        }

        return Inference(FieldType.TEXT, null, 0.5)
    }

    // -------- Scoring --------

    private data class SubtypeScore(val subType: FieldSubType, val score: Double, val specificity: Int)
    private data class TypeScore(val type: FieldType, val score: Double)

    private fun scoreSubtypes(
        label: String,
        tokens: List<String>,
        trailing: String
    ): SubtypeScore? {
        val candidates = SUBTYPES.mapNotNull { (subType, hintRes, base, specificity) ->
            val hints = hint(hintRes)
            val s = matchScore(label, tokens, trailing, hints, base)
            if (s > 0) SubtypeScore(subType, s, specificity) else null
        }
        if (candidates.isEmpty()) return null
        // Tie-break: higher score, then more specific (postalCode > state > city > country, etc.).
        return candidates.maxWithOrNull(
            compareBy(SubtypeScore::score, SubtypeScore::specificity)
        )
    }

    private fun scoreTypes(label: String, tokens: List<String>): TypeScore? {
        val candidates = TYPES.mapNotNull { (type, hintRes, base) ->
            val hints = hint(hintRes)
            val s = matchScore(label, tokens, label, hints, base)
            if (s > 0) TypeScore(type, s) else null
        }
        return candidates.maxByOrNull { it.score }
    }

    /**
     * Matches `hints` against the label/tokens/trailing-segment.
     * - Exact phrase in label: +1.0 + base
     * - Whole-token match: +0.6 + base
     * - Substring in token: +0.4 + base
     * - Hit in trailing segment: +0.3 boost
     * Returns 0 when no hint matched.
     */
    private fun matchScore(
        label: String,
        tokens: List<String>,
        trailing: String,
        hints: List<String>,
        base: Double
    ): Double {
        var best = 0.0
        for (hint in hints) {
            if (hint.isBlank()) continue
            val h = normalize(hint)
            var s = 0.0
            if (label.contains(h)) s = max(s, base + 1.0)
            if (tokens.any { it == h }) s = max(s, base + 0.6)
            if (tokens.any { it.contains(h) || h.contains(it) }) s = max(s, base + 0.4)
            if (trailing.contains(h)) s += 0.3
            if (s > best) best = s
        }
        return best
    }

    // -------- Resource loading --------

    private val hintCache = mutableMapOf<Int, List<String>>()

    private fun hint(resId: Int): List<String> = hintCache.getOrPut(resId) {
        // Localized hint plus an English fallback so we always recognize English labels.
        val localized = context.getString(resId).split(",").map { it.trim() }
        val englishConfig = android.content.res.Configuration(context.resources.configuration).apply {
            setLocale(java.util.Locale.ENGLISH)
        }
        val englishContext = context.createConfigurationContext(englishConfig)
        val english = englishContext.getString(resId).split(",").map { it.trim() }
        (localized + english).filter { it.isNotBlank() }.distinct()
    }

    // -------- Helpers --------

    private fun normalize(s: String): String {
        val nfd = Normalizer.normalize(s, Normalizer.Form.NFD)
        val stripped = DIACRITIC.replace(nfd, "")
        return stripped.lowercase(java.util.Locale.ROOT).replace(WHITESPACE, " ").trim()
    }

    private fun any(label: String, hints: List<String>): Boolean =
        hints.any { it.isNotBlank() && label.contains(normalize(it)) }

    private fun trailingSegment(s: String): String {
        // After the last `:`, `-`, `/` — what comes last is usually the most specific cue.
        val splitChar = SEGMENT_SPLITS.findAll(s).lastOrNull()?.value ?: return s
        return s.substringAfterLast(splitChar)
    }

    private fun forcedType(subType: FieldSubType): FieldType = when (subType) {
        FieldSubType.BIRTH_DATE -> FieldType.DATE
        FieldSubType.HOME_ADDRESS,
        FieldSubType.WORK_ADDRESS,
        FieldSubType.CITY,
        FieldSubType.STATE,
        FieldSubType.COUNTRY,
        FieldSubType.POSTAL_CODE -> FieldType.ADDRESS
        FieldSubType.WEBSITE -> FieldType.TEXT
        else -> FieldType.TEXT
    }

    companion object {
        private val NON_ALPHANUMERIC = Regex("[^\\p{L}\\p{N}]+")
        private val DIACRITIC = Regex("\\p{Mn}+")
        private val WHITESPACE = Regex("\\s+")
        private val SEGMENT_SPLITS = Regex("[:\\-/]")

        // (subtype, hintRes, base confidence, specificity score for tie-break)
        // Higher specificity wins ties. Mirrors iOS logic where postalCode > state > city > country, etc.
        private val SUBTYPES: List<Quad<FieldSubType, Int, Double, Int>> = listOf(
            Quad(FieldSubType.POSTAL_CODE,         R.string.hint_postal_code, 0.9, 100),
            Quad(FieldSubType.BIRTH_DATE,          R.string.hint_birth_date,  0.85, 90),
            Quad(FieldSubType.TAX_ID,              R.string.hint_tax_id,      0.85, 80),
            Quad(FieldSubType.WEBSITE,             R.string.hint_website,     0.8, 70),
            Quad(FieldSubType.REGISTRATION_NUMBER, R.string.hint_registration, 0.7, 65),
            Quad(FieldSubType.COMPANY_NAME,        R.string.hint_company,     0.75, 60),
            Quad(FieldSubType.INDUSTRY,            R.string.hint_industry,    0.7, 55),
            Quad(FieldSubType.HOME_ADDRESS,        R.string.hint_home_address, 0.8, 50),
            Quad(FieldSubType.WORK_ADDRESS,        R.string.hint_work_address, 0.8, 45),
            Quad(FieldSubType.STATE,               R.string.hint_state,       0.75, 40),
            Quad(FieldSubType.CITY,                R.string.hint_city,        0.75, 35),
            Quad(FieldSubType.COUNTRY,             R.string.hint_country,     0.7, 30),
            Quad(FieldSubType.LAST_NAME,           R.string.hint_last_name,   0.85, 25),
            Quad(FieldSubType.MIDDLE_NAME,         R.string.hint_middle_name, 0.8, 20),
            Quad(FieldSubType.FIRST_NAME,          R.string.hint_first_name,  0.85, 15)
        )

        private val TYPES: List<Triple<FieldType, Int, Double>> = listOf(
            Triple(FieldType.EMAIL,     R.string.hint_email, 0.85),
            Triple(FieldType.PHONE,     R.string.hint_phone, 0.85),
            Triple(FieldType.DATE,      R.string.hint_date, 0.8),
            Triple(FieldType.SIGNATURE, R.string.hint_signature, 0.9),
            Triple(FieldType.ADDRESS,   R.string.hint_address, 0.8)
        )
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
