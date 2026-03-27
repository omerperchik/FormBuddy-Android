package com.formbuddy.android.data.model

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class FormTemplate(
    val id: String = UUID.randomUUID().toString(),
    var documentName: String = "",
    var isVerified: Boolean = false,
    val pages: MutableList<FormPage> = mutableListOf()
) {
    val allFields: List<FormField>
        get() = pages.flatMap { it.fields }

    val completedFieldsCount: Int
        get() = allFields.count { !it.isEmpty }

    val totalFieldsCount: Int
        get() = allFields.size

    val completionPercentage: Float
        get() = if (totalFieldsCount == 0) 0f else completedFieldsCount.toFloat() / totalFieldsCount
}

data class FormPage(
    val index: Int,
    val fields: MutableList<FormField> = mutableListOf(),
    @Transient var pdfPageIndex: Int = 0
)

data class FormField(
    val id: String = UUID.randomUUID().toString(),
    var label: String = "",
    var fieldType: FieldType = FieldType.TEXT,
    var fieldSubType: FieldSubType? = null,
    val boundingBox: BoundingBox = BoundingBox(),
    var valueBoundingBox: BoundingBox? = null,
    var detectedValue: String? = null,
    var userValue: String? = null,
    var confidence: Double? = null,
    var isUserGenerated: Boolean = false,
    var userInputMethod: UserInputMethod? = null,
    var style: FieldStyle = FieldStyle()
) {
    val displayValue: String
        get() = userValue ?: detectedValue ?: ""

    val isEmpty: Boolean
        get() = when (fieldType) {
            FieldType.CHECKBOX -> userValue == null && detectedValue == null
            FieldType.SIGNATURE -> (userValue ?: detectedValue).isNullOrBlank()
            else -> displayValue.isBlank()
        }

    val contentBoundingBox: BoundingBox
        get() {
            val vBox = valueBoundingBox ?: return boundingBox
            return BoundingBox(
                x = minOf(boundingBox.x, vBox.x),
                y = minOf(boundingBox.y, vBox.y),
                width = maxOf(boundingBox.x + boundingBox.width, vBox.x + vBox.width) - minOf(boundingBox.x, vBox.x),
                height = maxOf(boundingBox.y + boundingBox.height, vBox.y + vBox.height) - minOf(boundingBox.y, vBox.y)
            )
        }
}

data class BoundingBox(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val normalizedVertices: List<NormalizedVertex> = emptyList()
) {
    fun scaledTo(pageWidth: Float, pageHeight: Float): android.graphics.RectF {
        return android.graphics.RectF(
            (x * pageWidth).toFloat(),
            (y * pageHeight).toFloat(),
            ((x + width) * pageWidth).toFloat(),
            ((y + height) * pageHeight).toFloat()
        )
    }
}

data class NormalizedVertex(
    val x: Double = 0.0,
    val y: Double = 0.0
)

enum class FieldType {
    @SerializedName("text") TEXT,
    @SerializedName("number") NUMBER,
    @SerializedName("checkbox") CHECKBOX,
    @SerializedName("signature") SIGNATURE,
    @SerializedName("date") DATE,
    @SerializedName("email") EMAIL,
    @SerializedName("phone") PHONE,
    @SerializedName("address") ADDRESS
}

enum class FieldSubType {
    @SerializedName("firstName") FIRST_NAME,
    @SerializedName("middleName") MIDDLE_NAME,
    @SerializedName("lastName") LAST_NAME,
    @SerializedName("birthDate") BIRTH_DATE,
    @SerializedName("homeAddress") HOME_ADDRESS,
    @SerializedName("workAddress") WORK_ADDRESS,
    @SerializedName("city") CITY,
    @SerializedName("state") STATE,
    @SerializedName("country") COUNTRY,
    @SerializedName("postalCode") POSTAL_CODE,
    @SerializedName("companyName") COMPANY_NAME,
    @SerializedName("taxId") TAX_ID,
    @SerializedName("registrationNumber") REGISTRATION_NUMBER,
    @SerializedName("website") WEBSITE,
    @SerializedName("industry") INDUSTRY
}

enum class UserInputMethod {
    @SerializedName("voiceFilled") VOICE_FILLED,
    @SerializedName("profileAutofill") PROFILE_AUTOFILL,
    @SerializedName("acceptedSuggestion") ACCEPTED_SUGGESTION
}

data class FieldStyle(
    var lineHeightFactor: Float = 1.0f,
    var textAlignment: TextAlignment = TextAlignment.LEFT,
    var kern: Float = 0f,
    var fontSize: Float = 12f,
    var topSpacing: Float = 0f,
    var rightSpacing: Float = 0f,
    var leftSpacing: Float = 0f,
    var dateFormat: DateFormatType = DateFormatType.US_STANDARD,
    var signatureWidth: Float = 200f,
    var checkboxType: CheckboxType = CheckboxType.NONE
)

enum class TextAlignment {
    LEFT, CENTER, RIGHT
}

enum class DateFormatType(val pattern: String) {
    US_STANDARD("MM/dd/yyyy"),
    EUROPEAN_STANDARD("dd/MM/yyyy"),
    ISO("yyyy-MM-dd"),
    SHORT_MONTH_US("MMM dd, yyyy"),
    LONG_MONTH_US("MMMM dd, yyyy"),
    SHORT_MONTH_EU("dd MMM yyyy"),
    LONG_MONTH_EU("dd MMMM yyyy")
}

enum class CheckboxType {
    NONE, XMARK, CHECKMARK, CIRCLE
}
