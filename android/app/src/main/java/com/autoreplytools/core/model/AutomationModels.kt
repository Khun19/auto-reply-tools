package com.autoreplytools.core.model

import android.app.PendingIntent

enum class AutomationState {
    IDLE,
    VIBER_MSG_DETECTED,
    OPEN_AI_APP,
    PASTE_TO_AI,
    SEND_TO_AI,
    WAIT_AI_RESPONSE,
    COPY_AI_RESPONSE,
    OPEN_VIBER,
    PASTE_TO_VIBER,
    SEND_TO_VIBER,
    VERIFY,
    ERROR,
    STOPPED,
}

enum class AiProvider {
    CHATGPT,
    GEMINI,
}

data class AutomationTask(
    val taskId: String,
    val sender: String,
    val conversationId: String?,
    val originalMessage: String,
    val timestamp: Long,
    val sourcePackage: String,
    val targetAiProvider: AiProvider,
    val conversationIntent: PendingIntent?,
)

data class AutomationTimeouts(
    val openAppMs: Long = 10_000L,
    val pasteMs: Long = 5_000L,
    val sendMs: Long = 10_000L,
    val waitResponseMs: Long = 60_000L,
    val verifyMs: Long = 10_000L,
    val overallMs: Long = 120_000L,
)

data class AppSettings(
    val enabled: Boolean = false,
    val aiProvider: AiProvider = AiProvider.CHATGPT,
    val systemPrompt: String = "You are an automatic reply assistant. Reply naturally and concisely. Do not invent facts. Keep the reply appropriate for a normal Viber conversation.",
    val whitelist: Set<String> = emptySet(),
)