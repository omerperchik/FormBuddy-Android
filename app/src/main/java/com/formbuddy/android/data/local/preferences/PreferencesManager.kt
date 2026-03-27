package com.formbuddy.android.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.formbuddy.android.data.model.FormMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "formbuddy_settings")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        val KEY_DID_SHOW_ONBOARDING = booleanPreferencesKey("did_show_onboarding")
        val KEY_DEFAULT_FORM_MODE = stringPreferencesKey("default_form_mode")
        val KEY_ASSISTANT_VOICE = stringPreferencesKey("assistant_voice")
        val KEY_IS_EDITOR_MODE = booleanPreferencesKey("is_editor_mode")
        val KEY_DEVICE_ID = stringPreferencesKey("device_id")
        val KEY_FORMS_COMPLETED_COUNT = intPreferencesKey("forms_completed_count")
        val KEY_VOICE_USAGE_COUNT = intPreferencesKey("voice_usage_count")
        val KEY_LIBRARY_USAGE_COUNT = intPreferencesKey("library_usage_count")
        val KEY_UPLOAD_USAGE_COUNT = intPreferencesKey("upload_usage_count")
        val KEY_SCAN_USAGE_COUNT = intPreferencesKey("scan_usage_count")
        val KEY_DID_SHOW_FIRST_SAVE_CELEBRATION = booleanPreferencesKey("did_show_first_save_celebration")
        val KEY_IS_LOCAL_PROCESSING_ENABLED = booleanPreferencesKey("is_local_processing_enabled")
        val KEY_FIRST_FORM_SOURCE = stringPreferencesKey("first_form_source")
        val KEY_LAST_REVIEW_REQUEST = longPreferencesKey("last_review_request")
    }

    val didShowOnboarding: Flow<Boolean> = context.dataStore.data.map { it[KEY_DID_SHOW_ONBOARDING] ?: false }
    val defaultFormMode: Flow<FormMode> = context.dataStore.data.map {
        try { FormMode.valueOf(it[KEY_DEFAULT_FORM_MODE] ?: "CHAT") } catch (_: Exception) { FormMode.CHAT }
    }
    val assistantVoice: Flow<String> = context.dataStore.data.map { it[KEY_ASSISTANT_VOICE] ?: "alloy" }
    val isEditorMode: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_EDITOR_MODE] ?: false }
    val didShowFirstSaveCelebration: Flow<Boolean> = context.dataStore.data.map { it[KEY_DID_SHOW_FIRST_SAVE_CELEBRATION] ?: false }

    suspend fun setDidShowOnboarding(value: Boolean) {
        context.dataStore.edit { it[KEY_DID_SHOW_ONBOARDING] = value }
    }

    suspend fun setDefaultFormMode(mode: FormMode) {
        context.dataStore.edit { it[KEY_DEFAULT_FORM_MODE] = mode.name }
    }

    suspend fun setAssistantVoice(voice: String) {
        context.dataStore.edit { it[KEY_ASSISTANT_VOICE] = voice }
    }

    suspend fun setEditorMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_IS_EDITOR_MODE] = enabled }
    }

    suspend fun setDidShowFirstSaveCelebration(value: Boolean) {
        context.dataStore.edit { it[KEY_DID_SHOW_FIRST_SAVE_CELEBRATION] = value }
    }

    suspend fun setDeviceId(id: String) {
        context.dataStore.edit { it[KEY_DEVICE_ID] = id }
    }

    suspend fun incrementUsage(key: Preferences.Key<Int>) {
        context.dataStore.edit { prefs ->
            prefs[key] = (prefs[key] ?: 0) + 1
        }
    }

    fun getUsageCount(key: Preferences.Key<Int>): Flow<Int> {
        return context.dataStore.data.map { it[key] ?: 0 }
    }
}
