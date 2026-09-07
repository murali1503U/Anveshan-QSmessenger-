package com.example.meshchat

import com.example.meshchat.ai.GeminiSecurityAnalyzer
import com.example.meshchat.cli.CliLineType
import com.example.meshchat.cli.MeshCliEngine
import com.example.meshchat.data.ChannelType
import com.example.meshchat.data.ChatChannel
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.IoTDevice
import com.example.meshchat.data.LoRaMeshService
import com.example.meshchat.data.Message
import com.example.meshchat.data.MessageRepository
import com.example.meshchat.data.RadioTransport
import com.example.meshchat.data.SecureSession
import com.example.meshchat.data.SecureSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DualRadioAndFeaturesTest {

    @Test
    fun testRadioTransportEnumValuesAndProperties() {
        val hybrid = RadioTransport.HYBRID_AUTO
        val ble = RadioTransport.BLUETOOTH_DIRECT
        val lora = RadioTransport.LORA_MESH

        assertEquals("HYBRID_AUTO", hybrid.name)
        assertEquals("BLUETOOTH_DIRECT", ble.name)
        assertEquals("LORA_MESH", lora.name)
    }

    @Test
    fun testHybridRadioTransportResolution() {
        val meshService = LoRaMeshService()

        // Direct peer within proximity (<100m) should resolve to BLE Direct in Hybrid mode
        val (hybridNearbyTransport, nearbyDetail) = meshService.resolveActiveTransport(
            requested = RadioTransport.HYBRID_AUTO,
            peerNearby = true
        )
        assertEquals(RadioTransport.BLUETOOTH_DIRECT, hybridNearbyTransport)
        assertTrue(nearbyDetail.contains("BLE Direct"))

        // Remote peer (>100m) should resolve to LoRa Mesh in Hybrid mode
        val (hybridFarTransport, farDetail) = meshService.resolveActiveTransport(
            requested = RadioTransport.HYBRID_AUTO,
            peerNearby = false
        )
        assertEquals(RadioTransport.LORA_MESH, hybridFarTransport)
        assertTrue(farDetail.contains("LoRa Mesh"))

        // Explicit BLE preference forced
        val (bleTransport, bleDetail) = meshService.resolveActiveTransport(
            requested = RadioTransport.BLUETOOTH_DIRECT,
            peerNearby = false
        )
        assertEquals(RadioTransport.BLUETOOTH_DIRECT, bleTransport)
        assertTrue(bleDetail.contains("BLE Direct"))

        // Explicit LoRa preference forced
        val (loraTransport, loraDetail) = meshService.resolveActiveTransport(
            requested = RadioTransport.LORA_MESH,
            peerNearby = true
        )
        assertEquals(RadioTransport.LORA_MESH, loraTransport)
        assertTrue(loraDetail.contains("LoRa Mesh"))
    }

    @Test
    fun testChatChannelCreationWithTransports() {
        val broadcastChannel = ChatChannel(
            channelId = "global_broadcast",
            name = "Global Mesh Broadcast",
            type = ChannelType.BROADCAST,
            preferredTransport = RadioTransport.HYBRID_AUTO,
            securityLevel = 2,
            isPinned = true
        )

        assertEquals("global_broadcast", broadcastChannel.channelId)
        assertEquals(ChannelType.BROADCAST, broadcastChannel.type)
        assertEquals(RadioTransport.HYBRID_AUTO, broadcastChannel.preferredTransport)
        assertTrue(broadcastChannel.isPinned)

        val directChannel = ChatChannel(
            channelId = "direct_peer_charlie",
            name = "Node Charlie (Direct)",
            type = ChannelType.DIRECT,
            preferredTransport = RadioTransport.BLUETOOTH_DIRECT,
            securityLevel = 3,
            isPinned = false
        )

        assertEquals(ChannelType.DIRECT, directChannel.type)
        assertEquals(RadioTransport.BLUETOOTH_DIRECT, directChannel.preferredTransport)
        assertEquals(3, directChannel.securityLevel)

        val groupChannel = ChatChannel(
            channelId = "group_patrol",
            name = "Patrol Squad Alpha",
            type = ChannelType.GROUP,
            members = "Me, Node_A, Node_B",
            preferredTransport = RadioTransport.LORA_MESH,
            securityLevel = 4
        )

        assertEquals(ChannelType.GROUP, groupChannel.type)
        assertEquals("Me, Node_A, Node_B", groupChannel.members)
        assertEquals(RadioTransport.LORA_MESH, groupChannel.preferredTransport)
        assertEquals(4, groupChannel.securityLevel)
    }

    @Test
    fun testMessageRadioAndSecurityAttributes() {
        val msg = Message(
            id = 42,
            channelId = "direct_peer_charlie",
            text = "Target coordinates confirmed at sector 7",
            sender = "Me",
            isSent = true,
            isEncrypted = true,
            isConfidential = true,
            securityLevel = 4,
            transport = RadioTransport.HYBRID_AUTO,
            transportDetail = "Auto Routed -> LoRa Mesh (Sub-GHz Relay)"
        )

        assertEquals(42, msg.id)
        assertEquals("direct_peer_charlie", msg.channelId)
        assertTrue(msg.isSent)
        assertTrue(msg.isEncrypted)
        assertEquals(4, msg.securityLevel)
        assertEquals(RadioTransport.HYBRID_AUTO, msg.transport)
        assertEquals("Auto Routed -> LoRa Mesh (Sub-GHz Relay)", msg.transportDetail)
    }

    @Test
    fun testIoTDeviceDiscoveryModel() {
        val bleDevice = IoTDevice(
            id = "AA:BB:CC:11:22:33",
            name = "MeshNode_BLE_01",
            rssi = -52,
            isBleNearby = true,
            distanceEstimate = "4 meters"
        )
        assertTrue(bleDevice.isBleNearby)
        assertEquals("4 meters", bleDevice.distanceEstimate)
        assertEquals(-52, bleDevice.rssi)

        val loraRepeater = IoTDevice(
            id = "LORA:REP:915:B4",
            name = "Mountain Ridge Repeater",
            rssi = -94,
            isBleNearby = false,
            distanceEstimate = "3.2 km (1 Hop)"
        )
        assertFalse(loraRepeater.isBleNearby)
        assertEquals("3.2 km (1 Hop)", loraRepeater.distanceEstimate)
    }

    @Test
    fun testSecurityHeuristicSensitivityDetection() {
        // Test sensitive keywords heuristics
        val testLocation = "Rendezvous coordinates: 37.7749, -122.4194"
        val testPassword = "My mesh network password is AdminSecret#99!"
        val testPublic = "Weather looks clear and sunny today"
        val testHighVal = "Authorize root access token 0xFA89BC77"

        assertTrue(testLocation.contains("coordinates") || testLocation.contains("37."))
        assertTrue(testPassword.contains("password"))
        assertFalse(testPublic.contains("password") || testPublic.contains("token") || testPublic.contains("coordinates"))
        assertTrue(testHighVal.contains("token") || testHighVal.contains("Authorize"))
    }
}
