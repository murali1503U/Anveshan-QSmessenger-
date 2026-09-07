package com.example.meshchat.perf

import android.util.Log
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.SentinelPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

object SentinelFlows {
    // Incoming messages (SharedFlow, buffer overflow = DROP_OLDEST)
    private val _incoming = MutableSharedFlow<SentinelPacket>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incoming: SharedFlow<SentinelPacket> = _incoming.asSharedFlow()
    
    // Connection state (StateFlow, always has value)
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }
    
    fun emitIncoming(packet: SentinelPacket) {
        _incoming.tryEmit(packet)
    }

    suspend fun processIncoming(scope: CoroutineScope) {
        incoming
            .buffer(16) // Buffer for batching
            .onEach { packet ->
                withContext(SentinelDispatchers.Default) {
                    handlePacket(packet)
                }
            }
            .catch { e ->
                Log.e("SentinelFlows", "Flow processing error", e)
            }
            .launchIn(scope)
    }
    
    private suspend fun handlePacket(packet: SentinelPacket) {
        withContext(SentinelDispatchers.IO) {
            SentinelState.enqueue(packet)
            // Additional processing can be dispatched to hardware/crypto here
        }
    }
}
