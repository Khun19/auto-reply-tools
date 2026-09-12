package com.autoreplytools.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo

class NodeScorer {
    fun score(node: AccessibilityNodeInfo, query: NodeQuery): Int {
        if (query.visible && !node.isVisibleToUser) return Int.MIN_VALUE
        if (query.requiredPackage != null && node.packageName?.toString() != query.requiredPackage) return Int.MIN_VALUE
        if (query.editable != null && node.isEditable != query.editable) return Int.MIN_VALUE
        if (query.clickable != null && node.isClickable != query.clickable) return Int.MIN_VALUE
        if (query.enabled != null && node.isEnabled != query.enabled) return Int.MIN_VALUE
        if (query.classNames.isNotEmpty() && node.className?.toString() !in query.classNames) return Int.MIN_VALUE

        var score = 0
        if (node.isVisibleToUser) score += 10
        if (node.isEnabled) score += 10
        if (node.isEditable) score += 50
        if (node.isClickable) score += 20
        if (node.className?.toString() in query.classNames) score += 15

        val searchable = listOfNotNull(node.text?.toString(), node.contentDescription?.toString())
            .joinToString(" ")
            .lowercase()
        query.semanticTerms.forEach { term ->
            if (searchable.contains(term.lowercase())) score += 20
        }
        return score
    }
}