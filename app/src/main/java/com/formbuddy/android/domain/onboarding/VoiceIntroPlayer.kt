package com.formbuddy.android.domain.onboarding

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the localized voice intro on the onboarding profile-setup page.
 *
 * iOS ships pre-recorded `.m4a` files under
 * `Fillin/SupportingFiles/Onboarding/ProfileVoiceAssets/{locale}/`. The Android
 * codebase loads the same files from `assets/onboarding/voice/{locale}.m4a`
 * when present; if the locale isn't bundled (because the user hasn't dropped
 * the recordings in yet), we fall back to the system [TextToSpeech] engine
 * and read the localized [introText] string aloud.
 *
 * To bundle real voice files later: drop them in
 *   `app/src/main/assets/onboarding/voice/{en,he,es,...}.m4a`
 * — no other code changes required.
 */
@Singleton
class VoiceIntroPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null

    fun play(locale: Locale, introText: String) {
        stop()
        val assetPath = "onboarding/voice/${locale.language}.m4a"
        val hasAsset = runCatching {
            context.assets.openFd(assetPath).also { it.close() }
            true
        }.getOrDefault(false)

        if (hasAsset) {
            player = MediaPlayer().apply {
                val afd = context.assets.openFd(assetPath)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } else {
            tts = TextToSpeech(context) { status ->
                if (status != TextToSpeech.SUCCESS) return@TextToSpeech
                tts?.language = locale
                tts?.speak(introText, TextToSpeech.QUEUE_FLUSH, null, INTRO_UTTERANCE)
            }
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
    }

    companion object {
        private const val INTRO_UTTERANCE = "voice_intro"
    }
}
