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

    override fun onListenerConnected() {
        super.onListenerConnected()
        logger.info("Notification listener connected")
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        if (!statusBarNotification.packageName.equals(ViberNotificationPolicy.VIBER_PACKAGE, ignoreCase = true)) {
            return
        }

        val notification = statusBarNotification.notification ?: return
        val extras = notification.extras
        logger.info("Viber notification received")

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
        )

        if (accepted == null) {
            logger.info("Viber notification rejected: no usable message payload")
            return
        }

        logger.info("Viber message captured from ${accepted.sender}")

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
            runCatching {
                RuntimeContainer.initialize(applicationContext)
                RuntimeContainer.engine.start()
                RuntimeContainer.engine.enqueue(task)
            }.onSuccess { acceptedByEngine ->
                if (!acceptedByEngine) {
                    logger.info("Viber message captured but blocked by app settings/whitelist/duplicate guard")
                }
            }.onFailure { error ->
                logger.error("Failed to enqueue Viber message", error)
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
