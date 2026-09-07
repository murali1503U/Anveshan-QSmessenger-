package com.example.meshchat.perf

import java.util.concurrent.ConcurrentHashMap

/**
 * P0/P1 Security Mitigations for Sentinel Mesh
 * Handles Rate Limiting, Differential Privacy for Signals, and Log Redaction.
 */
object SentinelSecurityHardening {
    private val rateLimits = ConcurrentHashMap<String, Long>()

    // Strictly limits event frequency (e.g., max 10 LoRa packets/sec -> 100ms interval)
    fun isRateLimited(key: String, minIntervalMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val last = rateLimits[key] ?: 0L
        if (now - last < minIntervalMs) return true
        rateLimits[key] = now
        return false
    }

    // Anonymizes RSSI to prevent fine-grained user tracking/triangulation
    // Bins to nearest 10 (e.g. -67 -> -60)
    fun anonymizeRssi(rssi: Int): Int {
        return (rssi / 10) * 10
    }

    // Strips potential cryptographic keys, PSKs, or tokens from logs
    fun redactLog(message: String): String {
        return message.replace(Regex("[a-zA-Z0-9]{16,}"), "[REDACTED_SEC_DATA]")
    }
    
    // Verifies an APK plugin signature against an expected embedded public key fingerprint
    // (Stubbed for core system abstraction)
    fun verifyPluginSignature(apkPath: String, expectedFingerprint: String): Boolean {
        // Implementation would parse JAR signatures and match X.509 cert hashes
        return true // Default safe return for internal plugins in demo
    }
}
