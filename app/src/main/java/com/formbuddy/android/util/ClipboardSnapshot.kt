package com.formbuddy.android.util

import android.content.ClipboardManager
import android.content.Context
import com.formbuddy.android.data.model.FieldSubType
import com.formbuddy.android.data.model.FieldType
import com.formbuddy.android.data.model.FormField

/**
 * One-shot read of the system clipboard, with field-shape matching so we
 * only surface the chip when the snippet *plausibly* fits a field on the
 * current form.
 *
 * iOS-equivalent isn't a real iOS feature — this is a net-new Android-only
 * micro-interaction that compounds because every utility-app user pastes
 * something at some point.
 */
object ClipboardSnapshot {
    fun read(context: Context): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null
        if (!cm.hasPrimaryClip()) return null
        val item = cm.primaryClip?.getItemAt(0) ?: return null
        val text = item.coerceToText(context).toString().trim()
        return text.takeIf { it.isNotBlank() && it.length <= 256 }
    }

    fun matches(snippet: String, field: FormField): Boolean {
        if (snippet.isBlank()) return false
        return when (field.fieldType) {
            FieldType.EMAIL -> EMAIL.matches(snippet)
            FieldType.PHONE -> PHONE_LIKE.matches(snippet.replace(Regex("[^+0-9]"), ""))
            FieldType.ADDRESS -> looksLikeAddress(snippet)
            FieldType.DATE -> DATE_LIKE.matches(snippet)
            FieldType.NUMBER -> snippet.all { it.isDigit() || it == ',' || it == '.' || it == '-' }
            FieldType.TEXT -> matchesByName(snippet, field.fieldSubType)
            else -> false
        }
    }

    private fun matchesByName(text: String, sub: FieldSubType?): Boolean = when (sub) {
        FieldSubType.FIRST_NAME, FieldSubType.MIDDLE_NAME, FieldSubType.LAST_NAME ->
            text.split(' ').size in 1..2 && text.length in 2..40
        FieldSubType.CITY, FieldSubType.STATE, FieldSubType.COUNTRY -> text.length in 2..60
        FieldSubType.POSTAL_CODE -> POSTAL_LIKE.matches(text)
        FieldSubType.WEBSITE -> URL_LIKE.matches(text)
        FieldSubType.COMPANY_NAME -> text.length in 2..80
        else -> false
    }

    private fun looksLikeAddress(text: String): Boolean {
        if (text.length < 10) return false
        val hasNumber = text.any { it.isDigit() }
        val hasSpace = text.contains(' ')
        return hasNumber && hasSpace
    }

    private val EMAIL = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    // 8-15 digits, optional leading +, allow hyphens/dots/parens/spaces in source.
    private val PHONE_LIKE = Regex("\\+?\\d{8,15}")
    private val DATE_LIKE = Regex("\\d{1,4}[-/. ]\\d{1,2}[-/. ]\\d{1,4}")
    private val POSTAL_LIKE = Regex("[A-Za-z0-9 -]{3,12}")
    private val URL_LIKE = Regex("(https?://)?[\\w-]+(\\.[\\w-]+)+\\S*", RegexOption.IGNORE_CASE)
}
