package com.example.meshchat

import com.example.meshchat.transport.QsTransport
import com.example.meshchat.transport.TransportManager
import com.example.meshchat.transport.TransportState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransportManagerTest {

    class MockTransport : QsTransport {
        var connectCalls = 0
        var shouldFail = true
        var mockState = TransportState.DISCONNECTED
        var simulateTimeout = false

        override suspend fun connect(): Result<Unit> {
            connectCalls++
            if (simulateTimeout) {
                delay(15000L)
            }
            return if (shouldFail) {
                mockState = TransportState.ERROR
                Result.failure(Exception("Mock failure"))
            } else {
                mockState = TransportState.CONNECTED
                Result.success(Unit)
            }
        }

        override suspend fun disconnect() {
            mockState = TransportState.DISCONNECTED
        }

        override suspend fun send(packet: ByteArray): Result<Unit> {
            if (simulateTimeout) delay(15000L)
            return Result.success(Unit)
        }

        override fun onReceive(callback: (ByteArray) -> Unit) {}

        override fun state(): TransportState = mockState
    }

    @Test
    fun testAutoReconnectWithBackoff() = runTest {
        val transport = MockTransport()
        val manager = TransportManager(transport, this)
        
        manager.start()
        
        // Initial connect attempt
        advanceTimeBy(100)
        assertEquals(1, transport.connectCalls)
        
        // 1st retry after 1s
        advanceTimeBy(1000)
        assertEquals(2, transport.connectCalls)
        
        // 2nd retry after 2s
        advanceTimeBy(2000)
        assertEquals(3, transport.connectCalls)
        
        // 3rd retry after 4s
        advanceTimeBy(4000)
        assertEquals(4, transport.connectCalls)
        
        manager.stop()
    }
    
    @Test
    fun testSendTimeout() = runTest {
        val transport = MockTransport().apply {
            simulateTimeout = true
            shouldFail = false
        }
        val manager = TransportManager(transport, this)
        val result = manager.send(ByteArray(0))
        assertTrue(result.isFailure)
    }
}
