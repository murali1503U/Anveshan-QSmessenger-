package com.example.meshchat.ui

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.DiscoveredWifiNode
import com.example.meshchat.data.IoTDevice
import com.example.meshchat.data.NodeTransportProtocol
import com.example.meshchat.data.RadioTransport
import com.example.meshchat.data.WifiTcpTransport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshPairingSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // Bluetooth States
    val btConnectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedBtDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val discoveredBtDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()

    // Wi-Fi TCP Node States
    val wifiConnectionState by viewModel.wifiConnectionState.collectAsStateWithLifecycle()
    val wifiBoardType by viewModel.wifiNodeBoardType.collectAsStateWithLifecycle()
    val wifiNodeId by viewModel.wifiNodeId.collectAsStateWithLifecycle()
    val wifiPingLatency by viewModel.wifiPingLatencyMs.collectAsStateWithLifecycle()
    val wifiPacketsSent by viewModel.wifiPacketsSent.collectAsStateWithLifecycle()
    val wifiPacketsReceived by viewModel.wifiPacketsReceived.collectAsStateWithLifecycle()
    val wifiLastError by viewModel.wifiLastError.collectAsStateWithLifecycle()

    // Wi-Fi HTTP REST Node States
    val httpConnectionState by viewModel.httpConnectionState.collectAsStateWithLifecycle()
    val httpBoardType by viewModel.httpNodeBoardType.collectAsStateWithLifecycle()
    val httpNodeIp by viewModel.httpNodeIp.collectAsStateWithLifecycle()
    val httpNodePort by viewModel.httpNodePort.collectAsStateWithLifecycle()
    val httpPingLatency by viewModel.httpPingLatencyMs.collectAsStateWithLifecycle()
    val httpPacketsSent by viewModel.httpPacketsSent.collectAsStateWithLifecycle()
    val httpPacketsReceived by viewModel.httpPacketsReceived.collectAsStateWithLifecycle()
    val httpLastError by viewModel.httpLastError.collectAsStateWithLifecycle()

    val discoveredWifiNodes by viewModel.discoveredWifiNodes.collectAsStateWithLifecycle()
    val isWifiScanning by viewModel.isWifiScanning.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) } // 0 = Wi-Fi Node, 1 = Bluetooth
    
    var nodeIpInput by remember { mutableStateOf(WifiTcpTransport.DEFAULT_NODE_IP) }
    var nodePortInput by remember { mutableStateOf("80") }
    var nodePskInput by remember { mutableStateOf("12345678") }
    
    var deviceToPair by remember { mutableStateOf<IoTDevice?>(null) }
    var preSharedCodeInput by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (selectedTabIndex == 0) Icons.Default.Wifi else Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Hardware Transceiver Pairing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { 
                    if (selectedTabIndex == 0) viewModel.scanWifiNodes() else viewModel.startScanning() 
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                }
            }

            Text(
                "Connect to ESP32, ESP-12E (ESP8266), or Arduino LoRa bridge transceivers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Transport Selector Tabs
            PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Wi-Fi ESP LoRa Bridge") },
                    icon = { Icon(Icons.Default.Wifi, contentDescription = null) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Bluetooth (Meshtastic)") },
                    icon = { Icon(Icons.Default.Bluetooth, contentDescription = null) }
                )
            }

            Spacer(Modifier.height(16.dp))

            if (selectedTabIndex == 0) {
                // ==================== WI-FI NODE SECTION ====================
                
                // Active HTTP REST Connection Card
                if (httpConnectionState == ConnectionState.CONNECTED) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "${httpBoardType.displayName} (HTTP REST Bridge)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Gateway: $httpNodeIp:$httpNodePort • Polling /message",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.disconnectHttpNode() },
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            HorizontalDivider(Modifier.padding(vertical = 10.dp))

                            // Telemetry Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (httpPingLatency >= 0) "$httpPingLatency ms RTT" else "Ping...",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Text(
                                    "Tx: $httpPacketsSent | Rx: $httpPacketsReceived pkts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                                TextButton(
                                    onClick = { viewModel.sendWifiPing() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Ping Now", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else if (wifiConnectionState == ConnectionState.CONNECTED) {
                    // Active TCP Binary Connection Card
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "${wifiBoardType.displayName} (TCP Socket)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            "Node ID: 0x${wifiNodeId.toString(16).uppercase().padStart(4, '0')} • $nodeIpInput:$nodePortInput",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { viewModel.disconnectWifiNode() },
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                ) {
                                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                                }
                            }

                            HorizontalDivider(Modifier.padding(vertical = 10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        if (wifiPingLatency >= 0) "$wifiPingLatency ms RTT" else "Ping...",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Text(
                                    "Tx: $wifiPacketsSent | Rx: $wifiPacketsReceived pkts",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                                TextButton(
                                    onClick = { viewModel.sendWifiPing() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                ) {
                                    Text("Ping Now", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    // Auto-Detect & Gateway Connect Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "AUTO-DETECT WI-FI GATEWAY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isWifiScanning) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                }
                            }
                            Text(
                                "Connect phone to ESP Wi-Fi ('ESP32_LORA', 'ESP8266_LORA', or 'SENTINEL_ESP32').",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )
                            Button(
                                onClick = {
                                    viewModel.scanWifiNodes()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isWifiScanning
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Auto-Scan & Connect to Gateway")
                            }
                        }
                    }

                    // Quick Preset Buttons Row
                    Text(
                        "Quick Presets",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = nodeIpInput == "192.168.4.1" && nodePortInput == "80",
                            onClick = {
                                nodeIpInput = "192.168.4.1"
                                nodePortInput = "80"
                                viewModel.connectHttpNode("192.168.4.1", 80)
                            },
                            label = { Text("192.168.4.1:80", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = nodeIpInput == "192.168.10.1",
                            onClick = {
                                nodeIpInput = "192.168.10.1"
                                nodePortInput = "80"
                                viewModel.connectHttpNode("192.168.10.1", 80)
                            },
                            label = { Text("ESP32 (10.1:80)", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = nodeIpInput == "192.168.20.1",
                            onClick = {
                                nodeIpInput = "192.168.20.1"
                                nodePortInput = "80"
                                viewModel.connectHttpNode("192.168.20.1", 80)
                            },
                            label = { Text("ESP8266 (20.1:80)", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    // Discovered Nodes List
                    if (discoveredWifiNodes.isNotEmpty()) {
                        Text(
                            "Discovered Nodes (${discoveredWifiNodes.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp).padding(bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(discoveredWifiNodes) { node ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        viewModel.autoConnectDetectedNode(node)
                                    },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(node.label.ifBlank { "${node.ip}:${node.port}" }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "${node.ip}:${node.port} • ${if (node.latencyMs >= 0) "${node.latencyMs}ms RTT" else "Online"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        FilledTonalButton(onClick = { viewModel.autoConnectDetectedNode(node) }) {
                                            Text("Connect")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Custom IP / Port / PSK Manual Connect
                    OutlinedTextField(
                        value = nodeIpInput,
                        onValueChange = { nodeIpInput = it },
                        label = { Text("Node IP Address") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Router, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = nodePortInput,
                            onValueChange = { nodePortInput = it },
                            label = { Text("Port (80 / 8266)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val port = nodePortInput.toIntOrNull() ?: 80
                                if (port == 8266) {
                                    viewModel.connectWifiNode(nodeIpInput.trim(), port, nodePskInput.trim())
                                } else {
                                    viewModel.connectHttpNode(nodeIpInput.trim(), port)
                                }
                            },
                            modifier = Modifier.weight(1f).align(Alignment.CenterVertically)
                        ) {
                            Text("Connect")
                        }
                    }

                    if (httpLastError != null || wifiLastError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    httpLastError ?: wifiLastError ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

            } else {
                // ==================== BLUETOOTH CLASSIC / BLE SECTION ====================
                if (btConnectionState == ConnectionState.CONNECTED && connectedBtDevice != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        connectedBtDevice?.name ?: "Connected Node",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "MAC: ${connectedBtDevice?.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            FilledTonalButton(onClick = { viewModel.disconnect() }) {
                                Text("Disconnect")
                            }
                        }
                    }
                }

                Text(
                    "Nearby Meshtastic Bluetooth Devices",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (discoveredBtDevices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SensorsOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No Bluetooth devices found. Tap Rescan above.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(discoveredBtDevices) { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { deviceToPair = device },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(device.name, fontWeight = FontWeight.SemiBold)
                                        Text(device.id, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }

            // Bluetooth PSK Dialog
            if (deviceToPair != null) {
                val targetDevice = deviceToPair!!
                AlertDialog(
                    onDismissRequest = { deviceToPair = null },
                    icon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    title = { Text("Pre-Shared Code Required", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Enter hardware pre-shared key for ${targetDevice.name}:")
                            OutlinedTextField(
                                value = preSharedCodeInput,
                                onValueChange = { preSharedCodeInput = it },
                                label = { Text("Pre-Shared Key *") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (preSharedCodeInput.isNotBlank()) {
                                    viewModel.connectToDevice(targetDevice, preSharedCodeInput.trim()) { success ->
                                        if (!success) {
                                            android.widget.Toast.makeText(context, "Connection failed. Verify PSK.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    deviceToPair = null
                                }
                            },
                            enabled = preSharedCodeInput.isNotBlank()
                        ) {
                            Text("Connect")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deviceToPair = null }) { Text("Cancel") }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
