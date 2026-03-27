package com.formbuddy.android.data.model

import java.util.Date
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: Sender,
    val content: String,
    val timestamp: Date = Date(),
    val associatedValue: AssociatedValue? = null
) {
    enum class Sender { USER, SYSTEM }

    sealed class AssociatedValue {
        data class Field(val fieldId: String) : AssociatedValue()
        data class SuggestedValue(val value: String, val fieldId: String) : AssociatedValue()
        data class SaveValue(val fieldId: String) : AssociatedValue()
        data object ConversationDone : AssociatedValue()
    }
}

data class ChatSession(
    val formTemplate: FormTemplate,
    var currentFieldIndex: Int = 0,
    val conversationHistory: MutableList<ChatMessage> = mutableListOf()
) {
    val currentField: FormField?
        get() = formTemplate.allFields.filter { it.isEmpty }.getOrNull(currentFieldIndex)

    val isComplete: Boolean
        get() = formTemplate.allFields.none { it.isEmpty }
}
