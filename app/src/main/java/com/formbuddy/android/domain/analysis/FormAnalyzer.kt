package com.formbuddy.android.domain.analysis

import android.content.Context
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.remote.firebase.FirebaseManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormAnalyzer @Inject constructor(
    private val fillablePdfParser: FillablePdfParser,
    private val ocrService: OcrService,
    private val fieldTypeInferencer: FieldTypeInferencer,
    private val geminiFieldRefiner: GeminiFieldRefiner,
    private val firebaseManager: FirebaseManager,
    private val context: Context
) {
    suspend fun analyzeDocument(documentData: ByteArray): FormTemplate = withContext(Dispatchers.IO) {
        // Check if PDF is fillable
        val isFillable = fillablePdfParser.isFillable(documentData)

        val pdfData = if (isFillable) {
            documentData
        } else {
            // Send to cloud for processing
            firebaseManager.processDocument(documentData)
        }

        // Parse the fillable PDF
        var template = fillablePdfParser.parse(pdfData)

        // Run OCR for label detection on nearby text
        template = ocrService.enrichFieldLabels(template, pdfData)

        // Infer field types from labels
        template = fieldTypeInferencer.inferFieldTypes(template)

        // Refine with Gemini Nano (if available)
        template = geminiFieldRefiner.refineFields(template)

        // Generate document name
        if (template.documentName.isBlank()) {
            template.documentName = "Form ${System.currentTimeMillis() / 1000}"
        }

        template
    }
}
