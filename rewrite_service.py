import re

content = """package com.example.meshchat.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.meshchat.perf.SentinelCryptoConscrypt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.UUID
import kotlin.random.Random
import timber.log.Timber

@SuppressLint("MissingPermission")
class LoRaMeshService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP
    private val APP_NAME = "SentinelMesh"

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<IoTDevice?>(null)
    val connectedDevice: StateFlow<IoTDevice?> = _connectedDevice.asStateFlow()

    private val _incomingMessages = MutableStateFlow<Message?>(null)
    val incomingMessages: StateFlow<Message?> = _incomingMessages.asStateFlow()
    
    private val _scannedDevices = MutableStateFlow<List<IoTDevice>>(emptyList())
    val scannedDevices: StateFlow<List<IoTDevice>> = _scannedDevices.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var pskCode = ""
    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var serverJob: Job? = null
    private var readJob: Job? = null
    private var scanJob: Job? = null
    private var reconnectJob: Job? = null

    init {
        startServer()
    }

    fun startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Timber.e("Bluetooth is not enabled or not supported.")
            _connectionState.value = ConnectionState.DISCONNECTED
            return
        }
        _connectionState.value = ConnectionState.SCANNING
        _scannedDevices.value = emptyList()

        scanJob?.cancel()
        scanJob = serviceScope.launch {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BluetoothDevice.ACTION_FOUND) {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            val newDevice = IoTDevice(
                                id = it.address,
                                name = it.name ?: "Unknown Device",
                                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt(),
                                isBleNearby = true,
                                distanceEstimate = "Direct BT",
                                radioType = RadioTransport.BLUETOOTH_DIRECT
                            )
                            val current = _scannedDevices.value.toMutableList()
                            if (current.none { d -> d.id == newDevice.id }) {
                                current.add(newDevice)
                                _scannedDevices.value = current
                            }
                        }
                    }
                }
            }
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
            delay(12000)
            bluetoothAdapter.cancelDiscovery()
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            if (_connectionState.value == ConnectionState.SCANNING) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
        }
    }

    suspend fun connectToDevice(device: IoTDevice, preSharedCode: String = ""): Boolean = withContext(Dispatchers.IO) {
        if (preSharedCode.isBlank()) {
            Timber.e("Connection rejected: PSK is mandatory.")
            return@withContext false
        }
        pskCode = preSharedCode
        _connectedDevice.value = device
        _connectionState.value = ConnectionState.CONNECTING
        
        scanJob?.cancel()
        bluetoothAdapter?.cancelDiscovery()
        val btDevice = bluetoothAdapter?.getRemoteDevice(device.id) ?: return@withContext false
        
        return@withContext attemptConnect(btDevice, pskCode)
    }

    private suspend fun attemptConnect(btDevice: BluetoothDevice, psk: String, retries: Int = 3): Boolean {
        var delayMs = 1000L
        for (i in 1..retries) {
            try {
                val socket = btDevice.createRfcommSocketToServiceRecord(APP_UUID)
                socket.connect()
                if (performHandshake(socket, psk, isClient = true)) {
                    manageConnectedSocket(socket, btDevice)
                    return true
                } else {
                    socket.close()
                    Timber.e("PSK Handshake failed")
                    _connectionState.value = ConnectionState.DISCONNECTED
                    return false
                }
            } catch (e: Exception) {
                Timber.e(e, "Connection attempt \$i failed")
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
            
            // PSK Key Derivation using SHA-512 (simplified HKDF for this scope)
            val derivedKey = SentinelCryptoConscrypt.sha512(psk.toByteArray())
            
            if (isClient) {
                val nonce = ByteArray(16).also { SecureRandom().nextBytes(it) }
                out.write(nonce)
                out.flush()
                val response = ByteArray(64)
                val read = `in`.read(response)
                if (read < 64) return false
                val expected = SentinelCryptoConscrypt.hmac(derivedKey, nonce)
                SentinelCryptoConscrypt.constantTimeEquals(response.sliceArray(0 until 64), expected)
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
            Timber.e(e, "Handshake exception")
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
                    Timber.e(e, "Disconnected during read")
                    break
                }
            }
            disconnect()
        }
    }
    
    private fun processIncomingPayload(payload: ByteArray) {
        // payload format: IV(12 bytes) + CipherText
        if (payload.size < 12) return
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val derivedKey = SentinelCryptoConscrypt.sha512(pskCode.toByteArray()).copyOfRange(0, 32)
        
        try {
            val decrypted = SentinelCryptoConscrypt.aesGcmDecrypt(ciphertext, derivedKey, iv)
            val parts = String(decrypted).split("|")
            if (parts.size >= 3) {
                val msg = Message(
                    channelId = parts[0],
                    text = parts[1],
                    sender = parts[2],
                    mediaBase64 = if (parts.size >= 4 && parts[3].isNotEmpty()) parts[3] else null,
                    isSent = false,
                    transport = RadioTransport.BLUETOOTH_DIRECT,
                    transportDetail = "Real BT Direct"
                )
                _incomingMessages.value = msg
            }
        } catch (e: Exception) {
            Timber.e("Failed to decrypt incoming message. Potential PSK mismatch or corruption.")
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        _connectedDevice.value = null
        try { activeSocket?.close() } catch (e: Exception) {}
        activeSocket = null
        readJob?.cancel()
        reconnectJob?.cancel()
        startServer()
    }

    private fun startServer() {
        serverJob?.cancel()
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return
        serverJob = serviceScope.launch {
            var serverSocket: BluetoothServerSocket? = null
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
                while (true) {
                    val socket = serverSocket?.accept() ?: break
                    Timber.d("Incoming connection accepted.")
                    if (pskCode.isNotBlank() && performHandshake(socket, pskCode, isClient = false)) {
                        manageConnectedSocket(socket, socket.remoteDevice)
                    } else {
                        socket.close()
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Server socket error")
            } finally {
                try { serverSocket?.close() } catch (e: Exception) {}
            }
        }
    }

    fun resolveActiveTransport(requested: RadioTransport, peerNearby: Boolean = true): Pair<RadioTransport, String> {
        return RadioTransport.BLUETOOTH_DIRECT to "Real BT Direct • 2.4 GHz"
    }

    suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED) return@withContext
        val payloadStr = "${message.channelId}|${message.text}|${message.sender}|${message.mediaBase64 ?: ""}"
        
        val derivedKey = SentinelCryptoConscrypt.sha512(pskCode.toByteArray()).copyOfRange(0, 32)
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        
        try {
            val encrypted = SentinelCryptoConscrypt.aesGcmEncrypt(payloadStr.toByteArray(), derivedKey, iv)
            val finalPayload = iv + encrypted
            activeSocket?.outputStream?.write(finalPayload)
            activeSocket?.outputStream?.flush()
            Timber.d("Sent encrypted real message.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            disconnect()
        }
    }
}
"""

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)

