package com.example.meshchat

import org.junit.Test
import org.junit.Assert.*
import com.example.meshchat.data.BleConstants

class MeshNetworkTest {

    @Test
    fun testBleConstants() {
        assertEquals("6E400001-B5A3-F393-E0A9-E50E24DCCA9E", BleConstants.SERVICE_UUID.toString().uppercase())
        assertEquals("6E400003-B5A3-F393-E0A9-E50E24DCCA9E", BleConstants.TX_CHARACTERISTIC_UUID.toString().uppercase())
        assertEquals("6E400002-B5A3-F393-E0A9-E50E24DCCA9E", BleConstants.RX_CHARACTERISTIC_UUID.toString().uppercase())
        assertEquals(128, BleConstants.BLE_MTU_SIZE)
    }
}
