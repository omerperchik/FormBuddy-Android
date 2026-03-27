package com.formbuddy.android.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.data.model.FormMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val formMode: StateFlow<FormMode> = preferencesManager.defaultFormMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormMode.CHAT)

    val assistantVoice: StateFlow<String> = preferencesManager.assistantVoice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "alloy")

    fun setFormMode(mode: FormMode) {
        viewModelScope.launch { preferencesManager.setDefaultFormMode(mode) }
    }

    fun setAssistantVoice(voice: String) {
        viewModelScope.launch { preferencesManager.setAssistantVoice(voice) }
    }
}
