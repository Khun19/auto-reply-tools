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
    ): AccessibilityNodeInfo? {
        return withTimeoutOrNull(timeoutMs) {
            var result: AccessibilityNodeInfo? = null
            while (result == null) {
                val root = service.rootInActiveWindow
                if (root != null && root.packageName?.toString() in packageNames) {
                    result = root
                } else {
                    delay(POLL_INTERVAL_MS)
                }
            }
            result
        }
    }

    suspend fun awaitNode(
        packageNames: Set<String>,
        finder: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?,
        timeoutMs: Long,
    ): AccessibilityNodeInfo? {
        return withTimeoutOrNull(timeoutMs) {
            var result: AccessibilityNodeInfo? = null
            while (result == null) {
                val root = service.rootInActiveWindow
                if (root != null && root.packageName?.toString() in packageNames) {
                    result = finder(root)
                }
                if (result == null) delay(POLL_INTERVAL_MS)
            }
            result
        }
    }

    fun setText(
        node: AccessibilityNodeInfo,
        text: String,
    ): Boolean {
        val arguments =
            Bundle().apply {
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

    suspend fun setTextWithRetry(
        packageNames: Set<String>,
        finder: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?,
        text: String,
        timeoutMs: Long,
        policy: AccessibilityActionPolicy = AccessibilityActionPolicy(),
    ): AccessibilityActionResult {
        return performWithRetry(
            packageNames = packageNames,
            finder = finder,
            timeoutMs = timeoutMs,
            policy = policy,
            actionName = "set text",
        ) { node -> setText(node, text) }
    }

    suspend fun clickWithRetry(
        packageNames: Set<String>,
        finder: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?,
        timeoutMs: Long,
        policy: AccessibilityActionPolicy = AccessibilityActionPolicy(),
    ): AccessibilityActionResult {
        return performWithRetry(
            packageNames = packageNames,
            finder = finder,
            timeoutMs = timeoutMs,
            policy = policy,
            actionName = "click",
        ) { node -> click(node) }
    }

    private suspend fun performWithRetry(
        packageNames: Set<String>,
        finder: (AccessibilityNodeInfo) -> AccessibilityNodeInfo?,
        timeoutMs: Long,
        policy: AccessibilityActionPolicy,
        actionName: String,
        action: (AccessibilityNodeInfo) -> Boolean,
    ): AccessibilityActionResult {
        var attempts = 0
        val result =
            withTimeoutOrNull(timeoutMs.coerceAtLeast(1L)) {
                while (attempts < policy.maxAttempts) {
                    val root = service.rootInActiveWindow
                    if (root != null && root.packageName?.toString() in packageNames) {
                        attempts++
                        val node = finder(root)
                        if (node != null && action(node)) {
                            return@withTimeoutOrNull AccessibilityActionResult(
                                succeeded = true,
                                attempts = attempts,
                            )
                        }
                    }
                    if (attempts < policy.maxAttempts) delay(policy.retryDelayMs)
                }
                AccessibilityActionResult(
                    succeeded = false,
                    attempts = attempts,
                    failureReason = "$actionName failed after ${policy.maxAttempts} attempts",
                )
            }
        return result ?: AccessibilityActionResult(
            succeeded = false,
            attempts = attempts,
            failureReason = "$actionName timed out after ${timeoutMs.coerceAtLeast(1L)}ms",
        )
    }

    private companion object {
        const val POLL_INTERVAL_MS = 300L
    }
}
