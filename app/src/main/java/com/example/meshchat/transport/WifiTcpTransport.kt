package com.example.meshchat.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class WifiTcpTransport : QsTransport {
    private var socket: Socket? = null
    private val currentState = AtomicReference(TransportState.DISCONNECTED)
    private var receiveCallback: ((ByteArray) -> Unit)? = null
    
    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        currentState.set(TransportState.CONNECTING)
        try {
            socket = Socket()
            socket?.connect(InetSocketAddress("192.168.4.1", 8266), 10000)
            currentState.set(TransportState.CONNECTED)
            startListening()
            Result.success(Unit)
        } catch (e: IOException) {
            currentState.set(TransportState.ERROR)
            Result.failure(e)
        } catch (e: SocketTimeoutException) {
            currentState.set(TransportState.ERROR)
            Result.failure(e)
        }
    }
    
    private fun startListening() {
        thread {
            try {
                val input = socket?.getInputStream()
                val buffer = ByteArray(1024)
                var bytes: Int = 0
                while (socket?.isConnected == true && input?.read(buffer).also { bytes = it ?: -1 } != -1) {
                    val data = buffer.copyOf(bytes)
                    receiveCallback?.invoke(data)
                }
            } catch (e: Exception) {
                currentState.set(TransportState.ERROR)
            }
        }
    }
    
    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            socket?.close()
        } catch (e: Exception) {}
        currentState.set(TransportState.DISCONNECTED)
    }
    
    override suspend fun send(packet: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (currentState.get() != TransportState.CONNECTED) {
                return@withContext Result.failure(IOException("Not connected"))
            }
            socket?.getOutputStream()?.write(packet)
            socket?.getOutputStream()?.flush()
            Result.success(Unit)
        } catch (e: Exception) {
            currentState.set(TransportState.ERROR)
            Result.failure(e)
        }
    }
    
    override fun onReceive(callback: (ByteArray) -> Unit) {
        this.receiveCallback = callback
    }
    
    override fun state(): TransportState = currentState.get()
}
