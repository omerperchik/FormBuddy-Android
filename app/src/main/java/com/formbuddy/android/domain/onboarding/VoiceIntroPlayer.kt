package com.formbuddy.android.domain.onboarding

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays the localized voice intro for each profile field during onboarding.
 *
 * Reuses the **exact** mp3 assets shipped with the iOS app
 * (`Fillin/SupportingFiles/Onboarding/ProfileVoiceAssets/{locale}/profile_onboarding_transcript_{field}_{lang}.mp3`).
 * We copied them into `app/src/main/assets/onboarding/voice/{locale}/`
 * unchanged so both platforms read from the same source-of-truth recordings.
 *
 * Locale folder naming follows iOS (`pt_br`, `pt_pt`, `zh_hans`) — the
 * [iosLocaleFolder] mapping converts an Android `Locale` to that name.
 *
 * On unsupported locales or missing assets we fall back to the system
 * [TextToSpeech] engine so the screen still narrates something.
 */
@Singleton
class VoiceIntroPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null

    enum class Prompt(val key: String) {
        Opening("opening_prompt"),
        FirstName("first_name"),
        MiddleName("middle_name"),
        LastName("last_name"),
        Email("email"),
        Phone("phone"),
        BirthDate("birth_date"),
        HomeAddress("home_address"),
        WorkAddress("work_address"),
        City("city"),
        State("state"),
        Country("country"),
        PostalCode("postal_code"),
        Signature("signature"),
        Closing("closing_prompt"),
        NoMissingFields("no_missing_fields_prompt"),
        RepeatUnclear("repeat_unclear_input"),
        SkipUnclear("skipping_unclear_input")
    }

    /**
     * Plays a [Prompt] in the [locale]. If the asset can't be loaded
     * (locale not bundled, file missing), falls back to TTS reading
     * [fallbackText] aloud.
     */
    fun play(prompt: Prompt, locale: Locale, fallbackText: String) {
        stop()
        val folder = iosLocaleFolder(locale)
        val lang = iosLangCode(locale)
        val assetPath = "onboarding/voice/$folder/profile_onboarding_transcript_${prompt.key}_$lang.mp3"

        val opened = runCatching {
            val afd = context.assets.openFd(assetPath)
            player = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setOnPreparedListener { it.start() }
                setOnCompletionListener { release(); player = null }
                prepareAsync()
            }
            true
        }.getOrDefault(false)

        if (!opened) playTtsFallback(locale, fallbackText)
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { tts?.stop() }
        runCatching { tts?.shutdown() }
        tts = null
    }

    private fun playTtsFallback(locale: Locale, text: String) {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) return@TextToSpeech
            tts?.language = locale
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, INTRO_UTTERANCE)
        }
    }

    /** Maps `Locale` to the iOS folder name used inside the `assets/onboarding/voice/` tree. */
    private fun iosLocaleFolder(locale: Locale): String = when (locale.language) {
        "en" -> "en"
        "he", "iw" -> "he" // iOS uses `he`; Android historically `iw`
        "es" -> "es"
        "fr" -> "fr"
        "de" -> "de"
        "ru" -> "ru"
        "ja" -> "ja"
        "ar" -> "ar"
        "hi" -> "hi"
        "pt" -> if (locale.country.equals("BR", ignoreCase = true)) "pt_br" else "pt_pt"
        "zh" -> "zh_hans"
        else -> "en" // fall through to English assets
    }

    /** Filename suffix per iOS naming convention. */
    private fun iosLangCode(locale: Locale): String = when (locale.language) {
        "en" -> "en"
        "he", "iw" -> "he"
        "es" -> "es"
        "fr" -> "fr"
        "de" -> "de"
        "ru" -> "ru"
        "ja" -> "ja"
        "ar" -> "ar"
        "hi" -> "hi"
        "pt" -> if (locale.country.equals("BR", ignoreCase = true)) "pt_br" else "pt_pt"
        "zh" -> "zh_hans"
        else -> "en"
    }

    companion object {
        private const val INTRO_UTTERANCE = "voice_intro"
    }
}
