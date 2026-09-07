package com.example.meshchat

import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.HttpNodeTransport
import com.example.meshchat.data.WifiNodeScanner
import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder
import java.net.URLEncoder

class WifiHttpTransportTest {

    @Test
    fun testCandidateIpListContainsRequiredGateways() {
        val list = WifiNodeScanner.FALLBACK_IPS
        assertTrue("Must contain 192.168.4.1", list.contains("192.168.4.1"))
        assertTrue("Must contain 192.168.10.1 (ESP32 AP)", list.contains("192.168.10.1"))
        assertTrue("Must contain 192.168.20.1 (ESP8266 AP)", list.contains("192.168.20.1"))
    }

    @Test
    fun testHttpRestUrlConstructionWithSpacesSymbolsAndEmoji() {
        val transport = HttpNodeTransport()
        val ip = "192.168.4.1"
        val port = 80
        val payload = "Hello World & Friends! 🛰️ Special symbols: #,?,=,/"

        val url = transport.buildSendUrl(ip, port, payload)
        assertTrue("URL must start with http://192.168.4.1:80/send?msg=", url.startsWith("http://192.168.4.1:80/send?msg="))

        // Extract query parameter and decode
        val queryParam = url.substringAfter("msg=")
        val decoded = URLDecoder.decode(queryParam, "UTF-8")
        assertEquals("Decoded URL query parameter must match original payload", payload, decoded)
    }

    @Test
    fun testHttpTransportInitialStateIsDisconnected() {
        val transport = HttpNodeTransport()
        assertEquals(ConnectionState.DISCONNECTED, transport.connectionState.value)
        assertEquals(-1L, transport.pingLatencyMs.value)
        assertEquals(0L, transport.packetsSent.value)
        assertEquals(0L, transport.packetsReceived.value)
    }

    @Test
    fun testMessageDeduplicationLogic() {
        // Polling loop deduplication check
        var lastReceived = ""
        val receivedQueue = mutableListOf<String>()

        fun onPollResult(msg: String) {
            if (msg.isNotBlank() && msg != "No message received yet" && msg != "None" && msg != lastReceived) {
                lastReceived = msg
                receivedQueue.add(msg)
            }
        }

        onPollResult("No message received yet") // Ignored placeholder
        assertEquals(0, receivedQueue.size)

        onPollResult("Hello Mesh!") // First message
        assertEquals(1, receivedQueue.size)

        onPollResult("Hello Mesh!") // Duplicate polled message -> ignored!
        assertEquals(1, receivedQueue.size)

        onPollResult("New Second Message") // New message -> added!
        assertEquals(2, receivedQueue.size)
    }
}
