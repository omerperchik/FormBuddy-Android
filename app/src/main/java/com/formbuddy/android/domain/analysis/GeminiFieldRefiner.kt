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
     * Tries the AICore SDK at runtime via reflection so this app still
     * compiles and runs without it on the build path. When Google ships a
     * stable AICore artifact, replace this body with a direct dependency.
     */
    private fun pickBestProvider(): Provider {
        // Probe for AICore via reflection. The package name is the one Google
        // uses on Pixel 8+ / Galaxy S24+: `com.google.android.aicore`.
        val onDevice = tryLoadAiCore()
        if (onDevice != null) return onDevice

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
            if (cloud != null) return cloud
        }

        return NoopProvider
    }

    private fun tryLoadAiCore(): Provider? = try {
        val featureCls = Class.forName("com.google.android.aicore.feature.GenerateContentFeature")
        // `featureCls.getMethod("isFeatureAvailable", Context::class.java)` style probe.
        // We deliberately don't call it without the real SDK — this is a marker that
        // the SDK is reachable on this device. Without the real binding we fall
        // through to cloud / no-op.
        Log.i(TAG, "AICore feature class found: $featureCls — wire AICoreProvider here")
        null
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
