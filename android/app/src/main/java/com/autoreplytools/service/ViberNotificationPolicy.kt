package com.autoreplytools.service

/** Pure notification acceptance/extraction rules used by the Viber listener. */
object ViberNotificationPolicy {
    const val VIBER_PACKAGE = "com.viber.voip"

    data class Input(
        val packageName: String,
        val isGroupSummary: Boolean,
        val sender: String?,
        val bigText: String?,
        val text: String?,
        val conversationId: String?,
    )

    data class Accepted(
        val sender: String,
        val message: String,
        val conversationId: String?,
    )

    fun accept(input: Input): Accepted? {
        if (!input.packageName.equals(VIBER_PACKAGE, ignoreCase = true)) return null
        if (input.isGroupSummary) return null

        val message = (input.bigText ?: input.text)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        // Viber can expose the notification title as the conversation title.
        // Keep a safe fallback so a valid Viber notification is not discarded
        // only because EXTRA_TITLE is missing on a particular Android/MIUI build.
        val sender = input.sender?.trim()?.takeIf { it.isNotBlank() } ?: "Viber"

        return Accepted(
            sender = sender,
            message = message.trim(),
            conversationId = input.conversationId,
        )
    }
}
