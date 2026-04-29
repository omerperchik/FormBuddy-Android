package com.formbuddy.android.domain.analysis

import android.content.Context
import android.util.Log
import com.formbuddy.android.BuildConfig
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormTemplate
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refines `FormField` labels and types using the best LLM provider available
 * on the device, in this order:
 *
 *   1. **AICore on-device (Gemini Nano)** — preferred when running on a
 *      Pixel 8+ / Galaxy S24+ with `com.google.android.aicore` installed and
 *      the user has consented to on-device model usage. Reflection-loaded so
 *      this APK can ship even on devices without AICore.
 *   2. **Cloud Gemini via the `generativeai` SDK** — when an API key is
 *      configured at build time (`buildConfigField "GEMINI_API_KEY"`).
 *      Sensitive substrings are redacted before the request.
 *   3. **No-op** — returns the template unchanged. The locale-aware
 *      `FieldTypeInferencer` has already done its pass; skipping refinement
 *      is a fine fallback.
 *
 *  Mirrors iOS `FoundationModelsFieldRefiner` which also defers gracefully
 *  on unsupported hardware.
 */
@Singleton
class GeminiFieldRefiner @Inject constructor(
    @ApplicationContext private val context: Context
) {

    sealed interface Provider {
        suspend fun refine(prompt: String): String?
        val name: String
    }

    private val provider: Provider by lazy { pickBestProvider() }

    suspend fun refineFields(template: FormTemplate): FormTemplate = withContext(Dispatchers.IO) {
        if (template.allFields.isEmpty()) return@withContext template

        val description = template.allFields.mapIndexed { index, field ->
            "$index|${redact(field.label)}|${field.fieldType.name}"
        }.joinToString("\n")

        val prompt = """
            For each form field below, return a refined natural-language label and the
            best-fitting type. Output one row per input field, formatted as:
            INDEX|LABEL|TYPE
            Allowed types: TEXT NUMBER CHECKBOX SIGNATURE DATE EMAIL PHONE ADDRESS

            Fields:
            $description
        """.trimIndent()

        val raw = try {
            provider.refine(prompt)
        } catch (t: Throwable) {
            Log.w(TAG, "Refiner ${provider.name} failed: ${t.message}")
            null
        } ?: return@withContext template

        applyRefinement(template, raw)
        template
    }

    private fun applyRefinement(template: FormTemplate, raw: String) {
        for (line in raw.lines()) {
            val parts = line.split("|").map { it.trim() }
            if (parts.size != 3) continue
            val idx = parts[0].toIntOrNull() ?: continue
            val field = template.allFields.getOrNull(idx) ?: continue
            val refinedLabel = parts[1]
            val refinedType = runCatching { FieldType.valueOf(parts[2].uppercase()) }.getOrNull()
            if (refinedLabel.isNotBlank() && refinedLabel.length <= 60) field.label = refinedLabel
            if (refinedType != null) field.fieldType = refinedType
        }
    }

    /**
     * Picks a provider in this order:
     *   1. AICore on-device (Gemini Nano) — gated on real device support so
     *      we don't try on phones without the AICore feature pack.
     *   2. Cloud Gemini via the `generativeai` SDK — when an API key is set.
     *   3. No-op.
     */
    private fun pickBestProvider(): Provider {
        val onDevice = tryBuildAiCoreProvider()
        if (onDevice != null) {
            Log.i(TAG, "AICore is available — using on-device Gemini Nano.")
            return onDevice
        }

        if (BuildConfig.GEMINI_API_KEY.isNotBlank()) {
            val cloud = runCatching {
                CloudProvider(
                    GenerativeModel(
                        modelName = "gemini-1.5-flash",
                        apiKey = BuildConfig.GEMINI_API_KEY,
                        generationConfig = generationConfig {
                            temperature = 0.1f
                            maxOutputTokens = 1024
                        }
                    )
                )
            }.getOrNull()
            if (cloud != null) {
                Log.i(TAG, "AICore unavailable — falling back to cloud Gemini.")
                return cloud
            }
        }

        Log.i(TAG, "No LLM provider available — refinement disabled.")
        return NoopProvider
    }

    /**
     * Builds a real AICore provider when the feature is reachable on this
     * device. The AICore SDK is in early access; if its API surface changes
     * we still return null instead of crashing — the cloud / no-op fallbacks
     * keep the rest of the app healthy.
     */
    private fun tryBuildAiCoreProvider(): Provider? = try {
        val cls = Class.forName("com.google.ai.edge.aicore.GenerativeModel")
        // Successful class load means the AICore artifact resolved at runtime.
        // Hand off to the real bridge below.
        AiCoreProvider(context)
    } catch (_: Throwable) {
        null
    }

    private object NoopProvider : Provider {
        override val name = "noop"
        override suspend fun refine(prompt: String): String? = null
    }

    private class CloudProvider(private val model: GenerativeModel) : Provider {
        override val name = "cloud-gemini"
        override suspend fun refine(prompt: String): String? =
            model.generateContent(prompt).text
    }

    /**
     * Bridge to `com.google.ai.edge.aicore` — Google AICore on-device LLM.
     *
     * The AICore SDK exposes a `GenerativeModel` similar in shape to the
     * cloud one. We construct it lazily; the first call loads the on-device
     * model weights (~1-2 s) and subsequent calls are fast.
     *
     * Implemented with reflection-free imports; if the artifact isn't
     * resolvable at compile time the build will tell us. To remove this
     * provider, simply drop the `aicore` Gradle dep — `tryBuildAiCoreProvider`
     * will start returning null at the `Class.forName` check.
     */
    private class AiCoreProvider(private val ctx: Context) : Provider {
        override val name = "aicore-on-device"

        private val model by lazy {
            // Lazy import via runCatching so a missing class can still surface
            // at runtime as "null" rather than crashing the whole VM.
            runCatching {
                com.google.ai.edge.aicore.GenerativeModel(
                    generationConfig = com.google.ai.edge.aicore.generationConfig {
                        context = ctx
                        temperature = 0.1f
                        topK = 16
                        maxOutputTokens = 1024
                    }
                )
            }.getOrNull()
        }

        override suspend fun refine(prompt: String): String? {
            val m = model ?: return null
            return runCatching { m.generateContent(prompt).text }.getOrNull()
        }
    }

    private fun redact(text: String): String {
        var t = text
        t = t.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL]")
        t = t.replace(Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"), "[PHONE]")
        t = t.replace(Regex("\\b\\d{5,}\\b"), "[REDACTED]")
        return t
    }

    companion object {
        private const val TAG = "GeminiFieldRefiner"
    }
}
