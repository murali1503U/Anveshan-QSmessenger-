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
    fun testStartScanningDiscoversEsp32AndLoRaNodes() = runTest {
        assertEquals(ConnectionState.DISCONNECTED, meshService.connectionState.value)
        
        // We can't perfectly test delays in a non-injected dispatcher without more setup, 
        // but we can just run the suspend function if it completes. 
        // Wait, startScanning has a delay(1200), runTest might auto-advance it.
        meshService.startScanning()
        
        val devices = meshService.discoveredDevices.value
        assertTrue("Should discover multiple IoT devices", devices.isNotEmpty())
        
        val bleNode = devices.find { it.radioType == RadioTransport.BLUETOOTH_DIRECT }
        assertTrue("Should contain a BLE ESP32 node", bleNode != null)
        
        val loraNode = devices.find { it.radioType == RadioTransport.LORA_MESH }
        assertTrue("Should contain a LoRa mesh node", loraNode != null)
    }

    @Test
    fun testConnectToDeviceRejectsWithoutPreSharedCodeInProduction() = runTest {
        meshService.setDemoSimulation(false)
        val dummyDevice = IoTDevice("1", "Dummy", -50, true, "10m", RadioTransport.BLUETOOTH_DIRECT)
        
        val connected = meshService.connectToDevice(dummyDevice, preSharedCode = "")
        assertFalse("Should reject connection without pre-shared code in production", connected)
        assertEquals(ConnectionState.DISCONNECTED, meshService.connectionState.value)
    }

    @Test
    fun testConnectToDeviceAcceptsWithPreSharedCode() = runTest {
        meshService.setDemoSimulation(false)
        val dummyDevice = IoTDevice("1", "Dummy", -50, true, "10m", RadioTransport.BLUETOOTH_DIRECT)
        
        val connected = meshService.connectToDevice(dummyDevice, preSharedCode = "SECRET123")
        assertTrue("Should accept connection with valid code", connected)
        assertEquals(ConnectionState.CONNECTED, meshService.connectionState.value)
        assertEquals(dummyDevice, meshService.connectedDevice.value)
        
        meshService.disconnect()
        assertEquals(ConnectionState.DISCONNECTED, meshService.connectionState.value)
    }

    @Test
    fun testResolveActiveTransportHybridMode() = runTest {
        meshService.setDemoSimulation(true)
        val dummyBleDevice = IoTDevice("1", "DummyBLE", -50, true, "10m", RadioTransport.BLUETOOTH_DIRECT)
        
        meshService.connectToDevice(dummyBleDevice)
        
        // Nearby -> resolves to BLE Direct
        val transportBLE = meshService.resolveActiveTransport(RadioTransport.HYBRID_AUTO, true)
        assertEquals(RadioTransport.BLUETOOTH_DIRECT, transportBLE.first)
        
        meshService.disconnect()
        
        val dummyLoRaDevice = IoTDevice("2", "DummyLoRa", -90, false, "3km", RadioTransport.LORA_MESH)
        meshService.connectToDevice(dummyLoRaDevice)
        
        // Far -> resolves to LoRa Mesh
        val transportLoRa = meshService.resolveActiveTransport(RadioTransport.HYBRID_AUTO, false)
        assertEquals(RadioTransport.LORA_MESH, transportLoRa.first)
    }
}
