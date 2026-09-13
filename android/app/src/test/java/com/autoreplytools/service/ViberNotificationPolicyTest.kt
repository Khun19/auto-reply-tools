package com.autoreplytools.service

import com.autoreplytools.core.automation.DuplicateGuard
import com.autoreplytools.core.automation.Whitelist
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ViberNotificationPolicyTest {
    @Test
    fun rejectsNonViberPackage() {
        assertNull(
            ViberNotificationPolicy.accept(
                ViberNotificationPolicy.Input(
                    packageName = "com.example.other",
                    isGroupSummary = false,
                    sender = "Alice",
                    bigText = "Hello",
                    text = null,
                    conversationId = "chat-1",
                ),
            ),
        )
    }

    @Test
    fun rejectsGroupSummary() {
        assertNull(
            ViberNotificationPolicy.accept(
                ViberNotificationPolicy.Input(
                    packageName = ViberNotificationPolicy.VIBER_PACKAGE,
                    isGroupSummary = true,
                    sender = "Alice",
                    bigText = "Hello",
                    text = null,
                    conversationId = "chat-1",
                ),
            ),
        )
    }

    @Test
    fun rejectsMissingSender() {
        assertNull(
            ViberNotificationPolicy.accept(
                ViberNotificationPolicy.Input(
                    packageName = ViberNotificationPolicy.VIBER_PACKAGE,
                    isGroupSummary = false,
                    sender = null,
                    bigText = "Hello",
                    text = null,
                    conversationId = "chat-1",
                ),
            ),
        )
    }

    @Test
    fun rejectsBlankMessage() {
        assertNull(
            ViberNotificationPolicy.accept(
                ViberNotificationPolicy.Input(
                    packageName = ViberNotificationPolicy.VIBER_PACKAGE,
                    isGroupSummary = false,
                    sender = "Alice",
                    bigText = "   ",
                    text = null,
                    conversationId = "chat-1",
                ),
            ),
        )
    }

    @Test
    fun prefersBigTextAndPreservesConversationId() {
        val accepted = ViberNotificationPolicy.accept(
            ViberNotificationPolicy.Input(
                packageName = ViberNotificationPolicy.VIBER_PACKAGE,
                isGroupSummary = false,
                sender = "Alice",
                bigText = "Expanded message",
                text = "Collapsed message",
                conversationId = "chat-1",
            ),
        )

        assertEquals("Alice", accepted?.sender)
        assertEquals("Expanded message", accepted?.message)
        assertEquals("chat-1", accepted?.conversationId)
    }

    @Test
    fun fallsBackToTextWhenBigTextMissing() {
        val accepted = ViberNotificationPolicy.accept(
            ViberNotificationPolicy.Input(
                packageName = ViberNotificationPolicy.VIBER_PACKAGE,
                isGroupSummary = false,
                sender = "Alice",
                bigText = null,
                text = "Collapsed message",
                conversationId = null,
            ),
        )

        assertEquals("Collapsed message", accepted?.message)
    }

    @Test
    fun whitelistNormalizesCaseAndWhitespace() {
        val whitelist = Whitelist()
        assertTrue(whitelist.isAllowed("  Alice   Smith ", setOf("alice smith")))
        assertFalse(whitelist.isAllowed("Bob", setOf("alice smith")))
    }

    @Test
    fun duplicateGuardRejectsSameMessageInSameBucket() {
        val guard = DuplicateGuard(cooldownMs = 90_000L, bucketMs = 30_000L)
        val timestamp = 1_800_000_000_000L

        assertTrue(guard.shouldProcess("Alice", "Hello", timestamp))
        assertFalse(guard.shouldProcess("alice", "Hello", timestamp + 1_000L))
    }

    @Test
    fun duplicateGuardAllowsDifferentMessage() {
        val guard = DuplicateGuard(cooldownMs = 90_000L, bucketMs = 30_000L)
        val timestamp = 1_800_000_000_000L

        assertTrue(guard.shouldProcess("Alice", "Hello", timestamp))
        assertTrue(guard.shouldProcess("Alice", "Different", timestamp + 1_000L))
    }
}
