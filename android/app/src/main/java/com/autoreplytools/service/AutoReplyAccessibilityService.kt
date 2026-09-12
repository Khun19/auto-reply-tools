package com.autoreplytools.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.autoreplytools.core.RuntimeContainer
import com.autoreplytools.core.accessibility.AccessibilityController
import com.autoreplytools.core.logging.AppLogger

class AutoReplyAccessibilityService : AccessibilityService() {
    private lateinit var controller: AccessibilityController

    override fun onServiceConnected() {
        super.onServiceConnected()
        controller = AccessibilityController(this, AppLogger("Accessibility"))
        RuntimeContainer.registerAccessibilityController(controller)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // The automation engine polls boundedly for semantic nodes. Events are
        // intentionally not used as blind click triggers.
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::controller.isInitialized) RuntimeContainer.unregisterAccessibilityController(controller)
        super.onDestroy()
    }
}