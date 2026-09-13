package com.autoreplytools.core.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityActionPolicyTest {
    @Test
    fun defaultsBoundRetries() {
        val policy = AccessibilityActionPolicy()

        assertEquals(3, policy.maxAttempts)
        assertEquals(250L, policy.retryDelayMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedAttemptConfiguration() {
        AccessibilityActionPolicy(maxAttempts = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNegativeRetryDelay() {
        AccessibilityActionPolicy(retryDelayMs = -1L)
    }
}
