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

        // A sender is required because whitelist matching must never fall back
        // to a shared value such as "Viber". That would allow an ambiguous
        // notification to be processed as an approved sender.
        val sender = input.sender?.trim()?.takeIf { it.isNotBlank() } ?: return null

        return Accepted(
            sender = sender,
            message = message.trim(),
            conversationId = input.conversationId,
        )
    }
}
