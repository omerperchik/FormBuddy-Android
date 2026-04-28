package com.formbuddy.android.data.privacy

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.auditStore by preferencesDataStore("privacy_audit")

/**
 * Append-only audit log of every action that *could* leave the device. The
 * Settings screen renders this as a per-event list so the user can see what
 * was sent, where, and when — and export the file for their own records.
 *
 * Design constraints:
 *   - We never write the field VALUE to the log, only a hash + the
 *     destination + a category. The point is to prove what crossed the
 *     network boundary, not to leak the data we said we kept private.
 *   - The log is bounded ([MAX_ENTRIES]) so it can't grow without bound on
 *     long-running installs.
 */
@Singleton
class PrivacyAuditLog @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {

    enum class Category { OnDevice, Cloud, Telemetry, Storage }

    data class Entry(
        val timestampMs: Long,
        val category: Category,
        val destination: String,
        val description: String,
        val fieldHash: String? = null
    )

    val entries: Flow<List<Entry>> = context.auditStore.data.map { read(it) }

    suspend fun log(
        category: Category,
        destination: String,
        description: String,
        fieldValueForHash: String? = null
    ) {
        val newEntry = Entry(
            timestampMs = System.currentTimeMillis(),
            category = category,
            destination = destination,
            description = description,
            fieldHash = fieldValueForHash?.let { hashShort(it) }
        )
        context.auditStore.edit { prefs ->
            val list = read(prefs).toMutableList()
            list += newEntry
            // Trim oldest first
            while (list.size > MAX_ENTRIES) list.removeAt(0)
            prefs[KEY] = gson.toJson(list)
        }
    }

    suspend fun clear() {
        context.auditStore.edit { it.remove(KEY) }
    }

    suspend fun exportText(): String {
        val list = entries.first()
        return buildString {
            appendLine("# FormBuddy privacy audit log")
            appendLine("# Generated: ${java.time.Instant.now()}")
            appendLine()
            for (e in list) {
                val ts = java.time.Instant.ofEpochMilli(e.timestampMs)
                appendLine("$ts\t[${e.category}]\t${e.destination}\t${e.description}\t${e.fieldHash ?: "-"}")
            }
        }
    }

    private fun read(prefs: Preferences): List<Entry> {
        val raw = prefs[KEY] ?: return emptyList()
        return runCatching {
            gson.fromJson<List<Entry>>(raw, object : TypeToken<List<Entry>>() {}.type)
        }.getOrDefault(emptyList())
    }

    private fun hashShort(value: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(value.toByteArray()).joinToString("") { "%02x".format(it) }.take(16)
    }

    companion object {
        private val KEY = stringPreferencesKey("entries_json")
        private const val MAX_ENTRIES = 500
    }
}
