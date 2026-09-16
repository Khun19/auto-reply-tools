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
        val notification = statusBarNotification.notification ?: return
        val extras = notification.extras
        val accepted = ViberNotificationPolicy.accept(
            ViberNotificationPolicy.Input(
                packageName = statusBarNotification.packageName,
                isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                sender = extras.getString(Notification.EXTRA_TITLE)
                    ?: extras.getString(Notification.EXTRA_CONVERSATION_TITLE),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                conversationId = extras.getString(Notification.EXTRA_CONVERSATION_TITLE),
            ),
        ) ?: return

        scope.launch {
            RuntimeContainer.settingsRepository.recordRecentViberSender(accepted.sender)
        }

        val task = AutomationTask(
            taskId = UUID.randomUUID().toString(),
            sender = accepted.sender,
            conversationId = accepted.conversationId,
            originalMessage = accepted.message,
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
}
