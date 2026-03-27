package com.formbuddy.android.domain.tts

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.data.remote.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextToSpeechService @Inject constructor(
    private val context: Context,
    private val firebaseManager: FirebaseManager,
    private val preferencesManager: PreferencesManager
) {
    private var androidTts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isTtsReady = false

    init {
        androidTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                androidTts?.language = Locale.getDefault()
                isTtsReady = true
            }
        }
    }

    suspend fun speak(text: String) {
        val voice = preferencesManager.assistantVoice.first()
        val locale = Locale.getDefault().language

        try {
            // Try cloud TTS first
            val audioData = firebaseManager.getTextToSpeechAudio(text, voice, locale)
            playAudioData(audioData)
        } catch (_: Exception) {
            // Fallback to device TTS
            speakWithDeviceTts(text)
        }
    }

    private suspend fun playAudioData(data: ByteArray) = withContext(Dispatchers.IO) {
        val tempFile = File.createTempFile("tts_", ".mp3", context.cacheDir)
        FileOutputStream(tempFile).use { it.write(data) }

        withContext(Dispatchers.Main) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    tempFile.delete()
                    release()
                }
            }
        }
    }

    private fun speakWithDeviceTts(text: String) {
        if (isTtsReady) {
            androidTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    fun shutdown() {
        androidTts?.shutdown()
        mediaPlayer?.release()
    }
}
