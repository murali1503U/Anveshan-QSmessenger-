package com.example.meshchat.transport

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

@SuppressLint("MissingPermission")
class BluetoothTransport(private val deviceAddress: String) : QsTransport {
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val currentState = AtomicReference(TransportState.DISCONNECTED)
    private var socket: BluetoothSocket? = null
    private var receiveCallback: ((ByteArray) -> Unit)? = null

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        currentState.set(TransportState.CONNECTING)
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: throw IOException("No Bluetooth")
            val device = adapter.getRemoteDevice(deviceAddress)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket?.connect()
            
            // PSK challenge-response
            val pskChallenge = "CHALLENGE".toByteArray()
            socket?.outputStream?.write(pskChallenge)
            
            val response = ByteArray(1024)
            val bytes = socket?.inputStream?.read(response) ?: -1
            if (bytes == -1) throw IOException("Challenge failed")
            
            currentState.set(TransportState.CONNECTED)
            startListening()
            Result.success(Unit)
        } catch (e: Exception) {
            currentState.set(TransportState.ERROR)
            Result.failure(e)
        }
    }

    private fun startListening() {
        thread {
            try {
                val input = socket?.inputStream
                val buffer = ByteArray(1024)
                var bytes: Int = 0
                while (currentState.get() == TransportState.CONNECTED && input?.read(buffer).also { bytes = it ?: -1 } != -1) {
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
            socket?.outputStream?.write(packet)
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
