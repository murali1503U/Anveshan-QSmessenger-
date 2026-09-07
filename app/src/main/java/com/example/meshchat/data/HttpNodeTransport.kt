package com.example.meshchat.data

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HttpNodeTransport {

    companion object {
        private const val TAG = "HttpNodeTransport"
        const val DEFAULT_NODE_IP = "192.168.4.1"
        const val DEFAULT_NODE_PORT = 80
        const val POLL_INTERVAL_MS = 800L
        const val CONNECT_TIMEOUT_MS = 2500
        const val READ_TIMEOUT_MS = 2500
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null
    private var pingJob: Job? = null

    // Observables
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _nodeBoardType = MutableStateFlow(NodeBoardType.UNKNOWN)
    val nodeBoardType: StateFlow<NodeBoardType> = _nodeBoardType.asStateFlow()

    private val _nodeIp = MutableStateFlow(DEFAULT_NODE_IP)
    val nodeIp: StateFlow<String> = _nodeIp.asStateFlow()

    private val _nodePort = MutableStateFlow(DEFAULT_NODE_PORT)
    val nodePort: StateFlow<Int> = _nodePort.asStateFlow()

    private val _pingLatencyMs = MutableStateFlow(-1L)
    val pingLatencyMs: StateFlow<Long> = _pingLatencyMs.asStateFlow()

    private val _packetsSent = MutableStateFlow(0L)
    val packetsSent: StateFlow<Long> = _packetsSent.asStateFlow()

    private val _packetsReceived = MutableStateFlow(0L)
    val packetsReceived: StateFlow<Long> = _packetsReceived.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _receivedMessages = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val receivedMessages: SharedFlow<String> = _receivedMessages.asSharedFlow()

    private var lastReceivedRawMessage = ""
    private var isIntentionalDisconnect = false

    fun connect(ip: String = DEFAULT_NODE_IP, port: Int = DEFAULT_NODE_PORT, boardType: NodeBoardType = NodeBoardType.UNKNOWN) {
        disconnect(isIntentional = false)
        isIntentionalDisconnect = false
        _nodeIp.value = ip
        _nodePort.value = port
        _nodeBoardType.value = boardType
        _lastError.value = null

        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            val initialLatency = probeEndpoint(ip, port)
            if (initialLatency >= 0) {
                _pingLatencyMs.value = initialLatency
                _connectionState.value = ConnectionState.CONNECTED
                // Drain any stale message sitting on the ESP (e.g. old test emojis)
                // so it is never surfaced as a new message after restart.
                lastReceivedRawMessage = fetchLatestMessage(ip, port) ?: ""
                Log.i(TAG, "Connected to ESP HTTP REST Node @ $ip:$port (RTT=${initialLatency}ms) — drained stale: '$lastReceivedRawMessage'")
                startPolling(ip, port)
                startPeriodicPing(ip, port)
            } else {
                _connectionState.value = ConnectionState.DISCONNECTED
                _lastError.value = "Failed to reach ESP HTTP server at $ip:$port"
                Log.w(TAG, "Connection failed to ESP HTTP server at $ip:$port")
            }
        }
    }

    fun disconnect(isIntentional: Boolean = true) {
        if (isIntentional) {
            isIntentionalDisconnect = true
        }
        pollingJob?.cancel()
        pingJob?.cancel()
        _connectionState.value = ConnectionState.DISCONNECTED
        _pingLatencyMs.value = -1L
    }

    fun buildSendUrl(ip: String, port: Int, payload: String): String {
        val encodedPayload = URLEncoder.encode(payload, "UTF-8")
        return "http://$ip:$port/send?msg=$encodedPayload"
    }

    suspend fun send(payload: String): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value != ConnectionState.CONNECTED && !isEndpointReachable()) {
            return@withContext false
        }
        val ip = _nodeIp.value
        val port = _nodePort.value
        var connection: HttpURLConnection? = null
        return@withContext try {
            val urlString = buildSendUrl(ip, port, payload)
            val url = URL(urlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Connection", "close")
            }

            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                _packetsSent.value++
                Log.d(TAG, "Message sent via ESP HTTP Bridge: $payload")
                true
            } else {
                Log.w(TAG, "ESP HTTP Send failed with code: $responseCode")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HTTP message to ESP: ${e.message}")
            _lastError.value = e.message
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun startPolling(ip: String, port: Int) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val message = fetchLatestMessage(ip, port)
                    if (!message.isNullOrBlank() &&
                        message != "No message received yet" &&
                        message != "None" &&
                        message != lastReceivedRawMessage
                    ) {
                        lastReceivedRawMessage = message
                        _packetsReceived.value++
                        _receivedMessages.tryEmit(message)
                        Log.d(TAG, "Received LoRa packet from ESP HTTP Bridge: $message")
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Polling tick error: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun startPeriodicPing(ip: String, port: Int) {
        pingJob?.cancel()
        pingJob = scope.launch(Dispatchers.IO) {
            var failCount = 0
            while (isActive) {
                delay(10000L)
                if (_connectionState.value != ConnectionState.CONNECTED) break
                val latency = probeEndpoint(ip, port)
                if (latency >= 0) {
                    _pingLatencyMs.value = latency
                    failCount = 0
                } else if (!isIntentionalDisconnect) {
                    failCount++
                    Log.w(TAG, "Periodic ping failed ($failCount) to ESP @ $ip:$port — reconnecting...")
                    if (failCount >= 2) {
                        // Reconnect
                        pollingJob?.cancel()
                        _connectionState.value = ConnectionState.DISCONNECTED
                        delay(2000L)
                        connect(ip, port)
                        break // new connect() will spawn fresh ping loop
                    }
                }
            }
        }
    }

    fun sendPing(): Boolean {
        if (_connectionState.value != ConnectionState.CONNECTED) return false
        scope.launch(Dispatchers.IO) {
            val latency = probeEndpoint(_nodeIp.value, _nodePort.value)
            if (latency >= 0) {
                _pingLatencyMs.value = latency
            }
        }
        return true
    }

    private fun fetchLatestMessage(ip: String, port: Int): String? {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("http://$ip:$port/message")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Connection", "close")
            }
            if (connection.responseCode in 200..299) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                    reader.readText().trim()
                }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun probeEndpoint(ip: String, port: Int): Long {
        val start = System.currentTimeMillis()
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("http://$ip:$port/")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                useCaches = false
                setRequestProperty("Connection", "close")
            }
            val code = connection.responseCode
            if (code in 200..399) {
                System.currentTimeMillis() - start
            } else -1L
        } catch (e: Exception) {
            -1L
        } finally {
            connection?.disconnect()
        }
    }

    private fun isEndpointReachable(): Boolean = _pingLatencyMs.value >= 0
}
