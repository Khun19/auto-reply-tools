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
        if (input.packageName != VIBER_PACKAGE) return null
        if (input.isGroupSummary) return null

        val sender = input.sender?.takeIf { it.isNotBlank() } ?: return null
        val message = (input.bigText ?: input.text)?.takeIf { it.isNotBlank() } ?: return null

        return Accepted(
            sender = sender,
            message = message,
            conversationId = input.conversationId,
        )
    }
}
