package com.example.meshchat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.meshchat.data.ChannelType
import com.example.meshchat.data.ChatChannel
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.IoTDevice
import com.example.meshchat.data.RadioTransport
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsListScreen(
    viewModel: ChatViewModel,
    onChannelClick: (ChatChannel) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCli: () -> Unit = {},
    onOpenAiHub: () -> Unit = {},
    onOpenPairing: () -> Unit
) {
    val context = LocalContext.current
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val loRaState by viewModel.loRaState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val selectedRadioTransport by viewModel.selectedRadioTransport.collectAsStateWithLifecycle()
    
    val userCallsign = activeSession?.userCallsign ?: "SENTINEL-7A9B"
    val nodeId = activeSession?.nodeId ?: "0x7A9B"

    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showCreateDirectDialog by remember { mutableStateOf(false) }
    var showNodeIdentityDialog by remember { mutableStateOf(false) }
    var showEditCallsignDialog by remember { mutableStateOf(false) }
    
    var selectedFilterTab by remember { mutableStateOf(0) } // 0=All, 1=Direct, 2=Groups
    var searchQuery by remember { mutableStateOf("") }
    var channelToManage by remember { mutableStateOf<ChatChannel?>(null) }

    val filteredChannels = remember(channels, selectedFilterTab, searchQuery) {
        channels.filter { channel ->
            val matchesTab = when (selectedFilterTab) {
                1 -> channel.type == ChannelType.DIRECT
                2 -> channel.type == ChannelType.GROUP
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                channel.name.contains(searchQuery, ignoreCase = true) ||
                channel.description.contains(searchQuery, ignoreCase = true) ||
                channel.members.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Sentinel Mesh", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                androidx.compose.material.icons.Icons.Default.SatelliteAlt,
                                contentDescription = "LoRa Status",
                                tint = when(loRaState) {
                                    ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                                    ConnectionState.SCANNING -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "LoRa PRIORITY • ${if (loRaState == ConnectionState.CONNECTED) "Active" else "Unavailable"}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (connectionState) {
                                            ConnectionState.CONNECTED -> Color(0xFF2196F3) // Blue for BT Fallback
                                            ConnectionState.SCANNING, ConnectionState.CONNECTING -> Color(0xFFFFC107)
                                            else -> Color(0xFF9E9E9E)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (connectionState == ConnectionState.CONNECTED) {
                                    "BT Relay: ${connectedDevice?.name ?: "Node"}"
                                } else if (connectionState == ConnectionState.SCANNING) {
                                    "Scanning BT..."
                                } else {
                                    "BT Relay Offline"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPairing) {
                        Icon(
                            if (connectionState == ConnectionState.CONNECTED) Icons.Default.BluetoothConnected else Icons.Default.BluetoothSearching,
                            contentDescription = "Bluetooth Mesh Pairing",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Tune, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Quick New Direct Chat
                SmallFloatingActionButton(
                    onClick = { showCreateDirectDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("new_direct_fab")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "New Direct Chat")
                }
                
                // New Group FAB (WhatsApp style)
                ExtendedFloatingActionButton(
                    onClick = { showCreateGroupDialog = true },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = "New Group") },
                    text = { Text("New Group", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_group_fab")
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // User Callsign & Node Identity Header Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNodeIdentityDialog = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "MY CALLSIGN: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    userCallsign,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Node $nodeId • ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    
                                ) {
                                    Text(
                                        "DEPLOYED • PSK MANDATORY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Node Callsign", userCallsign))
                                Toast.makeText(context, "Copied callsign: $userCallsign", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy Callsign",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { showNodeIdentityDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "Show Identity QR",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search chats or groups...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Category Tabs: All, Direct, Groups
            TabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = selectedFilterTab == 0,
                    onClick = { selectedFilterTab = 0 },
                    text = { Text("All Chats (${channels.size})", fontWeight = if (selectedFilterTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedFilterTab == 1,
                    onClick = { selectedFilterTab = 1 },
                    text = { Text("Direct", fontWeight = if (selectedFilterTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedFilterTab == 2,
                    onClick = { selectedFilterTab = 2 },
                    text = { Text("Groups", fontWeight = if (selectedFilterTab == 2) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))


            // Channels List
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            if (selectedFilterTab == 2) Icons.Default.Groups else Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No chats match \"$searchQuery\"" else "No channels in this category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Tap the buttons below to create a direct secure chat or group.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    items(filteredChannels, key = { it.channelId }) { channel ->
                        ChannelListItem(
                            channel = channel,
                            onClick = {
                                viewModel.selectChannel(channel.channelId)
                                onChannelClick(channel)
                            },
                            onLongClick = {
                                channelToManage = channel
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 72.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }

    // Radio Transport Selection Sheet

    // Create Group Dialog
    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onDismiss = { showCreateGroupDialog = false },
            onCreate = { name, desc, members, secLevel, transport ->
                viewModel.createNewGroup(name, desc, members, secLevel, transport) { created ->
                    onChannelClick(created)
                }
                showCreateGroupDialog = false
            }
        )
    }

    // Create Direct Chat Dialog
    if (showCreateDirectDialog) {
        CreateDirectChatDialog(
            myCallsign = userCallsign,
            discoveredDevices = discoveredDevices,
            
            onDismiss = { showCreateDirectDialog = false },
            onCreate = { name, secLevel, transport, sharedSecret ->
                viewModel.createNewDirectChat(name, secLevel, transport, sharedSecret) { created ->
                    onChannelClick(created)
                }
                showCreateDirectDialog = false
            }
        )
    }

    // Node Identity Share Dialog
    if (showNodeIdentityDialog) {
        NodeIdentityShareDialog(
            callsign = userCallsign,
            nodeId = nodeId,
            onEditCallsign = {
                showNodeIdentityDialog = false
                showEditCallsignDialog = true
            },
            onDismiss = { showNodeIdentityDialog = false }
        )
    }

    // Edit Callsign Dialog
    if (showEditCallsignDialog) {
        EditCallsignDialog(
            currentCallsign = userCallsign,
            onSave = { newCallsign ->
                viewModel.updateUserCallsign(newCallsign)
                showEditCallsignDialog = false
            },
            onRandomize = {
                viewModel.randomizeUserCallsign()
            },
            onDismiss = { showEditCallsignDialog = false }
        )
    }

    // Manage Channel Bottom Sheet
    if (channelToManage != null) {
        val target = channelToManage!!
        ModalBottomSheet(
            onDismissRequest = { channelToManage = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = target.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (target.type == ChannelType.GROUP) "Group • ${target.members}" else "Direct Secure Chat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                // Pin / Unpin
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.togglePinChannel(target)
                            channelToManage = null
                        }
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (target.isPinned) Icons.Default.PushPin else Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(if (target.isPinned) "Unpin Chat" else "Pin to Top", fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Delete channel (if not broadcast)
                if (target.channelId != "global_broadcast") {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.deleteChannel(target.channelId)
                                channelToManage = null
                            }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(16.dp))
                            Text("Delete Chat & Messages", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChannelListItem(
    channel: ChatChannel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeFormatted = remember(channel.lastMessageTimestamp) {
        dateFormat.format(Date(channel.lastMessageTimestamp))
    }

    val avatarColor = remember(channel.avatarColorSeed) {
        val colors = listOf(
            Color(0xFF00E676),
            Color(0xFF00B0FF),
            Color(0xFFFF9100),
            Color(0xFFE040FB),
            Color(0xFF76FF03),
            Color(0xFFFF5252),
            Color(0xFF64FFDA)
        )
        val index = (Math.abs(channel.avatarColorSeed) % colors.size).toInt()
        colors[index]
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar Circle
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(avatarColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when (channel.type) {
                    ChannelType.BROADCAST -> Icons.Default.Podcasts
                    ChannelType.GROUP -> Icons.Default.Groups
                    ChannelType.DIRECT -> Icons.Default.Person
                },
                contentDescription = null,
                tint = avatarColor,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        // Name and Message Preview
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (channel.isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                if (channel.securityLevel >= 2) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Encrypted",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(4.dp))
                // Transport Pill
                val (tIcon, tColor, tLabel) = when (channel.preferredTransport) {
                    RadioTransport.BLUETOOTH_DIRECT -> Triple(Icons.Default.Bluetooth, Color(0xFF2979FF), "BLE")
                    RadioTransport.LORA_MESH -> Triple(Icons.Default.CellTower, Color(0xFFFF9100), "LoRa")
                    RadioTransport.HYBRID_AUTO -> Triple(Icons.Default.ElectricBolt, Color(0xFF00E676), "Auto")
                }
                Surface(
                    shape = RoundedCornerShape(3.dp),
                    color = tColor.copy(alpha = 0.15f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    ) {
                        Icon(tIcon, contentDescription = null, tint = tColor, modifier = Modifier.size(8.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(tLabel, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = tColor)
                    }
                }
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = channel.lastMessageText.ifBlank { channel.description.ifBlank { "Encrypted channel ready" } },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Time and Unread Badge
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = if (channel.unreadCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            if (channel.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Badge(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("${channel.unreadCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String, members: String, secLevel: Int, transport: RadioTransport) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupMembers by remember { mutableStateOf("") }
    var selectedSecLevel by remember { mutableStateOf(2) } // AES-256 default
    var selectedTransport by remember { mutableStateOf(RadioTransport.HYBRID_AUTO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Mesh Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Subject / Name") },
                    placeholder = { Text("e.g. Field Unit Alpha, Basecamp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = groupMembers,
                    onValueChange = { groupMembers = it },
                    label = { Text("Group Members (IDs / Callsigns)") },
                    placeholder = { Text("e.g. Node-Alpha, Charlie, Pilot-1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    label = { Text("Group Description (Optional)") },
                    placeholder = { Text("Tactical operational relay") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Wireless Radio Path:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        RadioTransport.HYBRID_AUTO to "⚡ Hybrid Auto",
                        RadioTransport.BLUETOOTH_DIRECT to "🔵 BLE (<100m)",
                        RadioTransport.LORA_MESH to "📻 LoRa Mesh"
                    ).forEach { (transport, label) ->
                        FilterChip(
                            selected = selectedTransport == transport,
                            onClick = { selectedTransport = transport },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }

                Text(
                    "Encryption Tier:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        0 to "Auto",
                        1 to "Fast",
                        2 to "Standard",
                        3 to "Enhanced",
                        4 to "Maximum"
                    ).forEach { (level, label) ->
                        FilterChip(
                            selected = selectedSecLevel == level,
                            onClick = { selectedSecLevel = level },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (groupName.isNotBlank()) {
                        onCreate(groupName, groupDescription, groupMembers, selectedSecLevel, selectedTransport)
                    }
                },
                enabled = groupName.isNotBlank()
            ) {
                Text("Create Group")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CreateDirectChatDialog(
    myCallsign: String,
    discoveredDevices: List<IoTDevice>,
        onDismiss: () -> Unit,
    onCreate: (peerName: String, secLevel: Int, transport: RadioTransport, sharedSecret: String) -> Unit
) {
    val context = LocalContext.current
    var peerName by remember { mutableStateOf("") }
    var sharedSecret by remember { mutableStateOf("") }
    var selectedTransport by remember { mutableStateOf(RadioTransport.HYBRID_AUTO) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("New Direct Chat", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // "My Callsign" Quick Share Strip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Your Mesh Callsign", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(myCallsign, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("My Callsign", myCallsign))
                                Toast.makeText(context, "Callsign copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Peer Input Field with Paste action
                OutlinedTextField(
                    value = peerName,
                    onValueChange = { peerName = it },
                    label = { Text("Peer Node Callsign or Name") },
                    placeholder = { Text("e.g. VIPER-4A12, Sarah, ECHO-09") },
                    singleLine = true,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    try {
                                        val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                                            .build()
                                        val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(context, options)
                                        scanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                barcode.rawValue?.let { raw ->
                                                    if (raw.startsWith("mesh://add?")) {
                                                        val uri = android.net.Uri.parse(raw)
                                                        val scannedCallsign = uri.getQueryParameter("callsign")
                                                        val pubkey = uri.getQueryParameter("pubkey")
                                                        if (scannedCallsign != null) {
                                                            peerName = scannedCallsign.trim()
                                                            android.widget.Toast.makeText(context, "Public Key Verified (OOB) via QR Scan!", android.widget.Toast.LENGTH_LONG).show()
                                                        }
                                                    } else {
                                                        peerName = raw.trim()
                                                    }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                android.widget.Toast.makeText(context, "Scan failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Scanner not available on this device.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR Code")
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (text.isNotBlank()) {
                                        peerName = text.trim()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste Callsign")
                            }
                            if (peerName.isNotEmpty()) {
                                IconButton(onClick = { peerName = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick 1-Tap Add: Discovered Radio Nodes
                if (discoveredDevices.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Nearby Radio Nodes Found (${discoveredDevices.size}):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            discoveredDevices.take(3).forEach { device ->
                                AssistChip(
                                    onClick = { peerName = device.name },
                                    label = { Text(device.name, fontSize = 11.sp, maxLines = 1) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }
                    }
                
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Suggested Test Nodes:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("VIPER-07", "ECHO-99", "BASECAMP").forEach { suggestion ->
                                AssistChip(
                                    onClick = { peerName = suggestion },
                                    label = { Text(suggestion, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Shared Secret Field
                val isPresharedCodeRequired = true
                val isPresharedCodeMissing = isPresharedCodeRequired && sharedSecret.isBlank()

                OutlinedTextField(
                    value = sharedSecret,
                    onValueChange = { 
                        sharedSecret = it 
                    },
                    label = { 
                        Text(
                            "Pre-Shared Secret Code (MANDATORY)"
                            
                        ) 
                    },
                    placeholder = { 
                        Text(
                            "e.g. 123456"
                            
                        ) 
                    },
                    singleLine = true,
                    isError = isPresharedCodeMissing,
                    supportingText = {
                        if (isPresharedCodeMissing) {
                            Text("Pre-shared code is mandatory before connection in deployment mode.", color = MaterialTheme.colorScheme.error)
                        } else {
                            Text("Required cryptographic key / PIN", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(
                            Icons.Default.VpnKey, 
                            contentDescription = "Secret Key", 
                            tint = if (sharedSecret.isNotEmpty()) MaterialTheme.colorScheme.primary 
                                   else if (isPresharedCodeMissing) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                Text(
                    "Wireless Radio Transport:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        RadioTransport.HYBRID_AUTO to "⚡ Auto",
                        RadioTransport.BLUETOOTH_DIRECT to "🔵 BLE (<100m)",
                        RadioTransport.LORA_MESH to "📻 LoRa Mesh"
                    ).forEach { (transport, label) ->
                        FilterChip(
                            selected = selectedTransport == transport,
                            onClick = { selectedTransport = transport },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (peerName.isNotBlank() && sharedSecret.isNotBlank()) {
                        val secLevel = if (sharedSecret.isNotBlank()) 2 else 0
                        onCreate(peerName.trim(), secLevel, selectedTransport, sharedSecret.trim())
                    }
                },
                enabled = peerName.isNotBlank() && sharedSecret.isNotBlank()
            ) {
                Text("Start Chat")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NodeIdentityShareDialog(
    callsign: String,
    nodeId: String,
    onEditCallsign: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("My Mesh Node Identity", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Share your unique node callsign with nearby mesh peers to start communicating off-grid.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // Large Display Card with Tactical Styling
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "NODE CALLSIGN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            callsign,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Hardware Node: $nodeId • Verified",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(14.dp))

                        // Actual QR Code
                        val qrBitmap = remember(callsign, nodeId) {
                            try {
                                val size = 512
                                val qrPayload = "mesh://add?callsign=${android.net.Uri.encode(callsign)}&pubkey=${nodeId}_PUBKEY"
                                val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                                    qrPayload, 
                                    com.google.zxing.BarcodeFormat.QR_CODE, 
                                    size, size
                                )
                                val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
                                for (x in 0 until size) {
                                    for (y in 0 until size) {
                                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                                    }
                                }
                                bitmap
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        if (qrBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR Code for $callsign",
                                modifier = Modifier
                                    .size(150.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(100.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("QR Error", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Node Callsign", callsign))
                            Toast.makeText(context, "Copied callsign: $callsign", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Callsign", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onEditCallsign,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun EditCallsignDialog(
    currentCallsign: String,
    onSave: (String) -> Unit,
    onRandomize: () -> String,
    onDismiss: () -> Unit
) {
    var inputCallsign by remember { mutableStateOf(currentCallsign) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Callsign") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter your custom mesh callsign or tap randomize to generate a tactical callsign.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = inputCallsign,
                    onValueChange = { inputCallsign = it.uppercase() },
                    label = { Text("Node Callsign") },
                    placeholder = { Text("e.g. VIPER-07, GHOST-42") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            val newOne = onRandomize()
                            inputCallsign = newOne
                        }
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Randomize")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputCallsign.isNotBlank()) {
                        onSave(inputCallsign.trim())
                    }
                },
                enabled = inputCallsign.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
