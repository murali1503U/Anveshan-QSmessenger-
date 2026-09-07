package com.example.meshchat

import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.IoTDevice
import com.example.meshchat.data.LoRaMeshService
import com.example.meshchat.data.RadioTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoRaMeshIntegrationTest {

    private lateinit var meshService: LoRaMeshService

    @Before
    fun setup() {
        meshService = LoRaMeshService()
    }

    @Test
    fun testInitialConnectionStateIsDisconnected() = runTest {
        assertEquals(ConnectionState.DISCONNECTED, meshService.connectionState.value)
        assertEquals(ConnectionState.DISCONNECTED, meshService.wifiConnectionState.value)
    }

    @Test
    fun testConnectToDeviceRejectsWithoutPreSharedCodeInProduction() = runTest {
        val dummyDevice = IoTDevice("1", "Dummy", -50, true, "10m", RadioTransport.BLUETOOTH_DIRECT)
        val connected = meshService.connectToDevice(dummyDevice, preSharedCode = "")
        assertFalse("Should reject connection without pre-shared code in production", connected)
        assertEquals(ConnectionState.DISCONNECTED, meshService.connectionState.value)
    }

    @Test
    fun testResolveActiveTransportResolution() {
        // When LoRa is available and Wi-Fi is disconnected
        val (transport, detail) = meshService.resolveActiveTransport(RadioTransport.HYBRID_AUTO, true)
        assertTrue(transport == RadioTransport.LORA_MESH || transport == RadioTransport.BLUETOOTH_DIRECT)
        assertNotNull(detail)
    }
}
