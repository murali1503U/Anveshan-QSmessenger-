package com.example.meshchat

import com.example.meshchat.ai.QsvmClassifier
import org.junit.Assert.assertTrue
import org.junit.Test

class QsvmExtremeAdversarialTest {

    private val qsvm = QsvmClassifier()

    @Test
    fun testZeroWidthObfuscation() {
        // Zero-width space (\u200B) injected between every letter of "master_key"
        val obfuscated = "m\u200Ba\u200Bs\u200Bt\u200Be\u200Br\u200B_\u200Bk\u200Be\u200By"
        val result = qsvm.classify("Initiate $obfuscated exchange")
        assertTrue("Expected Level >= 3 for zero-width obfuscated master_key", result.decision.level >= 3)
    }

    @Test
    fun testZalgoAndSpacedObfuscation() {
        // Spaced out with underscores, dots, and zalgo-like symbols
        val spaced = "p.a s_s_w.o. r_d is 12345"
        val result = qsvm.classify("my $spaced")
        assertTrue("Expected Level >= 3 for heavily spaced out password", result.decision.level >= 3)
    }

    @Test
    fun testLeetSpeakExtreme() {
        // "t0ps3cr3t nuc134r"
        val leet = "t0ps3cr3t nuc134r code"
        val result = qsvm.classify("Send the $leet")
        assertTrue("Expected Level >= 3 for extreme leetspeak", result.decision.level >= 3)
    }

    @Test
    fun testHiddenPayloadInBenignText() {
        // An enormous block of benign text with a hidden Base64 token
        val benignPrefix = "This is a completely normal conversation about the weather. ".repeat(50)
        val hiddenToken = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"
        val benignSuffix = " By the way, how is your dog doing?".repeat(50)
        
        val result = qsvm.classify(benignPrefix + hiddenToken + benignSuffix)
        assertTrue("Expected Level >= 3 for hidden JWT payload in massive text block", result.decision.level >= 3)
    }
}
