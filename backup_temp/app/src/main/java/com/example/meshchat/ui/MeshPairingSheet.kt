package com.example.meshchat.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.IoTDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshPairingSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    
    var deviceToPair by remember { mutableStateOf<IoTDevice?>(null) }
    var preSharedCodeInput by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.BluetoothSearching,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Radio Transceiver & Pairing",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = { viewModel.startScanning() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                }
            }

            Text(
                "Scan and pair nearby Bluetooth P2P nodes or connect with long-range LoRa hardware transceivers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Operational Security Mode Badge / Notice
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "MANDATORY SECURE PAIRING",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Pre-shared code is mandatory before connecting to hardware nodes.",
                            
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Current Connection Status Card
            if (connectionState == ConnectionState.CONNECTED && connectedDevice != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BluetoothConnected,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    connectedDevice!!.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    "Connected • Signal: ${connectedDevice!!.rssi} dBm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Disconnect")
                        }
                    }
                }
            }

            // Discovered Devices Section
            Text(
                "Discovered Nodes & Hardware (${discoveredDevices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (connectionState == ConnectionState.SCANNING && discoveredDevices.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Scanning Bluetooth & Sub-GHz bands...")
                }
            } else if (discoveredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No nodes found in range.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.startScanning() }) {
                            Text("Start Scanning")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp)) {
                    items(discoveredDevices) { device ->
                        val isConnecting = connectionState == ConnectionState.CONNECTING && connectedDevice?.id == device.id
                        val isConnected = connectionState == ConnectionState.CONNECTED && connectedDevice?.id == device.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
                                    if (connectionState != ConnectionState.CONNECTING && !isConnected) {
                                        deviceToPair = device
                                        preSharedCodeInput = ""
                                        hasAttemptedSubmit = false
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                else if (device.isBleNearby) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (device.isBleNearby) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (device.isBleNearby) Icons.Default.Bluetooth else Icons.Default.CellTower,
                                        contentDescription = null,
                                        tint = if (device.isBleNearby) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(device.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (device.isBleNearby) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                if (device.isBleNearby) "BLE DIRECT" else "LORA MESH",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (device.isBleNearby) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Distance: ${device.distanceEstimate} • Signal: ${device.rssi} dBm",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isConnecting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (isConnected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Connected", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            if (deviceToPair != null) {
                val targetDevice = deviceToPair!!
                AlertDialog(
                    onDismissRequest = { deviceToPair = null },
                    icon = {
                        Icon(
                            Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    title = {
                        Text("Pre-Shared Code Required", fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Mandatory for Deployment",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Text(
                                "Connecting to ${targetDevice.name}. In live deployment mode, a pre-shared cryptographic key (PSK/PIN) is mandatory before establishing connection.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = preSharedCodeInput,
                                onValueChange = { 
                                    preSharedCodeInput = it 
                                    hasAttemptedSubmit = false
                                },
                                label = { Text("Pre-Shared Code *") },
                                placeholder = { Text("e.g. 749201 or alpha-passcode") },
                                singleLine = true,
                                isError = hasAttemptedSubmit && preSharedCodeInput.isBlank(),
                                supportingText = {
                                    if (hasAttemptedSubmit && preSharedCodeInput.isBlank()) {
                                        Text("Pre-shared code is mandatory before connection.", color = MaterialTheme.colorScheme.error)
                                    } else {
                                        Text("Must match transceiver hardware key.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                trailingIcon = {
                                    Icon(Icons.Default.VpnKey, contentDescription = null)
                                },
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
                                } else {
                                    hasAttemptedSubmit = true
                                }
                            },
                            enabled = preSharedCodeInput.isNotBlank()
                        ) {
                            Text("Authorize & Connect")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { deviceToPair = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            LaunchedEffect(connectionState) {
                if (connectionState == ConnectionState.CONNECTED) {
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
