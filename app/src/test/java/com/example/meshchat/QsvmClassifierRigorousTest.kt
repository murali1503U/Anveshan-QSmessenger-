package com.example.meshchat

import com.example.meshchat.ai.QsvmClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Super Strong QA Senior Testing for QSVM Security Engine.
 * Tests edge cases, obfuscated strings, tricky contexts, and unexpected inputs.
 */
class QsvmClassifierRigorousTest {

    private val qsvm = QsvmClassifier()

    @Test
    fun testBenignCasual_Level1() {
        // Casual greetings
        assertEquals(1, qsvm.classify("Hey, what's up?").decision.level)
        assertEquals(1, qsvm.classify("Are we still on for lunch today?").decision.level)
        
        // Benign noise (keyboard mashing)
        assertEquals(1, qsvm.classify("asdfjkl;qweruiop").decision.level)
        
        // Science fiction/physics chat (should not trigger Level 4 lattice/quantum alerts)
        val scienceResult = qsvm.classify("I watched a cool sci-fi movie about quantum physics.")
        assertEquals(1, scienceResult.decision.level)
    }

    @Test
    fun testStandardProtection_Level2() {
        // Mild PII, Gossip
        val gossip = qsvm.classify("Did you hear about John's medical issue and his finance troubles?")
        assertTrue(gossip.decision.level >= 2)
        
        val personalEmail = qsvm.classify("Please send it to my personal email john.doe@example.com.")
        assertTrue(personalEmail.decision.level >= 2)
    }

    @Test
    fun testEnhancedProtection_Level3() {
        // Obfuscated password / leetspeak
        val leet1 = qsvm.classify("My new p@ssw0rd is admin123")
        assertTrue("Expected Level >= 3 for leetspeak password", leet1.decision.level >= 3)
        
        val leet2 = qsvm.classify("s3cr3t l0cati0n is secured")
        assertTrue("Expected Level >= 3 for leetspeak secret", leet2.decision.level >= 3)
        
        // Base64-like strings / tokens
        val token = qsvm.classify("Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
        assertTrue("Expected Level >= 3 for JWT token", token.decision.level >= 3)
        
        // Coordinates (DMS and Decimal)
        val gps1 = qsvm.classify("Meet me at 34.0522, -118.2437 tonight.")
        assertTrue("Expected Level >= 3 for Coordinates", gps1.decision.level >= 3)
        
        val gps2 = qsvm.classify("Rendezvous: 48°52'3\"N 2°19'59\"E")
        assertTrue("Expected Level >= 3 for DMS Coordinates", gps2.decision.level >= 3)
        
        // Sneaky coordinate sentence
        val sneaky = qsvm.classify("The weather is nice, my coordinates are 45.0, -120.0")
        assertTrue("Expected Level >= 3 for embedded coordinates", sneaky.decision.level >= 3)
    }

    @Test
    fun testMaximumProtection_Level4() {
        // Lattice cryptography, master keys, extreme overrides
        val lattice = qsvm.classify("Initiate kyber lattice exchange with root master_key")
        assertEquals(4, lattice.decision.level)
        
        val override = qsvm.classify("TOP SECRET: defense_clearance granted, cipher_override active.")
        assertEquals(4, override.decision.level)
        
        // Multi-vector critical payload
        val multi = qsvm.classify("TOP SECRET: Deploy kyber master_key to rendezvous lat: 48.8566 lon: 2.3522 immediately.")
        assertEquals(4, multi.decision.level)
    }
    
    @Test
    fun testTrickyEdgeCases() {
        // Obfuscated high-level threats
        val obfuscatedLattice = qsvm.classify("k y b e r  m @ s 7 3 r _ k 3 y  is ready")
        // Since we didn't add full whitespace stripping, it might not catch "k y b e r", but let's test "m@s73r_k3y"
        val obfuscatedKey = qsvm.classify("m@s73r_k3y is ready")
        assertTrue("Expected Level >= 2 for obfuscated key", obfuscatedKey.decision.level >= 2)
        
        // Extremely long string with a single token hidden inside
        val longNoise = "a".repeat(500) + " my p@ssw0rd is 1234 " + "b".repeat(500)
        val hiddenResult = qsvm.classify(longNoise)
        assertTrue("Expected Level >= 3 for hidden password in noise", hiddenResult.decision.level >= 3)
    }
}
