package com.formbuddy.android.ui.screens.settings

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.data.model.FormMode
import com.formbuddy.android.data.remote.firebase.FirebaseManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val firebaseManager: FirebaseManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val formMode: StateFlow<FormMode> = preferencesManager.defaultFormMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FormMode.CHAT)

    val assistantVoice: StateFlow<String> = preferencesManager.assistantVoice
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "alloy")

    private val _playingVoice = MutableStateFlow<String?>(null)
    val playingVoice: StateFlow<String?> = _playingVoice.asStateFlow()

    private var samplePlayer: MediaPlayer? = null
    private var sampleJob: Job? = null

    fun setFormMode(mode: FormMode) {
        viewModelScope.launch { preferencesManager.setDefaultFormMode(mode) }
    }

    fun setAssistantVoice(voice: String) {
        viewModelScope.launch { preferencesManager.setAssistantVoice(voice) }
    }

    /** Streams a sample of [voice] via the Firebase TTS callable, mirroring
     *  iOS where the Settings → Assistant Voice list lets you preview each
     *  preset before selecting. */
    fun playVoiceSample(voice: String) {
        stopVoiceSample()
        sampleJob = viewModelScope.launch {
            val tmp = try {
                val mp3 = withContext(Dispatchers.IO) {
                    firebaseManager.getTextToSpeechAudio(
                        text = "Hi, I'm $voice. I'll help you fill forms by voice.",
                        voice = voice
                    )
                }
                File.createTempFile("voice-sample-${UUID.randomUUID()}", ".mp3", context.cacheDir).apply {
                    writeBytes(mp3)
                }
            } catch (_: Throwable) {
                _playingVoice.value = null
                return@launch
            }
            samplePlayer = MediaPlayer().apply {
                setDataSource(tmp.absolutePath)
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    runCatching { it.release() }
                    samplePlayer = null
                    _playingVoice.value = null
                    tmp.delete()
                }
                setOnErrorListener { mp, _, _ ->
                    runCatching { mp.release() }
                    samplePlayer = null
                    _playingVoice.value = null
                    tmp.delete()
                    true
                }
                prepareAsync()
            }
            _playingVoice.value = voice
        }
    }

    fun stopVoiceSample() {
        sampleJob?.cancel()
        sampleJob = null
        runCatching { samplePlayer?.stop() }
        runCatching { samplePlayer?.release() }
        samplePlayer = null
        _playingVoice.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopVoiceSample()
    }
}
