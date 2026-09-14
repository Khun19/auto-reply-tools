package com.autoreplytools.core.adapters

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import com.autoreplytools.core.accessibility.NodeMatcher
import com.autoreplytools.core.accessibility.NodeQuery

class ViberAdapter : AppAdapter {
    override val supportedPackages = setOf("com.viber.voip")
    private val matcher = NodeMatcher()

    fun openConversation(context: Context, conversationIntent: Intent?): Boolean {
        return try {
            val intent = conversationIntent ?: context.packageManager.getLaunchIntentForPackage("com.viber.voip")
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (intent == null) false else {
                context.startActivity(intent)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override fun openApp(context: Context): Boolean = openConversation(context, null)

    override fun findInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        matcher.findBest(
            root,
            NodeQuery(
                requiredPackage = "com.viber.voip",
                editable = true,
                visible = true,
                enabled = true,
                semanticTerms = setOf("message", "type", "write"),
                classNames = setOf("android.widget.EditText"),
                minimumScore = 60,
            ),
        )

    override fun findSend(root: AccessibilityNodeInfo): AccessibilityNodeInfo? =
        matcher.findBest(
            root,
            NodeQuery(
                requiredPackage = "com.viber.voip",
                clickable = true,
                visible = true,
                enabled = true,
                semanticTerms = setOf("send", "message"),
                minimumScore = 55,
            ),
        )
}
