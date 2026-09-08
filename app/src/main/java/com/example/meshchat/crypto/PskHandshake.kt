package com.example.meshchat.crypto

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.ConcurrentHashMap

class PskHandshake {
    
    companion object {
        const val CHALLENGE_SIZE = 32
        const val MAX_ATTEMPTS_PER_MINUTE = 5
    }

    private val secureRandom = SecureRandom()
    
    // rate limiting: mapping of responderCallsign to list of timestamps
    private val attempts = ConcurrentHashMap<String, MutableList<Long>>()

    // Generate random challenge for the initiator to send
    fun generateChallenge(): ByteArray {
        val challenge = ByteArray(CHALLENGE_SIZE)
        secureRandom.nextBytes(challenge)
        return challenge
    }

    // Hash the PSK for storage and usage
    fun hashPsk(psk: String): ByteArray {
        require(psk.length >= 8) { "PSK must be at least 8 characters long" }
        val digest = MessageDigest.getInstance("SHA-512")
        return digest.digest(psk.toByteArray(Charsets.UTF_8))
    }

    // Responder generates the response
    fun generateResponse(hashedPsk: ByteArray, challenge: ByteArray, responderCallsign: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        val secretKey = SecretKeySpec(hashedPsk, "HmacSHA512")
        mac.init(secretKey)
        
        mac.update(challenge)
        mac.update(responderCallsign.toByteArray(Charsets.UTF_8))
        return mac.doFinal()
    }

    // Initiator verifies the response
    fun verifyResponse(hashedPsk: ByteArray, challenge: ByteArray, responderCallsign: String, receivedResponse: ByteArray): Boolean {
        // Rate limiting check
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000
        
        val callsignAttempts = attempts.getOrPut(responderCallsign) { mutableListOf() }
        synchronized(callsignAttempts) {
            callsignAttempts.removeAll { it < oneMinuteAgo }
            if (callsignAttempts.size >= MAX_ATTEMPTS_PER_MINUTE) {
                // Reject connection, log attempt, rate limit
                println("RATE LIMIT EXCEEDED: Handshake attempts for $responderCallsign")
                return false
            }
        }

        val expectedResponse = generateResponse(hashedPsk, challenge, responderCallsign)
        val isMatch = MessageDigest.isEqual(expectedResponse, receivedResponse)

        if (!isMatch) {
            println("HANDSHAKE REJECTED: invalid response for $responderCallsign")
            synchronized(callsignAttempts) {
                callsignAttempts.add(now)
            }
            return false
        }

        return true
    }
}
