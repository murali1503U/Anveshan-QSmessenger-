package com.example.meshchat

import com.example.meshchat.crypto.MlKemEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MlKemEngineTest {

    @Test
    fun testGenerateKeyPair() = runBlocking {
        val engine = MlKemEngine()
        val result = engine.generateKeyPair()
        assertTrue(result.isSuccess)
        
        val keyPair = result.getOrThrow()
        assertEquals(1184, keyPair.publicKey.size)
        assertEquals(2400, keyPair.privateKey.size)
    }

    @Test
    fun testEncapsulateDecapsulate() = runBlocking {
        val engine = MlKemEngine()
        val keyPair = engine.generateKeyPair().getOrThrow()
        
        val encResult = engine.encapsulate(keyPair.publicKey)
        assertTrue(encResult.isSuccess)
        
        val (sharedSecret, ciphertext) = encResult.getOrThrow()
        assertEquals(32, sharedSecret.size)
        assertEquals(1088, ciphertext.size)

        val decResult = engine.decapsulate(keyPair.privateKey, ciphertext)
        assertTrue(decResult.isSuccess)
        
        val decryptedSecret = decResult.getOrThrow()
        assertEquals(32, decryptedSecret.size)
    }
}
