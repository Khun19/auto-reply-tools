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
        if (node.isEditable) score += 40
        if (node.isClickable) score += 30
        if (node.className?.toString() in query.classNames) score += 25

        val text = node.text?.toString()
        val contentDescription = node.contentDescription?.toString()
        query.semanticTerms.forEach { term ->
            if (SemanticText.containsTerm(text, term)) score += 25
            if (SemanticText.containsTerm(contentDescription, term)) score += 35
        }
        return score
    }
}