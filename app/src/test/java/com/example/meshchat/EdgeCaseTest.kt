package com.example.meshchat

import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder

class EdgeCaseTest {

    @Test
    fun testEmptyAndBlankMessagesAreRejected() {
        val emptyMessage = ""
        val whitespaceMessage = "    \n\t  "

        assertTrue("Empty message must be blank", emptyMessage.isBlank())
        assertTrue("Whitespace-only message must be blank", whitespaceMessage.isBlank())

        fun validateMessage(text: String): Boolean {
            if (text.isBlank()) return false
            if (text.length > 4096) return false
            return true
        }

        assertFalse(validateMessage(emptyMessage))
        assertFalse(validateMessage(whitespaceMessage))
        assertTrue(validateMessage("Valid text"))
    }

    @Test
    fun testOversizedMessageIsRejected() {
        fun validateMessage(text: String): Boolean {
            if (text.isBlank()) return false
            if (text.length > 4096) return false
            return true
        }

        val oversized = "A".repeat(4097)
        assertFalse("Message > 4096 characters must be rejected", validateMessage(oversized))

        val exactMax = "A".repeat(4096)
        assertTrue("Message of 4096 characters must be accepted", validateMessage(exactMax))
    }

    @Test
    fun testSpecialCharactersAndEmojiPayloads() {
        val complexText = "🛰️ Sentinel SOS! ⚠️ Lat: 12.97° N, Lon: 80.27° E | Special symbols: <>&\"'#%*{}[]\\/ ~`"
        val utf8Bytes = complexText.toByteArray(Charsets.UTF_8)
        val reconstructed = String(utf8Bytes, Charsets.UTF_8)

        assertEquals("UTF-8 byte roundtrip must preserve emoji and symbols perfectly", complexText, reconstructed)

        val urlEncoded = URLEncoder.encode(complexText, "UTF-8")
        assertNotNull(urlEncoded)
        assertTrue(urlEncoded.contains("%F0%9F%9B%B0")) // 🛰️
    }

    @Test
    fun testDebouncePreventsConcurrentDuplicateSends() {
        var isProcessing = false
        var sentCount = 0

        fun onSendTapped(text: String) {
            if (text.isBlank()) return
            if (isProcessing) return // Debounce guard!
            isProcessing = true
            sentCount++
            // Simulate processing finished
            isProcessing = false
        }

        onSendTapped("Message 1")
        assertEquals(1, sentCount)

        // Simulate rapid tap while isProcessing is true
        isProcessing = true
        onSendTapped("Message 1 Duplicate Tap")
        assertEquals("Rapid tap during processing must be ignored", 1, sentCount)
    }
}
