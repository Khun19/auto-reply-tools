package com.autoreplytools.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo

class NodeMatcher(
    private val scorer: NodeScorer = NodeScorer(),
) {
    fun findBest(root: AccessibilityNodeInfo, query: NodeQuery): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestScore = Int.MIN_VALUE

        fun visit(node: AccessibilityNodeInfo) {
            val score = scorer.score(node, query)
            if (score > bestScore) {
                bestScore = score
                best = node
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(::visit)
            }
        }

        visit(root)
        return best?.takeIf { bestScore > 0 }
    }
}