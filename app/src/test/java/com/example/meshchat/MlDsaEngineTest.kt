package com.example.meshchat

import com.example.meshchat.crypto.MlDsaEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MlDsaEngineTest {

    @Test
    fun testGenerateKeyPair() = runBlocking {
        val engine = MlDsaEngine()
        val result = engine.generateKeyPair()
        assertTrue(result.isSuccess)
        
        val keyPair = result.getOrThrow()
        assertEquals(1952, keyPair.publicKey.size)
        assertEquals(4032, keyPair.privateKey.size)
    }

    @Test
    fun testSignAndVerify() = runBlocking {
        val engine = MlDsaEngine()
        val keyPair = engine.generateKeyPair().getOrThrow()
        val message = "Test message".toByteArray()
        
        val signResult = engine.sign(keyPair.privateKey, message)
        assertTrue(signResult.isSuccess)
        
        val signature = signResult.getOrThrow()
        assertEquals(3309, signature.size)

        val verifyResult = engine.verify(keyPair.publicKey, message, signature)
        assertTrue(verifyResult.isSuccess)
        assertTrue(verifyResult.getOrThrow())
    }
}
