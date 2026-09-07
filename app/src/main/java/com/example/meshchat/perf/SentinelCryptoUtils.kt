package com.example.meshchat.perf

/**
 * Constant-time cryptographic operations to prevent timing attacks.
 */
object SentinelCryptoUtils {
    /**
     * Compares two byte arrays in constant time to prevent timing side-channel attacks.
     * @return true if arrays are equal, false otherwise
     */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].toInt() xor b[i].toInt())
        }
        return result == 0
    }
}
