package com.example.meshchat.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

enum class NodeTransportProtocol {
    HTTP_REST,
    TCP_BINARY
}

data class DiscoveredWifiNode(
    val ip: String,
    val port: Int = 80,
    val boardType: NodeBoardType = NodeBoardType.UNKNOWN,
    val protocol: NodeTransportProtocol = NodeTransportProtocol.HTTP_REST,
    val nodeId: Short = 0,
    val latencyMs: Long = -1,
    val isGateway: Boolean = false,
    val label: String = ""
)

class WifiNodeScanner(private val context: Context? = null) {

    companion object {
        private const val TAG = "WifiNodeScanner"
        const val DEFAULT_GATEWAY_IP = "192.168.4.1"
        const val PROBE_TIMEOUT_MS = 1200
        val FALLBACK_IPS = listOf("192.168.4.1", "192.168.10.1", "192.168.20.1")
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredNodes = MutableStateFlow<List<DiscoveredWifiNode>>(emptyList())
    val discoveredNodes: StateFlow<List<DiscoveredWifiNode>> = _discoveredNodes.asStateFlow()

    private val _currentGatewayNode = MutableStateFlow<DiscoveredWifiNode?>(null)
    val currentGatewayNode: StateFlow<DiscoveredWifiNode?> = _currentGatewayNode.asStateFlow()

    fun getDhcpGatewayIp(ctx: Context? = context): String? {
        if (ctx == null) return null
        try {
            // Android 10+ (API 29+) LinkProperties
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val activeNetwork = cm?.activeNetwork
                if (activeNetwork != null) {
                    val linkProperties: LinkProperties? = cm.getLinkProperties(activeNetwork)
                    val gateway = linkProperties?.routes?.firstOrNull { 
                        it.isDefaultRoute && it.hasGateway() && it.gateway is java.net.Inet4Address 
                    }?.gateway?.hostAddress
                    if (!gateway.isNullOrBlank() && gateway != "0.0.0.0" && !gateway.contains(":")) {
                        Log.d(TAG, "Detected Wi-Fi Gateway from LinkProperties: $gateway")
                        return gateway
                    }
                }
            }

            // Fallback via WifiManager DhcpInfo
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wm?.dhcpInfo
            if (dhcp != null && dhcp.gateway != 0) {
                val ip = (dhcp.gateway and 0xFF).toString() + "." +
                        ((dhcp.gateway shr 8) and 0xFF) + "." +
                        ((dhcp.gateway shr 16) and 0xFF) + "." +
                        ((dhcp.gateway shr 24) and 0xFF)
                if (ip != "0.0.0.0") {
                    Log.d(TAG, "Detected Wi-Fi Gateway from DhcpInfo: $ip")
                    return ip
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read DHCP gateway: ${e.message}")
        }
        return null
    }

    fun scanAllGateways(onFinished: ((List<DiscoveredWifiNode>) -> Unit)? = null) {
        scanJob?.cancel()
        scanJob = scope.launch {
            _isScanning.value = true
            val candidates = mutableListOf<String>()

            val dhcpIp = getDhcpGatewayIp()
            if (!dhcpIp.isNullOrBlank()) {
                candidates.add(dhcpIp)
            }
            FALLBACK_IPS.forEach { ip ->
                if (!candidates.contains(ip)) candidates.add(ip)
            }

            val foundNodes = mutableListOf<DiscoveredWifiNode>()

            for (ip in candidates) {
                // Probe HTTP Port 80 first (Fastest & most common for WebServer bridge)
                val httpNode = probeHttpNode(ip, 80)
                if (httpNode != null) {
                    foundNodes.add(httpNode)
                }

                // Probe TCP Binary Port 8266
                val tcpNode = probeTcpNode(ip, 8266)
                if (tcpNode != null) {
                    foundNodes.add(tcpNode)
                }
            }

            _discoveredNodes.value = foundNodes
            _currentGatewayNode.value = foundNodes.firstOrNull { it.isGateway } ?: foundNodes.firstOrNull()
            _isScanning.value = false

            withContext(Dispatchers.Main) {
                onFinished?.invoke(foundNodes)
            }
        }
    }

    fun probeDefaultGateway(port: Int = 80) {
        scanAllGateways()
    }

    fun probeCustomIp(ip: String, port: Int = 80, onResult: (DiscoveredWifiNode?) -> Unit) {
        scope.launch {
            val node = if (port == 8266) probeTcpNode(ip, 8266) else probeHttpNode(ip, port)
            if (node != null) {
                val list = _discoveredNodes.value.toMutableList()
                if (list.none { it.ip == node.ip && it.port == node.port }) {
                    list.add(node)
                    _discoveredNodes.value = list
                }
            }
            withContext(Dispatchers.Main) {
                onResult(node)
            }
        }
    }

    private suspend fun probeHttpNode(ip: String, port: Int): DiscoveredWifiNode? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            val url = URL("http://$ip:$port/")
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = PROBE_TIMEOUT_MS
                readTimeout = PROBE_TIMEOUT_MS
                useCaches = false
            }
            val responseCode = conn.responseCode
            if (responseCode in 200..399) {
                val latency = System.currentTimeMillis() - start
                val boardType = when {
                    ip.startsWith("192.168.10.") -> NodeBoardType.ESP32
                    ip.startsWith("192.168.20.") -> NodeBoardType.ESP8266_ESP12E
                    else -> NodeBoardType.ESP32
                }
                val isGw = (ip == getDhcpGatewayIp() || ip == DEFAULT_GATEWAY_IP)
                val label = "${boardType.displayName} (HTTP REST Bridge)"
                Log.i(TAG, "Found active ESP HTTP Node @ $ip:$port ($label, ${latency}ms)")
                return@withContext DiscoveredWifiNode(
                    ip = ip,
                    port = port,
                    boardType = boardType,
                    protocol = NodeTransportProtocol.HTTP_REST,
                    latencyMs = latency,
                    isGateway = isGw,
                    label = label
                )
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private suspend fun probeTcpNode(ip: String, port: Int): DiscoveredWifiNode? = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        val startTime = System.currentTimeMillis()
        try {
            socket = Socket()
            socket.tcpNoDelay = true
            socket.soTimeout = PROBE_TIMEOUT_MS
            socket.connect(InetSocketAddress(ip, port), PROBE_TIMEOUT_MS)

            val latency = System.currentTimeMillis() - startTime
            val inStream = socket.getInputStream()
            var boardType = NodeBoardType.UNKNOWN
            var nodeId: Short = 0

            val firstByte = inStream.read()
            if (firstByte != -1 && firstByte.toByte() == SentinelBinaryPacket.MAGIC) {
                val ver = inStream.read()
                val type = inStream.read()
                if (type == BinaryPacketType.AUTH_REQ.raw.toInt()) {
                    val senderHigh = inStream.read()
                    val senderLow = inStream.read()
                    if (senderHigh != -1 && senderLow != -1) {
                        nodeId = ((senderHigh shl 8) or (senderLow and 0xFF)).toShort()
                    }
                    boardType = if (ip == DEFAULT_GATEWAY_IP) NodeBoardType.ESP32 else NodeBoardType.ESP8266_ESP12E
                }
            }

            val isGw = (ip == getDhcpGatewayIp() || ip == DEFAULT_GATEWAY_IP)
            val label = "${boardType.displayName} (TCP Binary Socket)"
            Log.i(TAG, "Found active ESP TCP Node @ $ip:$port ($label, ${latency}ms)")
            return@withContext DiscoveredWifiNode(
                ip = ip,
                port = port,
                boardType = boardType,
                protocol = NodeTransportProtocol.TCP_BINARY,
                nodeId = nodeId,
                latencyMs = latency,
                isGateway = isGw,
                label = label
            )
        } catch (e: Exception) {
            null
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }
}
