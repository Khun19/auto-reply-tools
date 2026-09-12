package com.autoreplytools.core

import android.content.Context
import com.autoreplytools.core.accessibility.AccessibilityController
import com.autoreplytools.core.automation.AutomationEngine
import com.autoreplytools.core.automation.DuplicateGuard
import com.autoreplytools.core.automation.Whitelist
import com.autoreplytools.core.logging.AppLogger
import com.autoreplytools.storage.SettingsRepository

object RuntimeContainer {
    private var controller: AccessibilityController? = null
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var engine: AutomationEngine
        private set

    fun initialize(context: Context) {
        if (::engine.isInitialized) return
        val appContext = context.applicationContext
        settingsRepository = SettingsRepository(appContext)
        engine = AutomationEngine(
            context = appContext,
            settingsRepository = settingsRepository,
            duplicateGuard = DuplicateGuard(),
            whitelist = Whitelist(),
            controllerProvider = { controller },
            logger = AppLogger(),
        )
        engine.start()
    }

    fun registerAccessibilityController(value: AccessibilityController) {
        controller = value
    }

    fun unregisterAccessibilityController(value: AccessibilityController) {
        if (controller === value) controller = null
    }
}