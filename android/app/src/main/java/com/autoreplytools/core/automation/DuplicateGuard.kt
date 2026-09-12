package com.autoreplytools.core.automation

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class DuplicateGuard(
    private val cooldownMs: Long = 90_000L,
    private val bucketMs: Long = 30_000L,
) {
    private val recent = ConcurrentHashMap<String, Long>()

    fun shouldProcess(sender: String, message: String, timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        recent.entries.removeIf { now - it.value > cooldownMs }
        val fingerprint = fingerprint(sender, message, timestamp)
        val previous = recent.putIfAbsent(fingerprint, now)
        return previous == null
    }

    private fun fingerprint(sender: String, message: String, timestamp: Long): String {
        val bucket = timestamp / bucketMs
        val input = "${sender.trim().lowercase()}|${message.trim()}|$bucket"
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}