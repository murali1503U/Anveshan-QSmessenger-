package com.example.meshchat.session

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SessionState {
    CREATED, PSK_VERIFIED, ML_KEM_COMPLETE, ACTIVE, EXPIRED
}

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val sessionId: String = UUID.randomUUID().toString(),
    val peerCallsign: String,
    val securityLevel: Int,
    var mlKemPublicKey: String? = null, // Base64 encoded
    var mlDsaPublicKey: String? = null, // Base64 encoded
    var encryptedSessionKeyId: String? = null, // ID to lookup in KeyStorageManager
    var sequenceCounter: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    var lastUsedAt: Long = System.currentTimeMillis(),
    var messageCount: Int = 0,
    var isExpired: Boolean = false,
    var state: SessionState = SessionState.CREATED
)
