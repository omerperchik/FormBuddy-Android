package com.formbuddy.android.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.text.InputType
import android.view.View

/**
 * Classifies a [AssistStructure.ViewNode] into a [Hint] we know how to fill.
 *
 * Tries multiple signals because real-world apps are messy:
 *   1. Native Android `autofillHints` (the ground truth when present).
 *   2. `hint` text on the view (HTML form labels, EditText hints).
 *   3. The view's resource id name (often hand-set, e.g. `etEmail`).
 *   4. Web-domain attributes for WebView fields (`htmlInfo`).
 *   5. Input type fallback (PHONE / EMAIL / DATE input types).
 *
 * Conservative: returns null when nothing is confident.
 */
object AutofillFieldClassifier {

    enum class Hint(val displayName: String) {
        GIVEN_NAME("First name"),
        MIDDLE_NAME("Middle name"),
        FAMILY_NAME("Last name"),
        FULL_NAME("Full name"),
        EMAIL("Email"),
        PHONE("Phone"),
        BIRTH_DATE("Birth date"),
        STREET_ADDRESS("Address"),
        CITY("City"),
        REGION("State / region"),
        COUNTRY("Country"),
        POSTAL_CODE("Postal code")
    }

    fun classify(node: AssistStructure.ViewNode): Hint? {
        // 1. Native autofillHints are the most reliable.
        node.autofillHints?.firstNotNullOfOrNull { mapNative(it) }?.let { return it }

        // 2. Hint text (EditText.setHint or aria-label inside WebView).
        node.hint?.toString()?.let { mapKeyword(it) }?.let { return it }

        // 3. Resource id name (often `etPhone`, `inp_first_name`, etc).
        node.idEntry?.let { mapKeyword(it) }?.let { return it }

        // 4. WebView attributes — name=, autocomplete=, id=, placeholder=.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = node.htmlInfo?.attributes
            if (attrs != null) {
                for (i in 0 until attrs.size) {
                    val pair = attrs[i]
                    val key = pair.first
                    val value = pair.second
                    if (key in WEB_ATTR_KEYS) {
                        mapKeyword(value)?.let { return it }
                    }
                }
            }
        }

        // 5. Input-type fallback.
        when (node.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_PHONE -> return Hint.PHONE
        }
        if (node.inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS != 0) return Hint.EMAIL
        if (node.inputType and InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS != 0) return Hint.STREET_ADDRESS

        return null
    }

    private fun mapNative(hint: String): Hint? = when (hint) {
        View.AUTOFILL_HINT_NAME -> Hint.FULL_NAME
        View.AUTOFILL_HINT_USERNAME -> null
        View.AUTOFILL_HINT_EMAIL_ADDRESS -> Hint.EMAIL
        View.AUTOFILL_HINT_PHONE -> Hint.PHONE
        View.AUTOFILL_HINT_POSTAL_ADDRESS -> Hint.STREET_ADDRESS
        View.AUTOFILL_HINT_POSTAL_CODE -> Hint.POSTAL_CODE
        // Newer constants (API 28+) referenced by string so we don't shadow on older builds.
        "personGivenName", "given-name" -> Hint.GIVEN_NAME
        "personMiddleName", "additional-name" -> Hint.MIDDLE_NAME
        "personFamilyName", "family-name" -> Hint.FAMILY_NAME
        "addressLocality" -> Hint.CITY
        "addressRegion" -> Hint.REGION
        "addressCountry" -> Hint.COUNTRY
        "addressPostalCode" -> Hint.POSTAL_CODE
        "birthDateFull" -> Hint.BIRTH_DATE
        else -> null
    }

    private fun mapKeyword(input: String): Hint? {
        val s = input.lowercase().trim()
        if (s.isEmpty()) return null
        // Check most specific matches first.
        if (s.matches(POSTAL_CODE_RX)) return Hint.POSTAL_CODE
        for ((rx, hint) in PATTERNS) if (rx.containsMatchIn(s)) return hint
        return null
    }

    private val POSTAL_CODE_RX = Regex("zip|postal[ _-]?code|postcode|post[ _-]?code")
    private val PATTERNS: List<Pair<Regex, Hint>> = listOf(
        Regex("first[ _-]?name|given[ _-]?name|forename") to Hint.GIVEN_NAME,
        Regex("middle[ _-]?name|middle[ _-]?initial") to Hint.MIDDLE_NAME,
        Regex("last[ _-]?name|family[ _-]?name|surname") to Hint.FAMILY_NAME,
        Regex("\\bfull[ _-]?name|\\bname\\b") to Hint.FULL_NAME,
        Regex("e[-_]?mail|inbox") to Hint.EMAIL,
        Regex("phone|mobile|tel\\b|cell|fax") to Hint.PHONE,
        Regex("birth[ _-]?date|dob|date[ _-]?of[ _-]?birth") to Hint.BIRTH_DATE,
        Regex("address[ _-]?line|street|addr1|address1|home[ _-]?address") to Hint.STREET_ADDRESS,
        Regex("\\bcity|town|locality") to Hint.CITY,
        Regex("\\bstate|region|province") to Hint.REGION,
        Regex("country|nation") to Hint.COUNTRY
    )

    private val WEB_ATTR_KEYS = setOf("name", "id", "autocomplete", "placeholder", "aria-label", "type")
}
