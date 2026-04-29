package com.formbuddy.android.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.domain.onboarding.VoiceIntroPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val voiceIntroPlayer: VoiceIntroPlayer
) : ViewModel() {

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesManager.setDidShowOnboarding(true)
        }
    }

    /** Plays the iOS opening voice prompt for the user's locale. */
    fun playOpeningPrompt(fallbackText: String) {
        voiceIntroPlayer.play(
            prompt = VoiceIntroPlayer.Prompt.Opening,
            locale = Locale.getDefault(),
            fallbackText = fallbackText
        )
    }

    fun stopVoiceIntro() {
        voiceIntroPlayer.stop()
    }

    override fun onCleared() {
        super.onCleared()
        voiceIntroPlayer.stop()
    }
}
