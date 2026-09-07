package com.example.meshchat

import com.example.meshchat.ai.QsvmClassifier
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class QsvmClassifierTest {

    private lateinit var classifier: QsvmClassifier

    @Before
    fun setup() {
        classifier = QsvmClassifier(null)
    }

    @Test
    fun testBenignMessagesClassifyAsLevel1Fast() {
        val benignMessages = listOf(
            "hi",
            "hello",
            "ok",
            "hey there",
            "how are you doing today?",
            "see you tomorrow at lunch",
            "sounds good to me!"
        )

        for (msg in benignMessages) {
            val result = classifier.classify(msg)
            assertEquals("Message '$msg' should be Level 1 Fast", 1, result.decision.level)
            assertTrue("Overhead must be 0 for Level 1", result.decision.estimatedPacketOverheadBytes == 0)
        }
    }

    @Test
    fun testPasswordsAndApiKeysClassifyHigher() {
        val sensitiveMessages = listOf(
            "My password is SecretKey123",
            "Use apikey 0xabcdef1234567890 for auth",
            "Here is the rootpw for the server",
            "Please send the login token"
        )

        for (msg in sensitiveMessages) {
            val result = classifier.classify(msg)
            assertTrue("Message '$msg' should be >= Level 2", result.decision.level >= 2)
            assertTrue("Must contain sensitive factors", result.decision.sensitiveFactors.isNotEmpty())
        }
    }

    @Test
    fun testOtpAndBankDetailsClassifyHigher() {
        val financialMessages = listOf(
            "Your OTP is 849201",
            "My bank account number is 9876543210",
            "Confidential finance report for Q3"
        )

        for (msg in financialMessages) {
            val result = classifier.classify(msg)
            assertTrue("Message '$msg' should be >= Level 2", result.decision.level >= 2)
        }
    }

    @Test
    fun testGpsCoordinatesClassifyHigher() {
        val gpsMessages = listOf(
            "Meeting at 12.9716, 80.2707",
            "Target location: 37.7749 -122.4194",
            "Coordinates rendezvous point 13.0827, 80.2707"
        )

        for (msg in gpsMessages) {
            val result = classifier.classify(msg)
            assertTrue("GPS message '$msg' should be >= Level 2", result.decision.level >= 2)
            assertTrue(result.decision.sensitiveFactors.any { it.contains("gps") || it.contains("pattern") })
        }
    }

    @Test
    fun testObfuscatedAndLeetspeakInputs() {
        // "p@ssw0rd", "0TP", "lat 12.97 lon 80.27"
        val p1 = classifier.classify("Here is the p@ssw0rd to enter")
        assertTrue("p@ssw0rd must be detected as sensitive", p1.decision.level >= 2)

        val p2 = classifier.classify("Your 0TP is 123456")
        assertTrue("0TP must be detected as sensitive", p2.decision.level >= 2)

        val p3 = classifier.classify("Our rendezvous lat 12.97 lon 80.27")
        assertTrue("lat 12.97 lon 80.27 must be detected as sensitive", p3.decision.level >= 2)
    }

    @Test
    fun testNoBenignMessageTriggersMaximumProtectionByMistake() {
        val everydayMessages = listOf(
            "Hello team, the report is ready",
            "Good morning! Just checking in on the project status.",
            "I will be late for the meeting today",
            "We watched a cool quantum physics movie yesterday" // Harmless science discussion
        )

        for (msg in everydayMessages) {
            val result = classifier.classify(msg)
            assertNotEquals("Message '$msg' must NEVER trigger Maximum Protection (Level 4)", 4, result.decision.level)
        }
    }

    @Test
    fun testBatchClassification() {
        val batch = listOf("hello", "secret token 0x12345678abcdef", "ok")
        val results = classifier.classifyBatch(batch)
        assertEquals(3, results.size)
        assertEquals(1, results[0].decision.level)
        assertTrue(results[1].decision.level >= 2)
        assertEquals(1, results[2].decision.level)
    }

    @Test
    fun testTrainingDoesNotCorruptBehavior() {
        val trainingData = listOf(
            "casual chat 1" to 1,
            "private password 99" to 3
        )
        classifier.train(trainingData)

        // After training, benign messages must still classify as Level 1
        val result = classifier.classify("hello there")
        assertEquals(1, result.decision.level)
    }
}
