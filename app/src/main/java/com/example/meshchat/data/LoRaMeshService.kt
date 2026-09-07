package com.example.meshchat.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@SuppressLint("MissingPermission")
class LoRaMeshService(private val context: Context? = null) {

    private val bluetoothAdapter: BluetoothAdapter? = context?.getSystemService(BluetoothManager::class.java)?.adapter
    private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP
    private val APP_NAME = "SentinelMesh"

    val realBluetoothManager = RealBluetoothManager(bluetoothAdapter, APP_UUID, APP_NAME)
    val wifiTransport = WifiTcpTransport()
    val httpTransport = HttpNodeTransport()
    val wifiScanner = WifiNodeScanner(context)
    val loRaHAL: LoRaHAL = SX1278LoRa()
    val router = HybridRouter(loRaHAL, realBluetoothManager, wifiTransport, httpTransport)

    private val _discoveredDevices = MutableStateFlow<List<IoTDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<IoTDevice>> = _discoveredDevices.asStateFlow()

    // Bluetooth States
    val connectionState: StateFlow<ConnectionState> = realBluetoothManager.connectionState
    val connectedDevice: StateFlow<IoTDevice?> = realBluetoothManager.connectedDevice

    // Wi-Fi TCP Node States
    val wifiConnectionState: StateFlow<ConnectionState> = wifiTransport.connectionState
    val wifiNodeBoardType: StateFlow<NodeBoardType> = wifiTransport.nodeBoardType
    val wifiNodeId: StateFlow<Short> = wifiTransport.nodeId
    val wifiPingLatencyMs: StateFlow<Long> = wifiTransport.pingLatencyMs
    val wifiPacketsSent: StateFlow<Long> = wifiTransport.packetsSent
    val wifiPacketsReceived: StateFlow<Long> = wifiTransport.packetsReceived
    val wifiLastError: StateFlow<String?> = wifiTransport.lastError

    // Wi-Fi HTTP REST Node States
    val httpConnectionState: StateFlow<ConnectionState> = httpTransport.connectionState
    val httpNodeBoardType: StateFlow<NodeBoardType> = httpTransport.nodeBoardType
    val httpNodeIp: StateFlow<String> = httpTransport.nodeIp
    val httpNodePort: StateFlow<Int> = httpTransport.nodePort
    val httpPingLatencyMs: StateFlow<Long> = httpTransport.pingLatencyMs
    val httpPacketsSent: StateFlow<Long> = httpTransport.packetsSent
    val httpPacketsReceived: StateFlow<Long> = httpTransport.packetsReceived
    val httpLastError: StateFlow<String?> = httpTransport.lastError

    private val _incomingMessages = MutableStateFlow<Message?>(null)
    val incomingMessages: StateFlow<Message?> = _incomingMessages.asStateFlow()

    private val _loRaState = MutableStateFlow(ConnectionState.CONNECTED) // LoRa HAL readiness
    val loRaState: StateFlow<ConnectionState> = _loRaState.asStateFlow()

    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var scanJob: Job? = null
    private var routerJob: Job? = null
    private var wifiPacketJob: Job? = null
    private var httpPacketJob: Job? = null

    init {
        startRouterListener()
        startWifiPacketListener()
        startHttpPacketListener()
    }

    private fun startRouterListener() {
        routerJob?.cancel()
        routerJob = serviceScope.launch {
            while (true) {
                val msg = router.receive()
                if (msg != null) {
                    _incomingMessages.value = msg
                }
                delay(50)
            }
        }
    }

    private fun startWifiPacketListener() {
        wifiPacketJob?.cancel()
        wifiPacketJob = serviceScope.launch {
            wifiTransport.receivedPackets.collect { packet ->
                if (packet.type == BinaryPacketType.MESSAGE || packet.type == BinaryPacketType.SOS) {
                    val parsedMsg = router.parsePayload(packet.payload, RadioTransport.WIFI_TCP_NODE)
                    if (parsedMsg != null) {
                        _incomingMessages.value = parsedMsg
                    }
                }
            }
        }
    }

    private fun startHttpPacketListener() {
        httpPacketJob?.cancel()
        httpPacketJob = serviceScope.launch {
            httpTransport.receivedMessages.collect { rawPayload ->
                val parsedMsg = router.parsePayload(rawPayload.toByteArray(Charsets.UTF_8), RadioTransport.WIFI_TCP_NODE)
                if (parsedMsg != null) {
                    _incomingMessages.value = parsedMsg
                } else {
                    // Raw text message from ESP web terminal
                    _incomingMessages.value = Message(
                        channelId = "global_broadcast",
                        text = rawPayload,
                        sender = "ESP_LoRa_Node",
                        isSent = false,
                        transport = RadioTransport.WIFI_TCP_NODE,
                        transportDetail = "ESP LoRa Bridge • HTTP REST"
                    )
                }
            }
        }
    }

    fun connectHttpNode(ip: String = HttpNodeTransport.DEFAULT_NODE_IP, port: Int = HttpNodeTransport.DEFAULT_NODE_PORT, boardType: NodeBoardType = NodeBoardType.UNKNOWN) {
        httpTransport.connect(ip, port, boardType)
    }

