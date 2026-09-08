package com.example.meshchat.transport

import kotlinx.coroutines.*
import kotlin.math.min

enum class TransportState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

interface QsTransport {
    suspend fun connect(): Result<Unit>
    suspend fun disconnect()
    suspend fun send(packet: ByteArray): Result<Unit>
    fun onReceive(callback: (ByteArray) -> Unit)
    fun state(): TransportState
}

class TransportManager(private val transport: QsTransport, private val coroutineScope: CoroutineScope) {
    private var isRunning = false
    private var reconnectJob: Job? = null
    
    fun start() {
        if (isRunning) return
        isRunning = true
        reconnectJob = coroutineScope.launch {
            var backoff = 1000L
            while (isRunning) {
                if (transport.state() != TransportState.CONNECTED && transport.state() != TransportState.CONNECTING) {
                    val result = withTimeoutOrNull(10000L) {
                        transport.connect()
                    }
                    if (result != null && result.isSuccess) {
                        backoff = 1000L // reset on success
                    } else {
                        delay(backoff)
                        backoff = min(backoff * 2, 30000L)
                    }
                } else {
                    delay(1000L) // check periodically
                }
            }
        }
    }
    
    fun stop() {
        isRunning = false
        reconnectJob?.cancel()
        coroutineScope.launch {
            transport.disconnect()
        }
    }
    
    suspend fun send(packet: ByteArray): Result<Unit> {
        return withTimeoutOrNull(10000L) {
            transport.send(packet)
        } ?: Result.failure(Exception("Timeout"))
    }
}
