package com.autoreplytools.core.automation

class Whitelist {
    fun isAllowed(sender: String, configuredSenders: Set<String>): Boolean {
        val normalizedSender = normalize(sender)
        return normalizedSender.isNotEmpty() &&
            configuredSenders.any { normalize(it) == normalizedSender }
    }

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase()
}