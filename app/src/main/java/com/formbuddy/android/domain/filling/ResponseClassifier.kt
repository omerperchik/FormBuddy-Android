package com.formbuddy.android.domain.filling

import com.formbuddy.android.data.model.FormField
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResponseClassifier @Inject constructor() {

    enum class Classification {
        VALUE, YES, NO, SKIP
    }

    private val yesKeywords = setOf(
        "yes", "yeah", "yep", "sure", "ok", "correct", "right", "true",
        "sí", "si", "oui", "ja", "sim", "نعم", "да", "はい", "हाँ", "是"
    )

    private val noKeywords = setOf(
        "no", "nope", "nah", "false", "wrong",
        "no", "non", "nein", "não", "لا", "нет", "いいえ", "नहीं", "不"
    )

    private val skipKeywords = setOf(
        "skip", "pass", "next", "none", "n/a",
        "saltar", "passer", "überspringen", "pular", "تخطي", "пропустить", "スキップ", "छोड़ें", "跳过"
    )

    fun classify(response: String, field: FormField): Classification {
        val normalized = response.trim().lowercase()

        return when {
            skipKeywords.contains(normalized) -> Classification.SKIP
            yesKeywords.contains(normalized) -> Classification.YES
            noKeywords.contains(normalized) -> Classification.NO
            else -> Classification.VALUE
        }
    }
}
