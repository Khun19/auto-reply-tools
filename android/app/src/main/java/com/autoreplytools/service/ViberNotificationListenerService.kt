package com.autoreplytools.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.autoreplytools.core.RuntimeContainer
import com.autoreplytools.core.logging.AppLogger
import com.autoreplytools.core.model.AiProvider
import com.autoreplytools.core.model.AutomationTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class ViberNotificationListenerService : NotificationListenerService() {
    private val logger = AppLogger("ViberNotification")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (statusBarNotification.packageName != VIBER_PACKAGE) return
        val notification = statusBarNotification.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = notification.extras
        val sender = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
            ?: return
        val message = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return
        if (message.isBlank()) return

        val conversationId = extras.getString(Notification.EXTRA_CONVERSATION_TITLE)
        val task = AutomationTask(
            taskId = UUID.randomUUID().toString(),
            sender = sender,
            conversationId = conversationId,
            originalMessage = message,
            timestamp = statusBarNotification.postTime,
            sourcePackage = statusBarNotification.packageName,
            targetAiProvider = AiProvider.CHATGPT,
            conversationIntent = notification.contentIntent,
        )
        scope.launch {
            if (!RuntimeContainer.engine.enqueue(task)) {
                logger.info("Viber notification ignored by safety filters")
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) = Unit

    companion object {
        private const val VIBER_PACKAGE = "com.viber.voip"
    }
}