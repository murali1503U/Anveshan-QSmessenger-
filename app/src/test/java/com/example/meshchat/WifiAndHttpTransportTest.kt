package com.example.meshchat

import com.example.meshchat.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.URLEncoder
import java.util.UUID

class WifiAndHttpTransportTest {

    private class MockLoRaHal(val available: Boolean = false) : LoRaHAL {
        override fun begin(frequency: Long, sf: Int, bw: Int): Boolean = available
        override fun send(data: ByteArray): Boolean = available
        override fun receive(): ByteArray? = null
        override fun getRssi(): Int = -80
        override fun getSnr(): Float = 9.5f
        override fun isAvailable(): Boolean = available
        override fun sleep() {}
        override fun wake() {}
    }

    @Test
    fun testFallbackIpListContainsRequiredGateways() {
        val list = WifiNodeScanner.FALLBACK_IPS
        assertTrue("Must contain 192.168.4.1", list.contains("192.168.4.1"))
        assertTrue("Must contain 192.168.10.1 (ESP32 AP)", list.contains("192.168.10.1"))
        assertTrue("Must contain 192.168.20.1 (ESP8266 AP)", list.contains("192.168.20.1"))
    }

    @Test
    fun testHttpPayloadUrlEncoding() {
        val rawText = "channel_01|Hello LoRa Mesh! Special chars: &?=#|UserCallsign|"
        val encoded = URLEncoder.encode(rawText, "UTF-8")
        assertNotNull(encoded)
        assertFalse("Encoded payload should not contain raw unencoded ampersands", encoded.contains("&") && !encoded.contains("%26"))
        assertTrue(encoded.contains("%7C")) // '|' encoded
    }

    @Test
    fun testHybridRouterPriorityRouting() = runBlocking {
        val unavailableHal = MockLoRaHal(available = false)
        val dummyBt = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val httpTransport = HttpNodeTransport()
        val tcpTransport = WifiTcpTransport()

        val router = HybridRouter(unavailableHal, dummyBt, tcpTransport, httpTransport)

        // When no radio is active, defaults to HYBRID_AUTO
        assertEquals(RadioTransport.HYBRID_AUTO, router.getActiveTransport())

        // When LoRa HAL is active, routes to LORA_MESH
        val activeHal = MockLoRaHal(available = true)
        val routerWithLoRa = HybridRouter(activeHal, dummyBt, tcpTransport, httpTransport)
        assertEquals(RadioTransport.LORA_MESH, routerWithLoRa.getActiveTransport())
    }

    @Test
    fun testPayloadParsing() {
        val dummyHal = MockLoRaHal(available = false)
        val dummyBt = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val router = HybridRouter(dummyHal, dummyBt)

        val raw = "global_broadcast|Emergency SOS beacon|NodeAlpha|mediaData123".toByteArray(Charsets.UTF_8)
        val parsed = router.parsePayload(raw, RadioTransport.WIFI_TCP_NODE)

        assertNotNull(parsed)
        assertEquals("global_broadcast", parsed?.channelId)
        assertEquals("Emergency SOS beacon", parsed?.text)
        assertEquals("NodeAlpha", parsed?.sender)
        assertEquals("mediaData123", parsed?.mediaBase64)
        assertEquals(RadioTransport.WIFI_TCP_NODE, parsed?.transport)
    }

    @Test
    fun testBluetoothRejectsESP8266() = runBlocking {
        val btManager = RealBluetoothManager(null, UUID.randomUUID(), "TestApp")
        val adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter() ?: return@runBlocking
        val dev = adapter.getRemoteDevice("00:11:22:33:44:55")
        
        // Blank PSK must be rejected
        val resultNoPsk = btManager.attemptConnect(dev, "")
        assertFalse("Blank PSK must be rejected", resultNoPsk)
    }
}
