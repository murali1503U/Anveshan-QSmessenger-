package com.example.meshchat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secure_sessions")
data class SecureSession(
    @PrimaryKey val sessionId: String,
    val userCallsign: String = "SENTINEL-7A9B",
    val nodeId: String = "0x7A9B",
    val peerId: String = "global_mesh",
    val peerName: String = "Broadcast Mesh",
    val securityLevel: Int = 0, // 0=Auto, 1=Plain, 2=AES, 3=Session, 4=Post-Quantum
    val isQsvmActive: Boolean = true,
    val securityEngineMode: String = "QSVM", // "QSVM" (Quantum Support Vector Machine), "CLOUD" (Gemini)
    val termuxEndpoint: String = "http://127.0.0.1:11434",
    val termuxSelectedModel: String = "qsvm",
    val isQsvmReady: Boolean = true,
    val selectedModelName: String = "QSVM Quantum Kernel",
    val isDeveloperMode: Boolean = false,
    
    val isSetupCompleted: Boolean = false,
    val sessionKey: String = "0x7F4A9B2C1D8E3F60A1B2C3D4E5F67890",
    val ratchetCounter: Int = 0,
    val pqSharedSecret: String = "kyber768_sec_seed_9x8f2a4c1b",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastRotatedAt: Long = System.currentTimeMillis()
)
