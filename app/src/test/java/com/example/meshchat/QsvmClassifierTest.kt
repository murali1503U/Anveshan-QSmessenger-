package com.example.meshchat

import com.example.meshchat.ai.QsvmSecurityEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class QsvmClassifierTest {
    
    private val engine = QsvmSecurityEngine(null)

    @Test
    fun testLevel1BenignMessages() {
        assertEquals(1, engine.classify("hi"))
        assertEquals(1, engine.classify("hello"))
        assertEquals(1, engine.classify("ok"))
        assertEquals(1, engine.classify("thanks"))
        // No false positives for benign messages
        assertEquals(1, engine.classify("what time is the meeting?"))
        assertEquals(1, engine.classify("just casual chatter here"))
    }

    @Test
    fun testLevel3Triggers() {
        assertEquals(3, engine.classify("my password is abc123"))
        assertEquals(3, engine.classify("coordinates 12.9716 80.2707"))
        assertEquals(3, engine.classify("send me your bank details"))
        assertEquals(3, engine.classify("here is the api token"))
    }

    @Test
    fun testLevel4Triggers() {
        assertEquals(4, engine.classify("master key rotation required"))
        assertEquals(4, engine.classify("update infrastructure settings"))
    }
    
    @Test
    fun testLevel2Triggers() {
        assertEquals(2, engine.classify("contact me at test@example.com"))
        assertEquals(2, engine.classify("my phone number is +12345678901"))
    }

    @Test
    fun testBatchClassification() {
        val messages = listOf(
            "hi",
            "my password is abc123",
            "master key rotation required",
            "thanks"
        )
        val expected = listOf(1, 3, 4, 1)
        assertEquals(expected, engine.classifyBatch(messages))
    }
}
