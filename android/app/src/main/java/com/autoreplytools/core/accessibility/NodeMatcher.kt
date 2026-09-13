package com.autoreplytools.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

class NodeMatcher(
    private val scorer: NodeScorer = NodeScorer(),
) {
    fun findBest(root: AccessibilityNodeInfo, query: NodeQuery): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestScore = Int.MIN_VALUE
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var inspected = 0

        while (pending.isNotEmpty() && inspected < query.maxNodes) {
            val node = pending.removeLast()
            inspected++
            val score = scorer.score(node, query)
            if (score != Int.MIN_VALUE && score > bestScore) {
                bestScore = score
                best = node
            }
            for (index in node.childCount - 1 downTo 0) {
                node.getChild(index)?.let(pending::addLast)
            }
        }

        return best?.takeIf { bestScore >= query.minimumScore }
    }
}