package com.example.meshchat.crypto

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SessionKeyDeriver {

    private const val MAC_ALGORITHM = "HmacSHA512"

    /**
     * Derives a 32-byte session key using HKDF-SHA512.
     *
     * @param sharedSecret The input keying material (IKM).
     * @param salt The salt value (e.g., SHA-512(senderCallsign + receiverCallsign)).
     * @param info Context and application specific information.
     * @return 32-byte derived session key.
     */
    fun deriveSessionKey(sharedSecret: ByteArray, salt: ByteArray, info: ByteArray): ByteArray {
        // HKDF-Extract
        val mac = Mac.getInstance(MAC_ALGORITHM)
        val saltKey = SecretKeySpec(if (salt.isEmpty()) ByteArray(64) else salt, MAC_ALGORITHM)
        mac.init(saltKey)
        val prk = mac.doFinal(sharedSecret)

        // HKDF-Expand
        // Since we only need 32 bytes (which is <= 64 bytes output of SHA-512), 
        // we only need to compute T(1).
        val prkKey = SecretKeySpec(prk, MAC_ALGORITHM)
        mac.init(prkKey)
        mac.update(info)
        mac.update(1.toByte())
        val t1 = mac.doFinal()

        // We need 32 bytes for the session key
        val derivedKey = ByteArray(32)
        System.arraycopy(t1, 0, derivedKey, 0, 32)
        
        return derivedKey
    }
}
