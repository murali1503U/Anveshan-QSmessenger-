package com.example.meshchat.security

enum class SecurityLevel(val level: Int) {
    LEVEL_1(1),
    LEVEL_2(2),
    LEVEL_3(3),
    LEVEL_4(4)
}

data class CryptoOperations(
    val encryption: String,
    val signature: String,
    val keyExchange: String,
    val description: String,
    val useCases: String
)

object SecurityLevelMapper {
    fun mapLevelToOperations(level: SecurityLevel): CryptoOperations {
        return when (level) {
            SecurityLevel.LEVEL_1 -> CryptoOperations(
                encryption = "No encryption (Plaintext payload)",
                signature = "No signature",
                keyExchange = "None",
                description = "Fast",
                useCases = "casual chatter, public broadcast"
            )
            SecurityLevel.LEVEL_2 -> CryptoOperations(
                encryption = "AES-256-GCM",
                signature = "No signature",
                keyExchange = "PSK-derived key",
                description = "Standard Protection",
                useCases = "private messages, media"
            )
            SecurityLevel.LEVEL_3 -> CryptoOperations(
                encryption = "AES-256-GCM",
                signature = "ML-DSA-65 signature on every message",
                keyExchange = "PSK-derived key",
                description = "Enhanced Protection",
                useCases = "sensitive data, coordinates, credentials"
            )
            SecurityLevel.LEVEL_4 -> CryptoOperations(
                encryption = "AES-256-GCM encryption with derived key",
                signature = "ML-DSA-65 signature on every message",
                keyExchange = "ML-KEM-768 key exchange, PSK challenge-response before ML-KEM",
                description = "Maximum Protection",
                useCases = "critical infrastructure, master keys, long-term secrets"
            )
        }
    }
}
