package com.autoreplytools.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.autoreplytools.core.logging.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

class AccessibilityController(
    private val service: AccessibilityService,
    private val logger: AppLogger,
) {
    suspend fun awaitRoot(
        packageNames: Set<String>,
        timeoutMs: Long,
    ): AccessibilityNodeInfo? = withTimeoutOrNull(timeoutMs) {
        while (true) {
            val root = service.rootInActiveWindow
            if (root != null && root.packageName?.toString() in packageNames) return@withTimeoutOrNull root
            delay(300L)
        }
    }

    suspend fun awaitNode(
        packageNames: Set<String>,
        finder: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?,
        timeoutMs: Long,
    ): AccessibilityNodeInfo? = withTimeoutOrNull(timeoutMs) {
        while (true) {
            val root = service.rootInActiveWindow
            if (root != null && root.packageName?.toString() in packageNames) {
                finder(root)?.let { return@withTimeoutOrNull it }
            }
            delay(300L)
        }
    }

    fun setText(node: AccessibilityNodeInfo, text: String): Boolean {
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (!success) logger.warn("Semantic text insertion failed")
        return success
    }

    fun click(node: AccessibilityNodeInfo): Boolean {
        val success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!success) logger.warn("Semantic click failed")
        return success
    }
}