package com.example.meshchat.ai

data class SecurityDecision(
    val level: Int, // 1=Plaintext, 2=AES-256, 3=Session Keys (Forward Secrecy), 4=Post-Quantum Kyber
    val levelName: String,
    val confidence: Float,
    val reason: String,
    val sensitiveFactors: List<String>,
    val executionSource: String,
    val estimatedPacketOverheadBytes: Int,
    val latencyMs: Long
)
