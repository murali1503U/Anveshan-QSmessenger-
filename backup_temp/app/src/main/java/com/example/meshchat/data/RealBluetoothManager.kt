package com.example.meshchat.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.meshchat.perf.SentinelCryptoConscrypt
import com.example.meshchat.perf.SentinelSecurity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.UUID

class RealBluetoothManager(
    val bluetoothAdapter: BluetoothAdapter?,
    private val appUuid: UUID,
    private val appName: String
) {
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<IoTDevice?>(null)
    val connectedDevice: StateFlow<IoTDevice?> = _connectedDevice.asStateFlow()

    private val incomingQueue = java.util.concurrent.ConcurrentLinkedQueue<ByteArray>()

    private var activeSocket: BluetoothSocket? = null
    var pskCode = ""
    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var serverJob: Job? = null
    private var readJob: Job? = null

    init {
        startServer()
    }

    suspend fun attemptConnect(btDevice: BluetoothDevice, psk: String, retries: Int = 3): Boolean {
        var delayMs = 1000L
        for (i in 1..retries) {
            try {
                val socket = btDevice.createRfcommSocketToServiceRecord(appUuid)
                socket.connect()
                if (performHandshake(socket, psk, isClient = true)) {
                    manageConnectedSocket(socket, btDevice)
                    return true
                } else {
                    socket.close()
                    Log.e("RealBluetoothManager", "PSK Handshake failed")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    return false
                }
            } catch (e: Exception) {
                Log.e("RealBluetoothManager", "Connection attempt $i failed")
                delay(delayMs)
                delayMs *= 2
            }
        }
        _connectionState.value = ConnectionState.DISCONNECTED
        return false
    }

    private fun performHandshake(socket: BluetoothSocket, psk: String, isClient: Boolean): Boolean {
        return try {
            val out = socket.outputStream
            val `in` = socket.inputStream
            val derivedKey = SentinelCryptoConscrypt.sha512(psk.toByteArray())
            if (isClient) {
                val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
                out.write(nonce)
                out.flush()
                val response = ByteArray(64)
                val read = `in`.read(response)
                if (read < 64) return false
                val expected = SentinelCryptoConscrypt.hmac(derivedKey, nonce)
                SentinelSecurity.constantTimeEquals(response.sliceArray(0 until 64), expected)
            } else {
                val nonce = ByteArray(16)
                val read = `in`.read(nonce)
                if (read < 16) return false
                val response = SentinelCryptoConscrypt.hmac(derivedKey, nonce)
                out.write(response)
                out.flush()
                true
            }
        } catch (e: Exception) {
            Log.e("RealBluetoothManager", "Handshake exception")
            false
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket, btDevice: BluetoothDevice) {
        activeSocket = socket
        _connectionState.value = ConnectionState.CONNECTED
        if (_connectedDevice.value == null) {
            _connectedDevice.value = IoTDevice(
                id = btDevice.address,
                name = btDevice.name ?: "Unknown",
                rssi = -50,
                isBleNearby = true,
                distanceEstimate = "Direct BT",
                radioType = RadioTransport.BLUETOOTH_DIRECT
            )
        }
        readJob?.cancel()
        readJob = serviceScope.launch {
            val buffer = ByteArray(4096)
            val `in` = socket.inputStream
            while (true) {
                try {
                    val bytes = `in`.read(buffer)
                    if (bytes == -1) break
                    val receivedBytes = buffer.copyOfRange(0, bytes)
                    processIncomingPayload(receivedBytes)
                } catch (e: Exception) {
                    Log.e("RealBluetoothManager", "Disconnected during read")
                    break
                }
            }
            disconnect()
        }
    }

    private fun processIncomingPayload(payload: ByteArray) {
        if (payload.size < 12) return
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val derivedKey = SentinelCryptoConscrypt.sha512(pskCode.toByteArray()).copyOfRange(0, 32)
        try {
            val decrypted = SentinelCryptoConscrypt.aesGcmDecrypt(ciphertext, derivedKey, iv)
            incomingQueue.offer(decrypted)
        } catch (e: Exception) {
            Log.e("RealBluetoothManager", "Failed to decrypt incoming message.")
        }
    }

    fun broadcast(payload: ByteArray) {
        if (_connectionState.value != ConnectionState.CONNECTED) return
        val derivedKey = SentinelCryptoConscrypt.sha512(pskCode.toByteArray()).copyOfRange(0, 32)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        try {
            val encrypted = SentinelCryptoConscrypt.aesGcmEncrypt(payload, derivedKey, iv)
            val finalPayload = iv + encrypted
            activeSocket?.outputStream?.write(finalPayload)
            activeSocket?.outputStream?.flush()
            Log.d("RealBluetoothManager", "Sent encrypted BT message.")
        } catch (e: Exception) {
            Log.e("RealBluetoothManager", "Failed to send message")
            disconnect()
        }
    }

    fun receive(): ByteArray? = incomingQueue.poll()

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }
    
    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
        try { activeSocket?.close() } catch (e: Exception) {}
        activeSocket = null
        readJob?.cancel()
        startServer()
    }

    fun startServer() {
        serverJob?.cancel()
        if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) return
        serverJob = serviceScope.launch {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(appName, appUuid)
                while (true) {
                    val socket = serverSocket?.accept() ?: break
                    Log.d("RealBluetoothManager", "Incoming connection accepted.")
                    if (pskCode.isNotBlank() && performHandshake(socket, pskCode, isClient = false)) {
                        manageConnectedSocket(socket, socket.remoteDevice)
                    } else {
                        socket.close()
                    }
                }
            } catch (e: Exception) {
                Log.e("RealBluetoothManager", "Server socket error")
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }
}
