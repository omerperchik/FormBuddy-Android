package com.formbuddy.android.data.metrics

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val Context.timeSavedStore by preferencesDataStore("time_saved")

/**
 * Tracks the cumulative "minutes saved" headline number that the home screen
 * and loading screens reuse instead of generic spinners.
 *
 * Calibration: a baseline manual-fill estimate is **15 seconds per field**
 * (typing, tabbing, double-checking on a phone). A FormBuddy-completed field
 * costs roughly **2 seconds** on average (voice / suggestion / autofill), so
 * the saving per filled field is ~13 seconds. Tunable via [PER_FIELD_SAVING].
 */
@Singleton
class TimeSavedTracker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val totalSavedSeconds: Flow<Long> =
        context.timeSavedStore.data.map { it[KEY_TOTAL_SECONDS] ?: 0L }

    suspend fun recordCompletion(fieldsFilled: Int) {
        val saved = (PER_FIELD_SAVING * fieldsFilled).inWholeSeconds
        context.timeSavedStore.edit {
            it[KEY_TOTAL_SECONDS] = (it[KEY_TOTAL_SECONDS] ?: 0L) + saved
        }
    }

    /** Estimate the seconds the user would have spent filling [fieldCount] fields by hand. */
    fun manualEstimate(fieldCount: Int): Duration = MANUAL_PER_FIELD * fieldCount

    /** Estimate the seconds FormBuddy will spend on [fieldCount] fields. */
    fun estimatedAssistedDuration(fieldCount: Int): Duration = ASSISTED_PER_FIELD * fieldCount

    fun savingFor(fieldCount: Int): Duration = manualEstimate(fieldCount) - estimatedAssistedDuration(fieldCount)

    companion object {
        // Calibration knobs.
        val MANUAL_PER_FIELD: Duration = 15.seconds
        val ASSISTED_PER_FIELD: Duration = 2.seconds
        val PER_FIELD_SAVING: Duration = MANUAL_PER_FIELD - ASSISTED_PER_FIELD

        private val KEY_TOTAL_SECONDS = longPreferencesKey("total_saved_seconds")
    }
}

/** "4 min 12 s" / "12 s" / "1 h 3 min" etc. */
fun Duration.formatHuman(): String {
    val totalSeconds = inWholeSeconds
    if (totalSeconds < 60) return "${totalSeconds}s"
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    if (minutes < 60) return if (seconds == 0L) "${minutes} min" else "${minutes} min ${seconds}s"
    val hours = minutes / 60
    val remMin = minutes % 60
    return if (remMin == 0L) "${hours} h" else "${hours} h ${remMin} min"
}
