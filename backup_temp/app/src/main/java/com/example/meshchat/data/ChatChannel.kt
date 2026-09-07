package com.example.meshchat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ChannelType {
    BROADCAST,
    DIRECT,
    GROUP
}

@Entity(tableName = "chat_channels")
data class ChatChannel(
    @PrimaryKey val channelId: String,
    val name: String,
    val description: String = "",
    val type: ChannelType = ChannelType.DIRECT,
    val members: String = "", // Comma-separated names or IDs e.g. "Alice, Bob, Me"
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val avatarColorSeed: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val isDemo: Boolean = false,
    val encryptionKey: String = "0x7F4A9B2C1D8E3F60A1B2C3D4E5F67890",
    val securityLevel: Int = 0, // 0=Auto, 1=Plain, 2=AES, 3=Session, 4=Post-Quantum
    val preferredTransport: RadioTransport = RadioTransport.HYBRID_AUTO
)
