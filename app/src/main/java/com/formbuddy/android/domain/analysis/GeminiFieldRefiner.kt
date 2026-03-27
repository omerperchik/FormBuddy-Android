package com.formbuddy.android.domain.analysis

import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormTemplate
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiFieldRefiner @Inject constructor() {

    private val model: GenerativeModel? = try {
        GenerativeModel(
            modelName = "gemini-nano",
            generationConfig = generationConfig {
                temperature = 0.1f
                maxOutputTokens = 1024
            }
        )
    } catch (_: Exception) {
        null // Gemini Nano not available on this device
    }

    suspend fun refineFields(template: FormTemplate): FormTemplate {
        val ai = model ?: return template // Skip if not available

        try {
            val fieldsDescription = template.allFields.mapIndexed { index, field ->
                "$index: label=\"${redactSensitiveData(field.label)}\", type=${field.fieldType.name}"
            }.joinToString("\n")

            val prompt = """
                Analyze these form fields and suggest better labels and types.
                For each field, output: index|refined_label|type

                Available types: TEXT, NUMBER, CHECKBOX, SIGNATURE, DATE, EMAIL, PHONE, ADDRESS

                Fields:
                $fieldsDescription

                Output refined fields (one per line):
            """.trimIndent()

            val response = ai.generateContent(prompt)
            val text = response.text ?: return template

            // Parse response
            text.lines().forEach { line ->
                val parts = line.split("|").map { it.trim() }
                if (parts.size == 3) {
                    val index = parts[0].toIntOrNull() ?: return@forEach
                    val refinedLabel = parts[1]
                    val typeStr = parts[2].uppercase()

                    val field = template.allFields.getOrNull(index) ?: return@forEach
                    if (refinedLabel.isNotBlank()) {
                        field.label = refinedLabel
                    }
                    try {
                        field.fieldType = FieldType.valueOf(typeStr)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {
            // Gemini failed, continue with existing inference
        }

        return template
    }

    private fun redactSensitiveData(text: String): String {
        var redacted = text
        // Redact emails
        redacted = redacted.replace(Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"), "[EMAIL]")
        // Redact phone numbers
        redacted = redacted.replace(Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"), "[PHONE]")
        // Redact long digit sequences (SSN, etc)
        redacted = redacted.replace(Regex("\\b\\d{5,}\\b"), "[REDACTED]")
        return redacted
    }
}
