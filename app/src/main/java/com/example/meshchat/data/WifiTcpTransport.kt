package com.example.meshchat.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class WifiTcpTransport {

    companion object {
        private const val TAG = "WifiTcpTransport"
        const val DEFAULT_NODE_IP = "192.168.4.1"
        const val DEFAULT_NODE_PORT = 8266
        const val CONNECT_TIMEOUT_MS = 3000
        const val READ_TIMEOUT_MS = 15000
        const val PING_INTERVAL_MS = 10000L
        const val ANDROID_NODE_ID: Short = 0x00AA.toShort()
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var socket: Socket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var connectionJob: Job? = null
    private var readerJob: Job? = null
    private var pingJob: Job? = null

    private var sequenceCounter: Byte = 0

    // Observables
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _nodeBoardType = MutableStateFlow(NodeBoardType.UNKNOWN)
    val nodeBoardType: StateFlow<NodeBoardType> = _nodeBoardType.asStateFlow()

    private val _nodeId = MutableStateFlow<Short>(0)
    val nodeId: StateFlow<Short> = _nodeId.asStateFlow()

    private val _pingLatencyMs = MutableStateFlow(-1L)
    val pingLatencyMs: StateFlow<Long> = _pingLatencyMs.asStateFlow()

    private val _packetsSent = MutableStateFlow(0L)
    val packetsSent: StateFlow<Long> = _packetsSent.asStateFlow()

    private val _packetsReceived = MutableStateFlow(0L)
    val packetsReceived: StateFlow<Long> = _packetsReceived.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _receivedPackets = MutableSharedFlow<SentinelBinaryPacket>(extraBufferCapacity = 64)
    val receivedPackets: SharedFlow<SentinelBinaryPacket> = _receivedPackets.asSharedFlow()

    private var lastPingTimestamp: Long = 0L
    private var currentHost = DEFAULT_NODE_IP
    private var currentPort = DEFAULT_NODE_PORT
    private var currentPsk = ""
    private var shouldAutoReconnect = false

    fun connect(host: String = DEFAULT_NODE_IP, port: Int = DEFAULT_NODE_PORT, psk: String = "12345678") {
        disconnect(isIntentional = false)
        currentHost = host
        currentPort = port
        currentPsk = psk
        shouldAutoReconnect = true
        _lastError.value = null

        connectionJob?.cancel()
        connectionJob = scope.launch {
            attemptConnectionWithBackoff()
        }
    }

    fun disconnect(isIntentional: Boolean = true) {
        if (isIntentional) {
            shouldAutoReconnect = false
        }
        pingJob?.cancel()
        readerJob?.cancel()
        connectionJob?.cancel()

        try {
            socket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing socket: ${e.message}")
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            _connectionState.value = ConnectionState.DISCONNECTED
            _pingLatencyMs.value = -1L
        }
    }

    private suspend fun attemptConnectionWithBackoff() {
        var backoffMs = 1000L
        while (shouldAutoReconnect && _connectionState.value != ConnectionState.CONNECTED) {
            try {
                _connectionState.value = ConnectionState.CONNECTING
                Log.d(TAG, "Connecting to $currentHost:$currentPort...")

                val sock = Socket()
                sock.tcpNoDelay = true
                sock.keepAlive = true
                sock.connect(InetSocketAddress(currentHost, currentPort), CONNECT_TIMEOUT_MS)

                socket = sock
                inputStream = BufferedInputStream(sock.getInputStream())
                outputStream = BufferedOutputStream(sock.getOutputStream())

                // Authenticate with Node using Challenge-Response PSK
                val authSuccess = performPskChallengeResponse(currentPsk)
                if (authSuccess) {
                    _connectionState.value = ConnectionState.CONNECTED
                    _lastError.value = null
                    Log.i(TAG, "Authenticated successfully with node $currentHost:$currentPort")

                    // Start Reader and Keepalive Ping Loops
                    startReaderLoop()
                    startKeepalivePingLoop()
                    break
                } else {
                    _lastError.value = "Authentication failed (Invalid PSK or Challenge mismatch)"
                    Log.e(TAG, "Authentication failed with node $currentHost:$currentPort")
                    disconnect(isIntentional = true)
                    break
                }
            } catch (e: Exception) {
                Log.w(TAG, "Connection attempt failed: ${e.message}")
                _lastError.value = e.message ?: "Connection refused / timed out"
                _connectionState.value = ConnectionState.DISCONNECTED

                if (shouldAutoReconnect) {
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(10000L)
                }
            }
        }
    }

    private suspend fun performPskChallengeResponse(psk: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val inStream = inputStream ?: return@withContext false
            val outStream = outputStream ?: return@withContext false

            // Node sends AUTH_REQ (0x10) with 16-byte random challenge
            val reqPacket = readSinglePacket(inStream) ?: return@withContext false
            if (reqPacket.type != BinaryPacketType.AUTH_REQ || reqPacket.payload.size < 16) {
                Log.e(TAG, "Expected AUTH_REQ with 16 bytes challenge, got ${reqPacket.type}")
                return@withContext false
            }

            val challenge = reqPacket.payload.sliceArray(0 until 16)
            val nodeIdShort = reqPacket.senderId
            _nodeId.value = nodeIdShort

            // Compute HMAC-SHA256(pskBytes, challenge + nodeId + ANDROID_NODE_ID)
            val hmacData = ByteBuffer.allocate(16 + 2 + 2)
                .order(ByteOrder.BIG_ENDIAN)
                .put(challenge)
                .putShort(nodeIdShort)
                .putShort(ANDROID_NODE_ID)
                .array()

            val hmacResult = computeHmacSha256(psk.toByteArray(Charsets.UTF_8), hmacData)

            // Send AUTH_RESP (0x11)
            val authResp = SentinelBinaryPacket(
                type = BinaryPacketType.AUTH_RESP,
                senderId = ANDROID_NODE_ID,
                receiverId = nodeIdShort,
                sequence = ++sequenceCounter,
                payload = hmacResult
            )
            outStream.write(authResp.encode())
            outStream.flush()
            _packetsSent.value++

            // Wait for AUTH_OK (0x12) or AUTH_FAIL (0x13)
            val resultPacket = readSinglePacket(inStream) ?: return@withContext false
            if (resultPacket.type == BinaryPacketType.AUTH_OK) {
                if (resultPacket.payload.isNotEmpty()) {
                    _nodeBoardType.value = NodeBoardType.fromByte(resultPacket.payload[0])
                }
                return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error in challenge-response handshake", e)
            return@withContext false
        }
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        readerJob = scope.launch(Dispatchers.IO) {
            val inStream = inputStream ?: return@launch
            try {
                while (isActive && socket?.isConnected == true) {
                    val packet = readSinglePacket(inStream)
                    if (packet != null) {
                        _packetsReceived.value++
                        handleIncomingPacket(packet)
                    } else {
                        // EOF reached or socket closed
                        break
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Reader loop terminated: ${e.message}")
            } finally {
                if (shouldAutoReconnect) {
                    disconnect(isIntentional = false)
                    attemptConnectionWithBackoff()
                }
            }
        }
    }

    private fun handleIncomingPacket(packet: SentinelBinaryPacket) {
        when (packet.type) {
            BinaryPacketType.ACK, BinaryPacketType.IDENTITY -> {
                if (lastPingTimestamp > 0) {
                    val rtt = System.currentTimeMillis() - lastPingTimestamp
                    _pingLatencyMs.value = rtt
                    lastPingTimestamp = 0
                }
                if (packet.payload.isNotEmpty()) {
                    _nodeBoardType.value = NodeBoardType.fromByte(packet.payload[0])
                }
            }
            BinaryPacketType.PING -> {
                // Respond with ACK
                scope.launch(Dispatchers.IO) {
                    sendPacket(
                        SentinelBinaryPacket(
                            type = BinaryPacketType.ACK,
                            senderId = ANDROID_NODE_ID,
                            receiverId = packet.senderId,
                            sequence = packet.sequence,
                            payload = byteArrayOf(0x00)
                        )
                    )
                }
            }
            BinaryPacketType.MESSAGE, BinaryPacketType.SOS -> {
                _receivedPackets.tryEmit(packet)
            }
            else -> {
                Log.d(TAG, "Received packet type: ${packet.type}")
            }
        }
    }

    private fun startKeepalivePingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                delay(PING_INTERVAL_MS)
                sendPing()
            }
        }
    }

    fun sendPing(): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        lastPingTimestamp = System.currentTimeMillis()
        val pingPacket = SentinelBinaryPacket(
            type = BinaryPacketType.PING,
            senderId = ANDROID_NODE_ID,
            receiverId = _nodeId.value,
            sequence = ++sequenceCounter,
            payload = byteArrayOf(0x01)
        )
        return sendPacket(pingPacket)
    }

    fun sendPacket(packet: SentinelBinaryPacket): Boolean {
        return try {
            val outStream = outputStream ?: return false
            synchronized(outStream) {
                outStream.write(packet.encode())
                outStream.flush()
            }
            _packetsSent.value++
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send packet: ${e.message}")
            false
        }
    }

    fun sendMessagePayload(channelId: String, text: String, senderName: String, isEncrypted: Boolean = false): Boolean {
        val payloadStr = "$channelId|$text|$senderName|"
        val payloadBytes = payloadStr.toByteArray(Charsets.UTF_8)
        val flags: Byte = if (isEncrypted) 0x01 else 0x00

        val packet = SentinelBinaryPacket(
            type = BinaryPacketType.MESSAGE,
            senderId = ANDROID_NODE_ID,
            receiverId = _nodeId.value,
            sequence = ++sequenceCounter,
            flags = flags,
            payload = payloadBytes
        )
        return sendPacket(packet)
    }

    fun readSinglePacket(inStream: InputStream): SentinelBinaryPacket? {
        try {
            // Find Magic Byte 0xA5
            var b = inStream.read()
            while (b != -1 && b.toByte() != SentinelBinaryPacket.MAGIC) {
                b = inStream.read()
            }
            if (b == -1) return null

            val ver = inStream.read()
            if (ver == -1 || ver.toByte() != SentinelBinaryPacket.VERSION) return null

            val headerRest = ByteArray(SentinelBinaryPacket.HEADER_SIZE - 2)
            var read = 0
            while (read < headerRest.size) {
                val r = inStream.read(headerRest, read, headerRest.size - read)
                if (r == -1) return null
                read += r
            }

            val payloadLen = headerRest[7].toInt() and 0xFF
            val fullPacketBytes = ByteArray(SentinelBinaryPacket.HEADER_SIZE + payloadLen + SentinelBinaryPacket.FOOTER_SIZE)
            fullPacketBytes[0] = SentinelBinaryPacket.MAGIC
            fullPacketBytes[1] = SentinelBinaryPacket.VERSION
            System.arraycopy(headerRest, 0, fullPacketBytes, 2, headerRest.size)

            val remainingSize = payloadLen + SentinelBinaryPacket.FOOTER_SIZE
            var remainingRead = 0
            while (remainingRead < remainingSize) {
                val r = inStream.read(fullPacketBytes, SentinelBinaryPacket.HEADER_SIZE + remainingRead, remainingSize - remainingRead)
                if (r == -1) return null
                remainingRead += r
            }

            return SentinelBinaryPacket.decode(fullPacketBytes)
        } catch (e: Exception) {
            return null
        }
    }

    private fun computeHmacSha256(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(key, "HmacSHA256")
        mac.init(secretKey)
        return mac.doFinal(data)
    }
}
