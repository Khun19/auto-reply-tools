package com.autoreplytools.core.adapters

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import com.autoreplytools.core.accessibility.NodeMatcher
import com.autoreplytools.core.accessibility.NodeQuery
import com.autoreplytools.core.text.TextExtractor
import com.autoreplytools.core.text.TextSnapshot

abstract class SemanticAiAdapter(
    private val packageManager: PackageManager,
    private val matcher: NodeMatcher = NodeMatcher(),
    private val extractor: TextExtractor = TextExtractor(),
) : AiAppAdapter {
    protected abstract val inputTerms: Set<String>
    protected abstract val sendTerms: Set<String>
    protected abstract val generatingTerms: Set<String>

    override fun openApp(context: Context): Boolean {
        val packageName = supportedPackages.firstNotNullOfOrNull { candidate ->
            packageManager.getLaunchIntentForPackage(candidate)?.let { candidate to it }
        } ?: return false
        return try {
            val intent = packageName.second.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun findInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        matcher.findBest(
            root,
            NodeQuery(
                requiredPackage = root.packageName?.toString(),
                editable = true,
                visible = true,
                enabled = true,
                semanticTerms = inputTerms,
                classNames = setOf("android.widget.EditText"),
                minimumScore = 60,
            ),
        ) ?: matcher.findBest(
            root,
            NodeQuery(
                editable = true,
                visible = true,
                enabled = true,
                semanticTerms = inputTerms,
                minimumScore = 50,
            ),
        )

    override fun findSend(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        matcher.findBest(
            root,
            NodeQuery(
                clickable = true,
                visible = true,
                enabled = true,
                semanticTerms = sendTerms,
                minimumScore = 55,
            ),
        )

    override fun isGenerating(root: AccessibilityNodeInfo): Boolean {
        val snapshot = extractor.collect(root).texts.joinToString(" ").lowercase()
        return generatingTerms.any { it in snapshot }
    }

    override fun extractLatestResponse(before: TextSnapshot, root: AccessibilityNodeInfo): String? =
        extractor.newestCandidate(before, extractor.collect(root))
}