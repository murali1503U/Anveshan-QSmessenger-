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
class LoRaMeshService(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val APP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP
    private val APP_NAME = "SentinelMesh"

    val realBluetoothManager = RealBluetoothManager(bluetoothAdapter, APP_UUID, APP_NAME)
    val loRaHAL: LoRaHAL = SX1278LoRa()
    private val router = HybridRouter(loRaHAL, realBluetoothManager)

    // Expose states
    val connectionState: StateFlow<ConnectionState> = realBluetoothManager.connectionState
    val connectedDevice: StateFlow<IoTDevice?> = realBluetoothManager.connectedDevice

    private val _incomingMessages = MutableStateFlow<Message?>(null)
    val incomingMessages: StateFlow<Message?> = _incomingMessages.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<IoTDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<IoTDevice>> = _discoveredDevices.asStateFlow()

    private val _loRaState = MutableStateFlow(ConnectionState.CONNECTED) // LoRa is always on
    val loRaState: StateFlow<ConnectionState> = _loRaState.asStateFlow()

    private var serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var scanJob: Job? = null
    private var routerJob: Job? = null

    init {
        startRouterListener()
    }

    private fun startRouterListener() {
        routerJob?.cancel()
        routerJob = serviceScope.launch {
            while (true) {
                val msg = router.receive()
                if (msg != null) {
                    _incomingMessages.value = msg
                }
                delay(50) // Prevent tight loop polling
            }
        }
    }

    fun startScanning() {
        if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) {
            Log.e("LoRaMeshService", "Bluetooth is not enabled or not supported.")
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
                                Log.d("BluetoothScan", "Device found: ${newDevice.name} (${newDevice.id})")
                            }
                        }
                    }
                }
            }
            try {
                context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
                Log.d("BluetoothScan", "Scanning started")
                bluetoothAdapter.startDiscovery()
                delay(12000)
                bluetoothAdapter.cancelDiscovery()
                if (realBluetoothManager.connectionState.value == ConnectionState.SCANNING) {
                    realBluetoothManager.setConnectionState(ConnectionState.DISCONNECTED)
                }
            } catch (e: SecurityException) {
                Log.e("LoRaMeshService", "Missing Bluetooth permissions", e)
            } catch (e: Exception) {
                Log.e("LoRaMeshService", "Discovery error", e)
            } finally {
                try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
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
    }

    fun resolveActiveTransport(requested: RadioTransport, peerNearby: Boolean = true): Pair<RadioTransport, String> {
        return if (loRaHAL.isAvailable()) {
            RadioTransport.LORA_MESH to "LoRa Mesh Direct • 868 MHz"
        } else {
            RadioTransport.BLUETOOTH_DIRECT to "Real BT Direct • 2.4 GHz"
        }
    }

    suspend fun sendMessage(message: Message) = withContext(Dispatchers.IO) {
        val priority = if (message.text.contains("SOS", ignoreCase = true)) HybridRouter.PRIORITY_SOS else HybridRouter.PRIORITY_NORMAL
        router.send(message, priority)
    }
}
