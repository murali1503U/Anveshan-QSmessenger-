package com.example.meshchat

import com.example.meshchat.crypto.PskHandshake
import org.junit.Assert.*
import org.junit.Test

class PskHandshakeTest {

    @Test
    fun testPskLengthRequirement() {
        val handshake = PskHandshake()
        assertThrows(IllegalArgumentException::class.java) {
            handshake.hashPsk("short")
        }
        val hashed = handshake.hashPsk("validpsk123")
        assertNotNull(hashed)
    }

    @Test
    fun testSuccessfulHandshake() {
        val handshake = PskHandshake()
        val psk = "secretPassword123"
        val hashedPsk = handshake.hashPsk(psk)
        val responderCallsign = "RESPONDER_1"

        // Initiator sends
        val challenge = handshake.generateChallenge()
        assertEquals(32, challenge.size)

        // Responder sends
        val response = handshake.generateResponse(hashedPsk, challenge, responderCallsign)

        // Initiator verifies
        val isMatch = handshake.verifyResponse(hashedPsk, challenge, responderCallsign, response)
        assertTrue(isMatch)
    }

    @Test
    fun testFailedHandshake() {
        val handshake = PskHandshake()
        val initiatorHashedPsk = handshake.hashPsk("correctPassword123")
        val responderHashedPsk = handshake.hashPsk("wrongPassword123")
        val responderCallsign = "RESPONDER_2"

        val challenge = handshake.generateChallenge()
        val response = handshake.generateResponse(responderHashedPsk, challenge, responderCallsign)

        val isMatch = handshake.verifyResponse(initiatorHashedPsk, challenge, responderCallsign, response)
        assertFalse(isMatch)
    }

    @Test
    fun testRateLimiting() {
        val handshake = PskHandshake()
        val hashedPsk = handshake.hashPsk("secretPassword123")
        val wrongHashedPsk = handshake.hashPsk("wrongPassword123")
        val responderCallsign = "RATE_LIMITED_RESPONDER"

        val challenge = handshake.generateChallenge()
        val wrongResponse = handshake.generateResponse(wrongHashedPsk, challenge, responderCallsign)

        // 5 failed attempts
        for (i in 1..5) {
            val isMatch = handshake.verifyResponse(hashedPsk, challenge, responderCallsign, wrongResponse)
            assertFalse(isMatch)
        }

        // 6th attempt should be rate limited, even if correct
        val correctResponse = handshake.generateResponse(hashedPsk, challenge, responderCallsign)
        val isMatch = handshake.verifyResponse(hashedPsk, challenge, responderCallsign, correctResponse)
        assertFalse("Should be rate limited", isMatch)
    }
}
