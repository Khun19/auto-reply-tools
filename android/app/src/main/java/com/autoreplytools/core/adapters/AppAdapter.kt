package com.autoreplytools.core.adapters

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo

interface AppAdapter {
    val supportedPackages: Set<String>

    fun openApp(context: Context): Boolean

    fun findInput(root: AccessibilityNodeInfo): AccessibilityNodeInfo?

    fun findSend(root: AccessibilityNodeInfo): AccessibilityNodeInfo?
}