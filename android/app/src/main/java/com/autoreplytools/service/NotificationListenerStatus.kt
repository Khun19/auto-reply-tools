package com.autoreplytools.service

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings

object NotificationListenerStatus {
    private const val ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners"

    fun isEnabled(context: Context): Boolean {
        val appContext = context.applicationContext
        return runCatching {
            val component = ComponentName(appContext, ViberNotificationListenerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                appContext.getSystemService(NotificationManager::class.java)
                    ?.isNotificationListenerAccessGranted(component) == true
            } else {
                isComponentListed(
                    Settings.Secure.getString(
                        appContext.contentResolver,
                        ENABLED_NOTIFICATION_LISTENERS,
                    ),
                    component,
                )
            }
        }.getOrDefault(false)
    }

    private fun isComponentListed(rawValue: String?, expected: ComponentName): Boolean =
        rawValue.orEmpty()
            .split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
}