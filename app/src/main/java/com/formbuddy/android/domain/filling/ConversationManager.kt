package com.formbuddy.android.domain.filling

import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormTemplate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConversationManager @Inject constructor() {

    private var template: FormTemplate? = null

    fun startSession(formTemplate: FormTemplate) {
        template = formTemplate
    }

    fun getQuestionForField(field: FormField): String {
        val label = field.label.ifBlank { field.fieldType.name.lowercase() }

        return when (field.fieldType) {
            FieldType.TEXT -> "What is your $label?"
            FieldType.NUMBER -> "Please enter the $label:"
            FieldType.EMAIL -> "What is your email address?"
            FieldType.PHONE -> "What is your phone number?"
            FieldType.DATE -> "What is the $label? (e.g., MM/DD/YYYY)"
            FieldType.ADDRESS -> "What is your $label?"
            FieldType.CHECKBOX -> "Should \"$label\" be checked? (yes/no)"
            FieldType.SIGNATURE -> "Please provide your signature for \"$label\""
        }
    }

    fun getUnfilledFields(): List<FormField> {
        return template?.allFields?.filter { it.isEmpty } ?: emptyList()
    }
}
