package com.example.meshchat.ui

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.IoTDevice
import com.example.meshchat.data.Message
import com.example.meshchat.data.RadioTransport
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBackToChannels: (() -> Unit)? = null
) {
    val rawMessages by viewModel.messages.collectAsStateWithLifecycle()
    val currentChannelId by viewModel.currentChannelId.collectAsStateWithLifecycle()
    val selectedChannel by viewModel.selectedChannel.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val pendingMediaUri by viewModel.pendingMediaUri.collectAsStateWithLifecycle()
    val selectedSecurityLevel by viewModel.selectedSecurityLevel.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val selectedRadioTransport by viewModel.selectedRadioTransport.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val latestBitChatPacket by viewModel.latestBitChatPacket.collectAsStateWithLifecycle()
    val latestRelaySteps by viewModel.latestRelaySteps.collectAsStateWithLifecycle()

    var showAiHubSheet by remember { mutableStateOf(false) }
    var showSecuritySheet by remember { mutableStateOf(false) }
    var showBitChatInspector by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showRadioTransportSheet by remember { mutableStateOf(false) }
    var showModelChooserDialog by remember { mutableStateOf(false) }
    var showPairingSheet by remember { mutableStateOf(false) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showSafetyNumberDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedMessageForAction by remember { mutableStateOf<Message?>(null) }

    var inputText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()

    // Filter messages for current channel + search query
    val messages = remember(rawMessages, currentChannelId, searchQuery) {
        val channelMsgs = rawMessages.filter { it.channelId == currentChannelId }
        if (searchQuery.isBlank()) {
            channelMsgs
        } else {
            channelMsgs.filter { it.text.contains(searchQuery, ignoreCase = true) || it.sender.contains(searchQuery, ignoreCase = true) }
        }
    }

    // Auto-scroll when new message arrives
    LaunchedEffect(rawMessages.size) {
        if (rawMessages.isNotEmpty()) {
            listState.animateScrollToItem(rawMessages.size - 1)
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.setPendingMedia(uri)
            }
        }
    )

    if (pendingMediaUri != null) {
        AlertDialog(
            onDismissRequest = { viewModel.setPendingMedia(null) },
            title = { Text("Optimize Media") },
            text = { Text("LoRa networks have low bandwidth. Do you want to compress this image for transmission?") },
            confirmButton = {
                Button(onClick = { viewModel.sendMedia(context, downscale = true) }) {
                    Text("Compress & Send (Fast)")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewModel.sendMedia(context, downscale = false) }) {
                    Text("Original (Slow)")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (onBackToChannels != null) {
                        IconButton(onClick = onBackToChannels) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back to chats")
                        }
                    }
                },
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search in this chat...", fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearchQuery() }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        )
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedChannel?.name ?: "Broadcast",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (connectionState == ConnectionState.CONNECTED) MaterialTheme.colorScheme.primary
                                            else if (connectionState == ConnectionState.SCANNING) Color(0xFFFFD600)
                                            else MaterialTheme.colorScheme.outline
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                val subtitle = when (selectedChannel?.type) {
                                    com.example.meshchat.data.ChannelType.GROUP -> "${selectedChannel?.members ?: "Group"}"
                                    com.example.meshchat.data.ChannelType.DIRECT -> "Direct Secure Peer"
                                    else -> if (connectionState == ConnectionState.CONNECTED) {
                                        connectedDevice?.name ?: "Connected Mesh"
                                    } else {
                                        "Offline Broadcast"
                                    }
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        isSearchActive = !isSearchActive
                        if (!isSearchActive) {
                            viewModel.clearSearchQuery()
                        }
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search Messages",
                            tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // BitChat Mesh & Dynamic Security Testbench
                    if (!isSearchActive) {
                        if (selectedChannel?.type == com.example.meshchat.data.ChannelType.DIRECT) {
                            IconButton(onClick = { showSafetyNumberDialog = true }) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = "Verify Remote Peer",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        IconButton(onClick = { showBitChatInspector = true }) {
                            Icon(
                                Icons.Default.Hub,
                                contentDescription = "BitChat Mesh & Dynamic Security",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Pairing Quick Action
                    if (!isSearchActive) {
                        if (connectionState == ConnectionState.CONNECTED) {
                            IconButton(onClick = { viewModel.disconnect() }) {
                                Icon(Icons.Default.BluetoothConnected, contentDescription = "Disconnect", tint = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    showPairingSheet = true
                                    viewModel.startScanning()
                                }
                            ) {
                                Icon(Icons.Default.BluetoothSearching, contentDescription = "Pair Device", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Settings Button (Always accessible)
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Subtle Citizen Privacy Banner
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "AI Guard: ${activeSession?.selectedModelName?.take(22) ?: "Offline Guard"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { showSettingsSheet = true }
                    )
                }
            }

            // Dual Wireless Radio Link Indicator & Mode Bar
            Surface(
                color = when (selectedRadioTransport) {
                    RadioTransport.BLUETOOTH_DIRECT -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    RadioTransport.LORA_MESH -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                    RadioTransport.HYBRID_AUTO -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRadioTransportSheet = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (selectedRadioTransport) {
                                RadioTransport.BLUETOOTH_DIRECT -> Icons.Default.Bluetooth
                                RadioTransport.LORA_MESH -> Icons.Default.CellTower
                                RadioTransport.HYBRID_AUTO -> Icons.Default.ElectricBolt
                            },
                            contentDescription = null,
                            tint = when (selectedRadioTransport) {
                                RadioTransport.BLUETOOTH_DIRECT -> MaterialTheme.colorScheme.primary
                                RadioTransport.LORA_MESH -> MaterialTheme.colorScheme.secondary
                                RadioTransport.HYBRID_AUTO -> MaterialTheme.colorScheme.tertiary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = when (selectedRadioTransport) {
                                RadioTransport.HYBRID_AUTO -> "Hybrid Auto: BLE (<100m) / LoRa (>100m)"
                                RadioTransport.BLUETOOTH_DIRECT -> "Bluetooth Direct P2P (<100m Nearby)"
                                RadioTransport.LORA_MESH -> "LoRa Long-Range Mesh (Multi-Hop)"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Message List
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (searchQuery.isNotBlank()) Icons.Default.SearchOff else Icons.Default.ChatBubbleOutline,
                                contentDescription = "Empty",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "No messages match '$searchQuery'" else "No mesh messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "Try typing another keyword or clear search." else "Type below to send a secure message over the LoRa mesh network.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onRepairClick = { viewModel.repairMedia(msg) },
                            onLongClick = { selectedMessageForAction = msg },
                            onClick = {
                                viewModel.inspectMessagePacket(msg)
                                showBitChatInspector = true
                            }
                        )
                    }
                }
            }

            // AI Decision Loading Status
            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "AI inspecting privacy level...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Minimalist Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isProcessing
                    ) {
                        Icon(
                            Icons.Default.AddPhotoAlternate,
                            contentDescription = "Attach Photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("message_input"),
                        placeholder = { Text("Message mesh...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        enabled = !isProcessing,
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    val isSendEnabled = inputText.isNotBlank() && !isProcessing
                    FloatingActionButton(
                        onClick = {
                            if (isSendEnabled) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        containerColor = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("send_button"),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // BitChat Mesh & Dynamic Security Testbench Sheet
    if (showBitChatInspector) {
        BitChatInspectorSheet(
            packet = latestBitChatPacket,
            relaySteps = latestRelaySteps,
            onDismiss = { showBitChatInspector = false },
            onTestSamplePeer = { samplePeer ->
                viewModel.createNewDirectChat(
                    peerName = samplePeer.callsign,
                    securityLevel = 0,
                    preferredTransport = RadioTransport.BLUETOOTH_DIRECT
                )
                Toast.makeText(context, "Connected to ${samplePeer.callsign} (${samplePeer.defaultHops} BLE Hops)", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Settings & Mode Sheet
    if (showSettingsSheet) {
        MeshSettingsSheet(
            viewModel = viewModel,
            onDismiss = { showSettingsSheet = false },
            onOpenPairing = {
                showPairingSheet = true
                viewModel.startScanning()
            },
            onOpenAssistant = { showAssistantSheet = true },
            onOpenQsvmInspector = { showAiHubSheet = true }
        )
    }

    // Bluetooth / LoRa Pairing Sheet
    if (showPairingSheet) {
        MeshPairingSheet(
            viewModel = viewModel,
            onDismiss = { showPairingSheet = false }
        )
    }

    if (showAiHubSheet) {
        AiModelHubSheet(
            viewModel = viewModel,
            onDismiss = { showAiHubSheet = false }
        )
    }

    if (showSecuritySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSecuritySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Manual Security Override",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                val options = listOf(
                    0 to "Auto (Smart Protection)",
                    1 to "Level 1: Fast (Efficiency Mode)",
                    2 to "Level 2: Standard Protection",
                    3 to "Level 3: Enhanced Protection",
                    4 to "Level 4: Maximum Protection"
                )
                
                options.forEach { (level, title) ->
                    val isSelected = selectedSecurityLevel == level
                    Card(
                        onClick = {
                            viewModel.setSecurityLevel(level)
                            showSecuritySheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setSecurityLevel(level)
                                    showSecuritySheet = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showAssistantSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAssistantSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val assistantMessages by viewModel.assistantMessages.collectAsStateWithLifecycle()
            val isAssistantTyping by viewModel.isAssistantTyping.collectAsStateWithLifecycle()
            var assistantInput by remember { mutableStateOf("") }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.SupportAgent, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Mesh Assistant Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = false
                ) {
                    items(assistantMessages.size) { index ->
                        val msg = assistantMessages[index]
                        val isUser = msg.isUser
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    if (isAssistantTyping) {
                        item {
                            Text("Typing...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                        }
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = assistantInput,
                        onValueChange = { assistantInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask for help...") },
                        shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (assistantInput.isNotBlank()) {
                                viewModel.sendAssistantMessage(assistantInput)
                                assistantInput = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }

    // Clear Chat Confirmation Dialog
    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            icon = {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Clear All Messages?") },
            text = { Text("This will permanently delete all messages and media stored locally on this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChatHistory()
                        showClearChatDialog = false
                        Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Safety Number Verification Dialog
    if (showSafetyNumberDialog) {
        SafetyNumberDialog(
            mySession = activeSession,
            peerChannel = selectedChannel,
            onDismiss = { showSafetyNumberDialog = false }
        )
    }

    // Message Long-Press Action Bottom Sheet
    if (selectedMessageForAction != null) {
        val targetMsg = selectedMessageForAction!!
        val actionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedMessageForAction = null },
            sheetState = actionSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    "Message Options",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Copy Text Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            clipboardManager.setText(AnnotatedString(targetMsg.text))
                            selectedMessageForAction = null
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("Copy Text", fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Delete Message Option
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.deleteMessage(targetMsg.id)
                            selectedMessageForAction = null
                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text("Delete Message", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    onRepairClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val isMyMessage = message.isSent
    val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMyMessage) 16.dp else 4.dp,
                bottomEnd = if (isMyMessage) 4.dp else 16.dp
            ),
            color = if (isMyMessage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isMyMessage) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                
                if (message.mediaBase64 != null) {
                    if (message.isMediaCorrupted) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.BrokenImage, contentDescription = "Corrupted", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                            Text("Packet Loss Detected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            OutlinedButton(onClick = onRepairClick, modifier = Modifier.padding(top = 8.dp)) {
                                Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Run FEC Repair")
                            }
                        }
                    } else {
                        val bitmap = remember(message.mediaBase64) {
                            try {
                                val bytes = Base64.decode(message.mediaBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Media",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .padding(vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("Failed to render media", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Transport badge (BLE vs LoRa vs Auto)
                        val (transIcon, transLabel, transColor) = when (message.transport) {
                            RadioTransport.BLUETOOTH_DIRECT -> Triple(Icons.Default.Bluetooth, "BLE Direct", Color(0xFF2979FF))
                            RadioTransport.LORA_MESH -> Triple(Icons.Default.CellTower, "LoRa Mesh", Color(0xFFFF9100))
                            RadioTransport.HYBRID_AUTO -> Triple(Icons.Default.ElectricBolt, "Hybrid", Color(0xFF00E676))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background((if (isMyMessage) MaterialTheme.colorScheme.onPrimary else transColor).copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                transIcon,
                                contentDescription = transLabel,
                                modifier = Modifier.size(9.dp),
                                tint = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else transColor
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                transLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else transColor
                            )
                        }

                        if (message.isEncrypted) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background((if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    modifier = Modifier.size(9.dp),
                                    tint = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(2.dp))
                                val badgeText = when (message.securityLevel) {
                                    2 -> "PROTECTED"
                                    3 -> "ENHANCED"
                                    4 -> "MAXIMUM"
                                    else -> "PROTECTED"
                                }
                                Text(
                                    badgeText,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Dynamic compute optimization badge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF2E7D32).copy(alpha = if (isMyMessage) 0.35f else 0.18f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = "Fast Mode",
                                    modifier = Modifier.size(9.dp),
                                    tint = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else Color(0xFF2E7D32)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    "FAST (92% Saved)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = if (isMyMessage) MaterialTheme.colorScheme.onPrimary else Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                    Text(
                        text = dateFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = (if (isMyMessage) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioTransportSelectionSheet(
    currentTransport: RadioTransport,
    onSelectTransport: (RadioTransport) -> Unit,
    onDismiss: () -> Unit
) {
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
            Text(
                "Wireless Radio Protocol",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Select how messages are transmitted across devices and peer nodes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            val options = listOf(
                Triple(
                    RadioTransport.HYBRID_AUTO,
                    "⚡ Hybrid Auto-Route (Recommended)",
                    "Automatically connects via high-speed Bluetooth BLE if the peer is nearby (<100m) and seamlessly shifts to long-range LoRa mesh if they are far away (>100m)."
                ),
                Triple(
                    RadioTransport.BLUETOOTH_DIRECT,
                    "🔵 Bluetooth Direct P2P (Nearby)",
                    "Forces direct 2.4 GHz Bluetooth connection. Ultra low latency (~15ms) and high throughput (up to 2 Mbps) for peers within 100 meters."
                ),
                Triple(
                    RadioTransport.LORA_MESH,
                    "📻 LoRa Long-Range Mesh (Multi-Hop)",
                    "Forces sub-GHz LoRa modulation (868/915 MHz). Reaches 1-15+ km through walls and terrain using decentralized multi-hop relay nodes."
                )
            )

            options.forEach { (transport, title, desc) ->
                val isSelected = currentTransport == transport
                Card(
                    onClick = { onSelectTransport(transport) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectTransport(transport) }
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