    fun disconnectHttpNode() {
        httpTransport.disconnect(isIntentional = true)
    }

    fun connectWifiNode(host: String = WifiTcpTransport.DEFAULT_NODE_IP, port: Int = WifiTcpTransport.DEFAULT_NODE_PORT, psk: String = "12345678") {
        wifiTransport.connect(host, port, psk)
    }

    fun disconnectWifiNode() {
        wifiTransport.disconnect(isIntentional = true)
    }

    fun autoConnectDetectedNode(node: DiscoveredWifiNode) {
        if (node.protocol == NodeTransportProtocol.HTTP_REST) {
            connectHttpNode(node.ip, node.port, node.boardType)
        } else {
            connectWifiNode(node.ip, node.port, "12345678")
        }
    }

    fun sendWifiPing(): Boolean {
        if (httpTransport.connectionState.value == ConnectionState.CONNECTED) {
            return httpTransport.sendPing()
        }
        return wifiTransport.sendPing()
    }

    fun startScanning() {
        // Probe Wi-Fi Node Gateway simultaneously with Bluetooth discovery
        wifiScanner.scanAllGateways { discovered ->
            val firstNode = discovered.firstOrNull()
            if (firstNode != null &&
                httpTransport.connectionState.value != ConnectionState.CONNECTED &&
                wifiTransport.connectionState.value != ConnectionState.CONNECTED
            ) {
                Log.i("LoRaMeshService", "Auto-connecting to detected gateway: ${firstNode.ip}:${firstNode.port}")
                autoConnectDetectedNode(firstNode)
            }
        }

        if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) {
            Log.e("LoRaMeshService", "Bluetooth is not enabled.")
            return
        }
        _discoveredDevices.value = emptyList()
        realBluetoothManager.setConnectionState(ConnectionState.SCANNING)

        scanJob?.cancel()
        scanJob = serviceScope.launch {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == BluetoothDevice.ACTION_FOUND) {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            val newDevice = IoTDevice(
                                id = it.address,
                                name = try { it.name ?: "Unknown Device" } catch (e: SecurityException) { "Unknown Device" },
                                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt(),
                                isBleNearby = true,
                                distanceEstimate = "Direct BT",
                                radioType = RadioTransport.BLUETOOTH_DIRECT
                            )
                            val current = _discoveredDevices.value.toMutableList()
                            if (current.none { d -> d.id == newDevice.id }) {
                                current.add(newDevice)
                                _discoveredDevices.value = current
                            }
                        }
                    }
                }
            }
            try {
                context?.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
                bluetoothAdapter.startDiscovery()
                delay(12000)
                bluetoothAdapter.cancelDiscovery()
                if (realBluetoothManager.connectionState.value == ConnectionState.SCANNING) {
                    realBluetoothManager.setConnectionState(ConnectionState.DISCONNECTED)
                }
            } catch (e: Exception) {
                Log.e("LoRaMeshService", "Discovery error", e)
            } finally {
                try { context?.unregisterReceiver(receiver) } catch (_: Exception) {}
            }
        }
    }

    suspend fun connectToDevice(device: IoTDevice, preSharedCode: String = ""): Boolean = withContext(Dispatchers.IO) {
        if (preSharedCode.isBlank()) {
            Log.e("LoRaMeshService", "Connection rejected: PSK is mandatory.")
            return@withContext false
        }
        realBluetoothManager.pskCode = preSharedCode
        scanJob?.cancel()
        val btDevice = try {
            bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.getRemoteDevice(device.id)
        } catch (e: SecurityException) {
            Log.e("LoRaMeshService", "Missing Bluetooth permission for connect")
            null
        } ?: return@withContext false
        
        return@withContext realBluetoothManager.attemptConnect(btDevice, preSharedCode)
    }

    fun disconnect() {
        realBluetoothManager.disconnect()
        wifiTransport.disconnect(isIntentional = true)
        httpTransport.disconnect(isIntentional = true)
    }

    fun resolveActiveTransport(requested: RadioTransport, peerNearby: Boolean = true): Pair<RadioTransport, String> {
        return when {
            httpTransport.connectionState.value == ConnectionState.CONNECTED -> {
                RadioTransport.WIFI_TCP_NODE to "ESP HTTP REST Bridge • ${httpTransport.nodeBoardType.value.displayName} (${httpTransport.nodeIp.value})"
            }
            wifiTransport.connectionState.value == ConnectionState.CONNECTED -> {
                RadioTransport.WIFI_TCP_NODE to "ESP TCP Node • ${wifiTransport.nodeBoardType.value.displayName}"
            }
            loRaHAL.isAvailable() -> {
                RadioTransport.LORA_MESH to "LoRa Mesh Direct • 433/868 MHz"
            }
            else -> {
                RadioTransport.BLUETOOTH_DIRECT to "Real BT Direct • 2.4 GHz"
            }
        }
    }

    suspend fun sendMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        val priority = if (message.text.contains("SOS", ignoreCase = true)) HybridRouter.PRIORITY_SOS else HybridRouter.PRIORITY_NORMAL
        router.send(message, priority)
    }
}
