package com.formbuddy.android.domain.learning

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.formbuddy.android.data.model.FieldSubType
import com.formbuddy.android.data.model.FormField
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.days

private val Context.learnStore by preferencesDataStore("profile_learning")

/**
 * Sidecar store that turns the static `Profile` row into a profile that
 * actually learns from real form-filling sessions.
 *
 * It tracks two things per [FieldSubType]:
 *   1. **Frequency**: a count of how often a given value was committed for
 *      that subtype, so the suggestion ranker can prefer "Omer Perchik" over
 *      "Omer Per Chik" once the corrected spelling has won enough times.
 *   2. **Last-touched timestamp**: when the user last confirmed or edited
 *      this subtype, so the UI can surface stale fields ("Your address
 *      hasn't been touched in 14 months — still right?").
 *
 * The profile-quality score on the home screen is derived from the freshness
 * + the suggestion-acceptance rate, replacing the iOS "% of fields filled"
 * metric which is binary and uninformative once the profile is set up.
 */
@Singleton
class ProfileLearner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    data class ValueStat(
        val value: String,
        val acceptedCount: Int,
        val rejectedCount: Int,
        val lastSeenMs: Long
    ) {
        /**
         * Suggestion ranking score: acceptance rate weighted by recency.
         * Higher → more likely to be the user's preferred value.
         */
        fun score(): Float {
            val total = acceptedCount + rejectedCount
            if (total == 0) return 0f
            val ageDays = (System.currentTimeMillis() - lastSeenMs) / 86_400_000.0
            val recency = (1.0 - ageDays / 365.0).coerceIn(0.0, 1.0)
            return (acceptedCount.toFloat() / total) + 0.1f * recency.toFloat()
        }
    }

    data class SubtypeMemory(
        val subType: FieldSubType,
        val values: List<ValueStat>,
        val lastConfirmedMs: Long
    ) {
        val topValue: String? get() = values.maxByOrNull { it.score() }?.value
        val freshness: Float
            get() {
                val ageMs = System.currentTimeMillis() - lastConfirmedMs
                val staleAfter = STALE_AFTER.inWholeMilliseconds.toFloat()
                return (1f - ageMs.toFloat() / staleAfter).coerceIn(0f, 1f)
            }
    }

    /** Snapshot of all subtype memory. */
    val state: Flow<Map<FieldSubType, SubtypeMemory>> = context.learnStore.data.map { prefs ->
        val raw = prefs[KEY] ?: return@map emptyMap()
        runCatching {
            gson.fromJson<Map<FieldSubType, SubtypeMemory>>(
                raw,
                object : TypeToken<Map<FieldSubType, SubtypeMemory>>() {}.type
            )
        }.getOrDefault(emptyMap())
    }

    /** Called when the user confirms a value for a field (typed it themselves
     *  or accepted a suggestion). */
    suspend fun recordAcceptance(field: FormField, value: String) =
        bump(field.fieldSubType, value, accepted = true)

    /** Called when the user rejects/edits a suggestion we offered. */
    suspend fun recordRejection(field: FormField, suggestedValue: String) =
        bump(field.fieldSubType, suggestedValue, accepted = false)

    /** Returns the memory's preferred value for a subtype, if any has won enough. */
    suspend fun bestSuggestion(subType: FieldSubType?): String? {
        if (subType == null) return null
        val mem = state.first()[subType] ?: return null
        return mem.topValue
    }

    /** Subtypes that haven't been confirmed in [STALE_AFTER]. */
    suspend fun staleSubtypes(): List<SubtypeMemory> =
        state.first().values.filter { it.freshness <= 0f }

    /** Aggregate quality score for the home screen — replaces "% complete". */
    suspend fun qualityScore(): Float {
        val all = state.first().values
        if (all.isEmpty()) return 0f
        val freshness = all.map { it.freshness }.average().toFloat()
        val acceptance = all.flatMap { it.values }.let { stats ->
            val acc = stats.sumOf { it.acceptedCount }
            val rej = stats.sumOf { it.rejectedCount }
            if (acc + rej == 0) 1f else acc.toFloat() / (acc + rej)
        }
        return (0.6f * freshness + 0.4f * acceptance).coerceIn(0f, 1f)
    }

    private suspend fun bump(subType: FieldSubType?, value: String, accepted: Boolean) {
        if (subType == null || value.isBlank()) return
        context.learnStore.edit { prefs ->
            val current = prefs[KEY]?.let {
                runCatching {
                    gson.fromJson<Map<FieldSubType, SubtypeMemory>>(
                        it,
                        object : TypeToken<Map<FieldSubType, SubtypeMemory>>() {}.type
                    )
                }.getOrDefault(emptyMap())
            } ?: emptyMap()

            val now = System.currentTimeMillis()
            val mem = current[subType]
            val newValues = (mem?.values ?: emptyList()).toMutableList()
            val idx = newValues.indexOfFirst { it.value.equals(value, ignoreCase = true) }
            if (idx >= 0) {
                val v = newValues[idx]
                newValues[idx] = v.copy(
                    acceptedCount = v.acceptedCount + if (accepted) 1 else 0,
                    rejectedCount = v.rejectedCount + if (accepted) 0 else 1,
                    lastSeenMs = now
                )
            } else {
                newValues += ValueStat(
                    value = value,
                    acceptedCount = if (accepted) 1 else 0,
                    rejectedCount = if (accepted) 0 else 1,
                    lastSeenMs = now
                )
            }
            val newMem = SubtypeMemory(
                subType = subType,
                values = newValues.sortedByDescending { it.score() },
                lastConfirmedMs = if (accepted) now else (mem?.lastConfirmedMs ?: now)
            )
            val updated = current.toMutableMap().apply { put(subType, newMem) }
            prefs[KEY] = gson.toJson(updated)
        }
    }

    companion object {
        private val KEY = stringPreferencesKey("subtype_memory_json")
        val STALE_AFTER = 365.days
    }
}
