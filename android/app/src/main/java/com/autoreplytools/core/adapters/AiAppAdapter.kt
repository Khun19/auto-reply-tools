package com.autoreplytools.core.adapters

import android.view.accessibility.AccessibilityNodeInfo
import com.autoreplytools.core.text.TextSnapshot

interface AiAppAdapter : AppAdapter {
    fun isGenerating(root: AccessibilityNodeInfo): Boolean

    fun extractLatestResponse(before: TextSnapshot, root: AccessibilityNodeInfo): String?
}