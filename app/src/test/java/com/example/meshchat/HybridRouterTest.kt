package com.example.meshchat

import com.example.meshchat.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class HybridRouterTest {

    private class TestLoRaHal(val isRadioAvailable: Boolean = false) : LoRaHAL {
        val sentPackets = mutableListOf<ByteArray>()
        override fun begin(frequency: Long, sf: Int, bw: Int): Boolean = isRadioAvailable
        override fun send(data: ByteArray): Boolean {
            sentPackets.add(data)
            return isRadioAvailable
        }
        override fun receive(): ByteArray? = null
        override fun getRssi(): Int = -75
        override fun getSnr(): Float = 8.0f
        override fun isAvailable(): Boolean = isRadioAvailable
        override fun sleep() {}
        override fun wake() {}
    }

    @Test
    fun testDefaultRoutingWhenHardwareDisconnected() {
        val hal = TestLoRaHal(isRadioAvailable = false)
        val btManager = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val router = HybridRouter(hal, btManager)

        assertEquals(RadioTransport.HYBRID_AUTO, router.getActiveTransport())
    }

    @Test
    fun testRoutingWhenLoRaIsAvailable() {
        val hal = TestLoRaHal(isRadioAvailable = true)
        val btManager = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val router = HybridRouter(hal, btManager)

        assertEquals(RadioTransport.LORA_MESH, router.getActiveTransport())
    }

    @Test
    fun testPayloadEncodingAndDecodingRoundtrip() {
        val hal = TestLoRaHal(isRadioAvailable = false)
        val btManager = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val router = HybridRouter(hal, btManager)

        val originalMsg = Message(
            channelId = "ops_channel",
            text = "Status Report Alpha",
            sender = "Delta-1",
            mediaBase64 = "base64sample123"
        )

        val payloadStr = "${originalMsg.channelId}|${originalMsg.text}|${originalMsg.sender}|${originalMsg.mediaBase64 ?: ""}"
        val parsed = router.parsePayload(payloadStr.toByteArray(Charsets.UTF_8), RadioTransport.WIFI_TCP_NODE)

        assertNotNull(parsed)
        assertEquals("ops_channel", parsed?.channelId)
        assertEquals("Status Report Alpha", parsed?.text)
        assertEquals("Delta-1", parsed?.sender)
        assertEquals("base64sample123", parsed?.mediaBase64)
        assertEquals(RadioTransport.WIFI_TCP_NODE, parsed?.transport)
    }

    @Test
    fun testPriorityQueueOrdersSosAboveNormal() {
        val queue = LoRaQueue()
        val normalMsg = "Normal Message".toByteArray()
        val sosMsg = "EMERGENCY SOS".toByteArray()

        queue.enqueue(normalMsg, HybridRouter.PRIORITY_NORMAL)
        queue.enqueue(sosMsg, HybridRouter.PRIORITY_SOS)

        // Dequeue should return SOS first even though normal was enqueued first
        val firstOut = queue.dequeue()
        assertNotNull(firstOut)
        assertEquals(HybridRouter.PRIORITY_SOS, firstOut?.priority)
        assertArrayEquals(sosMsg, firstOut?.data)

        val secondOut = queue.dequeue()
        assertNotNull(secondOut)
        assertEquals(HybridRouter.PRIORITY_NORMAL, secondOut?.priority)
        assertArrayEquals(normalMsg, secondOut?.data)
    }

    @Test
    fun testSendReturnsFalseWhenNoHardwareConnected() = runBlocking {
        val hal = TestLoRaHal(isRadioAvailable = false)
        val btManager = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val router = HybridRouter(hal, btManager)

        val msg = Message(channelId = "c1", text = "Ping", sender = "Me")
        val sent = router.send(msg)

        assertFalse("Cannot send when no hardware transport is active", sent)
    }
}
