package com.autoreplytools.core.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticTextTest {
    @Test
    fun matchesTermsAcrossCaseAndPunctuation() {
        assertTrue(SemanticText.containsTerm("  Send-message  ", "send message"))
        assertTrue(SemanticText.containsTerm("Type your reply", "type"))
    }

    @Test
    fun doesNotMatchPartialWords() {
        assertFalse(SemanticText.containsTerm("Resend", "send"))
        assertFalse(SemanticText.containsTerm("Message", "write"))
    }

    @Test
    fun normalizesWhitespaceAndPunctuation() {
        assertTrue(SemanticText.normalize("  SEND!!!   message ") == "send message")
    }
}
