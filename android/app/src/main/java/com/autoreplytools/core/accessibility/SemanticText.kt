package com.autoreplytools.core.accessibility

import java.util.Locale

object SemanticText {
    fun normalize(value: String): String =
        value
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun containsTerm(
        value: String?,
        term: String,
    ): Boolean {
        if (value.isNullOrBlank()) return false
        val normalizedValue = normalize(value)
        val normalizedTerm = normalize(term)
        if (normalizedValue.isBlank() || normalizedTerm.isBlank()) return false
        return normalizedValue == normalizedTerm ||
            normalizedValue.startsWith("$normalizedTerm ") ||
            normalizedValue.endsWith(" $normalizedTerm") ||
            normalizedValue.contains(" $normalizedTerm ")
    }
}
