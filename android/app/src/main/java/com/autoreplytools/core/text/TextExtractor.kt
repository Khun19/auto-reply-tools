package com.autoreplytools.core.text

import android.view.accessibility.AccessibilityNodeInfo

data class TextSnapshot(val texts: Set<String>) {
    fun normalized(): Set<String> = texts.map { it.trim().replace(Regex("\\s+"), " ") }.toSet()
}

class TextExtractor {
    private val ignoredLabels = setOf(
        "send", "submit", "copy", "share", "stop generating", "regenerate",
        "new chat", "menu", "settings", "chatgpt", "gemini",
    )

    fun collect(root: AccessibilityNodeInfo): TextSnapshot {
        val texts = linkedSetOf<String>()
        fun visit(node: AccessibilityNodeInfo) {
            val candidates = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            candidates.forEach { value ->
                val normalized = value.trim().replace(Regex("\\s+"), " ")
                if (normalized.length >= 2 && normalized.lowercase() !in ignoredLabels) texts += normalized
            }
            for (index in 0 until node.childCount) node.getChild(index)?.let(::visit)
        }
        visit(root)
        return TextSnapshot(texts)
    }

    fun newestCandidate(before: TextSnapshot, current: TextSnapshot): String? {
        return current.normalized()
            .asSequence()
            .filterNot { it in before.normalized() }
            .filter { it.length >= 2 }
            .maxByOrNull { it.length }
    }
}