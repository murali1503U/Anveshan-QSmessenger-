package com.example.meshchat.data

import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom

class ChatChannelRepository(private val chatChannelDao: ChatChannelDao) {
    val allChannels: Flow<List<ChatChannel>> = chatChannelDao.getAllChannelsFlow()

    

    fun getChannelFlow(channelId: String): Flow<ChatChannel?> = chatChannelDao.getChannelFlow(channelId)

    suspend fun getChannel(channelId: String): ChatChannel? = chatChannelDao.getChannel(channelId)

    suspend fun initializeDefaultChannelsIfNeeded(isDemo: Boolean) {
        val count = chatChannelDao.getChannelCountByDemo(isDemo)
        if (count == 0) {
            if (isDemo) {
                val demoBroadcast = ChatChannel(
                    channelId = "demo_broadcast",
                    name = "Global Mesh Broadcast",
                    description = "Public broadcast channel for simulated mesh peers",
                    type = ChannelType.BROADCAST,
                    members = "All Simulated Nodes",
                    lastMessageText = "LoRa mesh simulation active. AI peers online.",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    avatarColorSeed = 101L,
                    isPinned = true,
                    isDemo = true,
                    securityLevel = 0
                )

                val emergencyGroup = ChatChannel(
                    channelId = "demo_emergency_sos",
                    name = "Emergency SOS & Alerts",
                    description = "High-priority mesh emergency relay channel with automated SOS dispatch",
                    type = ChannelType.GROUP,
                    members = "Emergency Responders, Alpha, Beta",
                    lastMessageText = "Standby mode: automated beacon enabled",
                    lastMessageTimestamp = System.currentTimeMillis() - 60000,
                    avatarColorSeed = 202L,
                    isPinned = true,
                    isDemo = true,
                    securityLevel = 3
                )

                val alphaDirect = ChatChannel(
                    channelId = "demo_peer_alpha",
                    name = "Node-Alpha (LoRa)",
                    description = "Direct peer simulation with Node-Alpha",
                    type = ChannelType.DIRECT,
                    members = "Node-Alpha, Me",
                    lastMessageText = "Keys exchanged. Ready for direct packets.",
                    lastMessageTimestamp = System.currentTimeMillis() - 120000,
                    avatarColorSeed = 303L,
                    isPinned = false,
                    isDemo = true,
                    securityLevel = 2
                )

                chatChannelDao.insertOrUpdate(demoBroadcast)
                chatChannelDao.insertOrUpdate(emergencyGroup)
                chatChannelDao.insertOrUpdate(alphaDirect)
            } else {
                // NORMAL MODE: Clean slate!
                // Only the fundamental Global Mesh Broadcast channel is pre-seeded.
                // NO pre-made dummy groups or fake peer chats.
                val broadcastChannel = ChatChannel(
                    channelId = "global_broadcast",
                    name = "Global Mesh Broadcast",
                    description = "Public mesh broadcast channel for nearby nodes",
                    type = ChannelType.BROADCAST,
                    members = "All Nearby Nodes",
                    lastMessageText = "Mesh network ready for communication.",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    avatarColorSeed = 101L,
                    isPinned = true,
                    isDemo = false,
                    securityLevel = 0
                )

                chatChannelDao.insertOrUpdate(broadcastChannel)
            }
        }
    }

    suspend fun createChannel(
        name: String,
        description: String,
        type: ChannelType,
        members: String,
        isDemo: Boolean = false,
        securityLevel: Int = 0,
        preferredTransport: RadioTransport = RadioTransport.HYBRID_AUTO,
        sharedSecret: String = ""
    ): ChatChannel {
        val prefix = if (isDemo) "demo_" else ""
        val typeStr = if (type == ChannelType.GROUP) "group" else "direct"
        val channelId = "${prefix}${typeStr}_" + System.currentTimeMillis() + "_" + (1000..9999).random()
        
        val encKey = if (sharedSecret.isNotBlank()) {
            val hash = com.example.meshchat.perf.SentinelCryptoConscrypt.sha512(sharedSecret.toByteArray(Charsets.UTF_8))
            "0x" + hash.joinToString("") { "%02X".format(it) }
        } else {
            generateRandomHexKey(16)
        }

        val channel = ChatChannel(
            channelId = channelId,
            name = name,
            description = description,
            type = type,
            members = members,
            lastMessageText = "Chat created.",
            lastMessageTimestamp = System.currentTimeMillis(),
            avatarColorSeed = System.currentTimeMillis(),
            isDemo = isDemo,
            encryptionKey = encKey,
            securityLevel = securityLevel,
            preferredTransport = preferredTransport
        )
        chatChannelDao.insertOrUpdate(channel)
        return channel
    }

    suspend fun createDirectChannelDeterministic(
        myCallsign: String,
        peerCallsign: String,
        securityLevel: Int,
        preferredTransport: RadioTransport,
        sharedSecret: String
    ): ChatChannel {
        val sortedNames = listOf(myCallsign, peerCallsign).sorted().joinToString("_")
        val channelId = "direct_$sortedNames"
        
        val encKey = if (sharedSecret.isNotBlank()) {
            val hash = com.example.meshchat.perf.SentinelCryptoConscrypt.sha512(sharedSecret.toByteArray(Charsets.UTF_8))
            "0x" + hash.joinToString("") { "%02X".format(it) }
        } else {
            generateRandomHexKey(16)
        }

        val channel = ChatChannel(
            channelId = channelId,
            name = peerCallsign,
            description = "Direct peer-to-peer secure channel",
            type = ChannelType.DIRECT,
            members = "$peerCallsign, Me",
            lastMessageText = "Chat created.",
            lastMessageTimestamp = System.currentTimeMillis(),
            avatarColorSeed = System.currentTimeMillis(),
            isDemo = false,
            encryptionKey = encKey,
            securityLevel = securityLevel,
            preferredTransport = preferredTransport
        )
        chatChannelDao.insertOrUpdate(channel)
        return channel
    }

    suspend fun updateLastMessage(channelId: String, text: String, timestamp: Long = System.currentTimeMillis()) {
        chatChannelDao.updateLastMessage(channelId, text, timestamp)
    }

    suspend fun clearUnread(channelId: String) {
        chatChannelDao.clearUnreadCount(channelId)
    }

    suspend fun incrementUnread(channelId: String) {
        chatChannelDao.incrementUnreadCount(channelId)
    }

    suspend fun togglePin(channelId: String, currentPinned: Boolean) {
        chatChannelDao.setPinned(channelId, !currentPinned)
    }

    suspend fun deleteChannel(channelId: String) {
        chatChannelDao.deleteChannel(channelId)
    }

    suspend fun clearAllLastMessages() {
        chatChannelDao.clearAllLastMessages()
    }

    private fun generateRandomHexKey(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return "0x" + bytes.joinToString("") { "%02X".format(it) }
    }
}
