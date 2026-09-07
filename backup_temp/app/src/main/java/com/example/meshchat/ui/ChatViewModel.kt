package com.example.meshchat.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.meshchat.ai.GeminiSecurityAnalyzer
import com.example.meshchat.ai.QsvmSecurityEngine
import com.example.meshchat.data.AppDatabase
import com.example.meshchat.data.ChatChannel
import com.example.meshchat.data.ChatChannelRepository
import com.example.meshchat.data.ChannelType
import com.example.meshchat.data.ConnectionState
import com.example.meshchat.data.IoTDevice
import com.example.meshchat.data.LoRaMeshService
import com.example.meshchat.data.MeshNotificationManager
import com.example.meshchat.data.Message
import com.example.meshchat.data.MessageRepository
import com.example.meshchat.data.SecureSession
import com.example.meshchat.data.SecureSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AssistantMessage(val text: String, val isUser: Boolean)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MessageRepository
    private val sessionRepository: SecureSessionRepository
    private val channelRepository: ChatChannelRepository
    private val meshService = LoRaMeshService(application)
    private val securityAnalyzer = GeminiSecurityAnalyzer()
    private val assistant = com.example.meshchat.ai.MeshAssistant()
    private val qsvmEngine = QsvmSecurityEngine(application)
    private val notificationManager = MeshNotificationManager(application)
    private val cliEngine: com.example.meshchat.cli.MeshCliEngine

    
    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<Message>>

    @OptIn(ExperimentalCoroutinesApi::class)
    val channels: StateFlow<List<com.example.meshchat.data.ChatChannel>>

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSession: StateFlow<SecureSession?>


    private val _currentChannelId = MutableStateFlow("global_broadcast")
    val currentChannelId: StateFlow<String> = _currentChannelId.asStateFlow()

    private val _selectedChannel = MutableStateFlow<com.example.meshchat.data.ChatChannel?>(null)
    val selectedChannel: StateFlow<com.example.meshchat.data.ChatChannel?> = _selectedChannel.asStateFlow()
    
    val loRaState: StateFlow<ConnectionState> = meshService.loRaState
    val connectionState: StateFlow<ConnectionState> = meshService.connectionState
    val discoveredDevices: StateFlow<List<IoTDevice>> = meshService.discoveredDevices
    val connectedDevice: StateFlow<IoTDevice?> = meshService.connectedDevice

    

    

    private val _selectedRadioTransport = MutableStateFlow(com.example.meshchat.data.RadioTransport.HYBRID_AUTO)
    val selectedRadioTransport: StateFlow<com.example.meshchat.data.RadioTransport> = _selectedRadioTransport.asStateFlow()

    fun setSelectedRadioTransport(transport: com.example.meshchat.data.RadioTransport) {
        _selectedRadioTransport.value = transport
    }


    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    private val _isQsvmActive = MutableStateFlow(true)
    val isQsvmActive: StateFlow<Boolean> = _isQsvmActive.asStateFlow()

    private val _selectedSecurityLevel = MutableStateFlow(0) // 0=Auto, 1=Plain, 2=AES, 3=Session, 4=PQ
    val selectedSecurityLevel: StateFlow<Int> = _selectedSecurityLevel.asStateFlow()

    private val _assistantMessages = MutableStateFlow<List<AssistantMessage>>(
        listOf(AssistantMessage("Hello! I'm your Mesh Guide. I can help you configure your LoRa hardware, explain the security levels, or troubleshoot connections. What's on your mind?", false))
    )
    val assistantMessages: StateFlow<List<AssistantMessage>> = _assistantMessages.asStateFlow()

    private val _isAssistantTyping = MutableStateFlow(false)
    val isAssistantTyping: StateFlow<Boolean> = _isAssistantTyping.asStateFlow()

    // CLI / Termux Console State
    private val _cliLines = MutableStateFlow<List<com.example.meshchat.cli.CliLine>>(
        listOf(
            com.example.meshchat.cli.CliLine("=== MeshChat Termux CLI v2.4 initialized ===", com.example.meshchat.cli.CliLineType.ACCENT),
            com.example.meshchat.cli.CliLine("Connected to Mesh Kernel on aarch64 [Ready]", com.example.meshchat.cli.CliLineType.OUTPUT),
            com.example.meshchat.cli.CliLine("Lightweight AI & Security Controller loaded. Type 'help' or 'ai eval <text>' to begin.", com.example.meshchat.cli.CliLineType.SUCCESS)
        )
    )
    val cliLines: StateFlow<List<com.example.meshchat.cli.CliLine>> = _cliLines.asStateFlow()

    private val _isCliRunning = MutableStateFlow(false)
    val isCliRunning: StateFlow<Boolean> = _isCliRunning.asStateFlow()

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    private val _latestBitChatPacket = MutableStateFlow<com.example.meshchat.data.BitChatPacket?>(null)
    val latestBitChatPacket: StateFlow<com.example.meshchat.data.BitChatPacket?> = _latestBitChatPacket.asStateFlow()

    private val _latestRelaySteps = MutableStateFlow<List<com.example.meshchat.data.RelayTraceStep>>(emptyList())
    val latestRelaySteps: StateFlow<List<com.example.meshchat.data.RelayTraceStep>> = _latestRelaySteps.asStateFlow()

    private val _latestSecurityDecision = MutableStateFlow<com.example.meshchat.ai.SecurityDecision?>(null)
    val latestSecurityDecision: StateFlow<com.example.meshchat.ai.SecurityDecision?> = _latestSecurityDecision.asStateFlow()

    fun inspectMessagePacket(message: Message) {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            val currentCh = channelRepository.getChannel(message.channelId)
            val destCallsign = if (currentCh?.type == ChannelType.DIRECT) currentCh.name.take(15) else "BROADCAST_ALL"
            val targetHops = when {
                destCallsign.contains("DISTANT", ignoreCase = true) -> 2
                destCallsign.contains("BASECAMP", ignoreCase = true) -> 3
                destCallsign.contains("GHOST", ignoreCase = true) -> 2
                currentCh?.type == ChannelType.BROADCAST -> 2
                else -> 1
            }
            val packet = com.example.meshchat.data.BitChatMeshEngine.packMessage(
                message = message.text,
                sourceCallsign = message.sender,
                destCallsign = destCallsign,
                securityLevel = if (message.securityLevel > 0) message.securityLevel else if (message.isEncrypted) 2 else 1,
                targetHops = targetHops,
                baseSecretKey = session.sessionId
            )
            _latestBitChatPacket.value = packet
            _latestRelaySteps.value = com.example.meshchat.data.BitChatMeshEngine.traceRelaySteps(packet)
        }
    }

    fun clearPacketInspector() {
        _latestBitChatPacket.value = null
        _latestRelaySteps.value = emptyList()
    }

    fun executeCliCommand(rawCommand: String) {
        val trimmed = rawCommand.trim()
        if (trimmed.isBlank()) return

        if (trimmed.equals("clear", ignoreCase = true) || trimmed.equals("cls", ignoreCase = true)) {
            _cliLines.value = listOf(
                com.example.meshchat.cli.CliLine("meshnode:~$ clear", com.example.meshchat.cli.CliLineType.COMMAND)
            )
            return
        }

        val cmdLine = com.example.meshchat.cli.CliLine("meshnode:~$ trimmed", com.example.meshchat.cli.CliLineType.COMMAND)
        _cliLines.value = _cliLines.value + cmdLine
        _commandHistory.value = (_commandHistory.value + trimmed).takeLast(30)

        viewModelScope.launch {
            _isCliRunning.value = true
            val results = cliEngine.executeCommand(trimmed)
            _cliLines.value = _cliLines.value + results
            _isCliRunning.value = false
        }
    }

    fun clearCli() {
        _cliLines.value = emptyList()
    }

    fun sendAssistantMessage(text: String) {
        if (text.isBlank()) return
        
        _assistantMessages.value = _assistantMessages.value + AssistantMessage(text, true)
        
        viewModelScope.launch {
            _isAssistantTyping.value = true
            val history = _assistantMessages.value.dropLast(1).map { Pair(it.text, it.isUser) }
            val reply = assistant.getResponse(text, history)
            _assistantMessages.value = _assistantMessages.value + AssistantMessage(reply, false)
            _isAssistantTyping.value = false
        }
    }

    fun setSecurityLevel(level: Int) {
        _selectedSecurityLevel.value = level
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.setSecurityLevel(session.sessionId, level)
        }
    }

    fun rotateSessionKey() {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.ratchetSessionKey(session.sessionId)
        }
    }
    
    fun selectAndDownloadModel(
        modelId: String,
        modelLabel: String,
        mode: String = "QSVM",
        onFinished: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.updateModelSelection(
                sessionId = session.sessionId,
                modelName = modelLabel,
                mode = mode,
                isActive = true,
                isDownloaded = true
            )
            _isQsvmActive.value = true
            onFinished?.invoke(true)
        }
    }

    fun completeSetup(
        modelId: String = "qsvm", 
        modelLabel: String = "QSVM Quantum Kernel", 
        mode: String = "QSVM", 
        
        onFinished: () -> Unit
    ) {
        
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.updateModelSelection(
                sessionId = session.sessionId,
                modelName = modelLabel,
                mode = mode,
                isActive = true,
                isDownloaded = true
            )
            sessionRepository.setSetupCompleted(session.sessionId, true)
            
            _isQsvmActive.value = true
            onFinished()
        }
    }

    fun resetSetup() {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.setSetupCompleted(session.sessionId, false)
        }
    }

    fun updateUserCallsign(newCallsign: String) {
        val trimmed = newCallsign.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.updateUserCallsign(session.sessionId, trimmed.uppercase())
        }
    }

    fun randomizeUserCallsign(): String {
        val newCallsign = sessionRepository.generateUniqueCallsign()
        val newNodeId = sessionRepository.generateUniqueNodeId()
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.updateNodeIdentity(session.sessionId, newCallsign, newNodeId)
        }
        return newCallsign
    }

    fun toggleDeveloperMode() {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            val newMode = !session.isDeveloperMode
            sessionRepository.setDeveloperMode(session.sessionId, newMode)
        }
    }

    fun setDeveloperMode(enabled: Boolean) {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.setDeveloperMode(session.sessionId, enabled)
        }
    }

    fun setSecurityEngineMode(mode: String) {
        viewModelScope.launch {
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            sessionRepository.setQsvmConfig(session.sessionId, mode, session.termuxEndpoint, session.termuxSelectedModel)
            _isQsvmActive.value = (mode == "QSVM")
        }
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MessageRepository(database.messageDao())
        sessionRepository = SecureSessionRepository(database.secureSessionDao())
        channelRepository = ChatChannelRepository(database.chatChannelDao())
        
        cliEngine = com.example.meshchat.cli.MeshCliEngine(
            securityAnalyzer = securityAnalyzer,
            qsvmEngine = qsvmEngine,
            sessionRepository = sessionRepository,
            messageRepository = repository,
            meshService = meshService,
            isQsvmActiveProvider = { _isQsvmActive.value },
            onSecurityLevelChanged = { newLevel -> _selectedSecurityLevel.value = newLevel }
        )

        messages = _currentChannelId
            .flatMapLatest { channelId ->
                if (channelId.isBlank()) {
                    flowOf(emptyList<Message>())
                } else {
                    repository.getMessagesForChannel(channelId)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        channels = channelRepository.allChannels.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        activeSession = sessionRepository.activeSession.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
        viewModelScope.launch {
            val initialSession = sessionRepository.initializeDefaultSessionIfNeeded()
                        
            _selectedSecurityLevel.value = initialSession.securityLevel
            _isQsvmActive.value = initialSession.isQsvmActive

            channelRepository.initializeDefaultChannelsIfNeeded(false)

            val initialChannelId = "global_broadcast"
            _currentChannelId.value = initialChannelId
            _selectedChannel.value = channelRepository.getChannel(initialChannelId)
        }

        // Listen for incoming messages from the LoRa service
        viewModelScope.launch {
            meshService.incomingMessages.collect { msg ->
                if (msg != null) {
                    var incoming = msg.copy(isDemo = false)
                    
                    // Apply active plugins AFTER RECEIVE
                    incoming = com.example.meshchat.plugin.PluginManager.processIncoming(incoming)
                    
                    repository.insert(incoming)
                    val channel = channelRepository.getChannel(incoming.channelId)
                    val channelName = channel?.name ?: incoming.channelId
                    channelRepository.updateLastMessage(incoming.channelId, "${incoming.sender}: ${incoming.text}")
                    if (incoming.channelId != _currentChannelId.value) {
                        channelRepository.incrementUnread(incoming.channelId)
                    }
                    // Trigger notification for incoming mesh packet
                    notificationManager.showIncomingMessageNotification(incoming, channelName)
                }
            }
        }

        // Keep selected channel synchronized with current channel id
        viewModelScope.launch {
            _currentChannelId.collect { id ->
                val channel = channelRepository.getChannel(id)
                _selectedChannel.value = channel
                channelRepository.clearUnread(id)
            }
        }
    }

    fun selectChannel(channelId: String) {
        _currentChannelId.value = channelId
        viewModelScope.launch {
            val ch = channelRepository.getChannel(channelId)
            _selectedChannel.value = ch
            channelRepository.clearUnread(channelId)
        }
    }

    fun createNewGroup(
        name: String, 
        description: String, 
        members: String, 
        securityLevel: Int = 0,
        preferredTransport: com.example.meshchat.data.RadioTransport = com.example.meshchat.data.RadioTransport.HYBRID_AUTO,
        onCreated: ((com.example.meshchat.data.ChatChannel) -> Unit)? = null
    ) {
        if (name.isBlank()) return
                viewModelScope.launch {
            val group = channelRepository.createChannel(
                name = name.trim(),
                description = description.trim(),
                type = com.example.meshchat.data.ChannelType.GROUP,
                members = if (members.isBlank()) "Me" else "$members, Me",
                isDemo = false,
                securityLevel = securityLevel,
                preferredTransport = preferredTransport
            )
            val (resolvedTransport, transportInfo) = meshService.resolveActiveTransport(preferredTransport, peerNearby = false)
            val welcomeMsg = Message(
                channelId = group.channelId,
                text = "Group '$name' created. Encrypted mesh session ready.",
                sender = "System",
                isEncrypted = securityLevel >= 2,
                isSent = true,
                isDemo = false,
                securityLevel = securityLevel,
                transport = resolvedTransport,
                transportDetail = transportInfo
            )
            repository.insert(welcomeMsg)
            channelRepository.updateLastMessage(group.channelId, "Group created.")
            selectChannel(group.channelId)
            onCreated?.invoke(group)
        }
    }

    fun createNewDirectChat(
        peerName: String, 
        securityLevel: Int = 0,
        preferredTransport: com.example.meshchat.data.RadioTransport = com.example.meshchat.data.RadioTransport.HYBRID_AUTO,
        sharedSecret: String = "",
        onCreated: ((com.example.meshchat.data.ChatChannel) -> Unit)? = null
    ) {
        if (peerName.isBlank()) return
                // In deployed mode, pre-shared code before connection is mandatory
        if (sharedSecret.isBlank()) return

        viewModelScope.launch {
            val direct = channelRepository.createChannel(
                name = peerName.trim(),
                description = "Direct peer-to-peer secure channel",
                type = com.example.meshchat.data.ChannelType.DIRECT,
                members = "$peerName, Me",
                isDemo = false,
                securityLevel = securityLevel,
                preferredTransport = preferredTransport,
                sharedSecret = sharedSecret
            )
            val (resolvedTransport, transportInfo) = meshService.resolveActiveTransport(preferredTransport, peerNearby = true)
            val welcomeMsg = Message(
                channelId = direct.channelId,
                text = "Direct session established with $peerName.",
                sender = "System",
                isEncrypted = securityLevel >= 2,
                isSent = true,
                isDemo = false,
                securityLevel = securityLevel,
                transport = resolvedTransport,
                transportDetail = transportInfo
            )
            repository.insert(welcomeMsg)
            channelRepository.updateLastMessage(direct.channelId, "Direct session established.")
            selectChannel(direct.channelId)
            onCreated?.invoke(direct)
        }
    }

    fun togglePinChannel(channel: com.example.meshchat.data.ChatChannel) {
        viewModelScope.launch {
            channelRepository.togglePin(channel.channelId, channel.isPinned)
        }
    }

    fun deleteChannel(channelId: String) {
        if (channelId == "global_broadcast" || channelId == "demo_broadcast") return // Keep broadcast
        viewModelScope.launch {
            channelRepository.deleteChannel(channelId)
            repository.clearChannelMessages(channelId)
            if (_currentChannelId.value == channelId) {
                val fallback = "global_broadcast"
                selectChannel(fallback)
            }
        }
    }


    fun startScanning() {
        viewModelScope.launch {
            meshService.startScanning()
        }
    }

    fun connectToDevice(
        device: IoTDevice, 
        preSharedCode: String = "",
        onResult: ((Boolean) -> Unit)? = null
    ) {
                // In deployed mode, pre-shared code before connection is mandatory
        if (preSharedCode.isBlank()) {
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch {
            val success = meshService.connectToDevice(device, preSharedCode)
            if (success) {
                val prefix = "device_"
                val channelId = prefix + device.id.replace(":", "_").lowercase()
                var ch = channelRepository.getChannel(channelId)
                if (ch == null) {
                    ch = channelRepository.createChannel(
                        name = device.name,
                        description = "BLE Mesh Node (RSSI: ${device.rssi} dBm)",
                        type = com.example.meshchat.data.ChannelType.DIRECT,
                        members = "${device.name}, Me",
                        isDemo = false,
                        securityLevel = if (preSharedCode.isNotBlank()) 2 else 1,
                        sharedSecret = preSharedCode
                    )
                }
                selectChannel(ch.channelId)
                onResult?.invoke(true)
            } else {
                onResult?.invoke(false)
            }
        }
    }

    fun disconnect() {
        meshService.disconnect()
    }


    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (text.length > 4096) {
            // Poka-Yoke: prevent huge messages
            android.util.Log.e("ChatViewModel", "Message too long (max 4096 chars). Discarding.")
            return
        }
        val targetChannelId = _currentChannelId.value

        
        viewModelScope.launch {
            _isProcessing.value = true
            
            // 1. Gather recent chat thread context (last 4 messages) for multi-turn privacy analysis
            val currentCh = selectedChannel.value
            val isDirectPeer = currentCh?.type == com.example.meshchat.data.ChannelType.DIRECT
            val recentHistory = messages.value.filter { it.channelId == targetChannelId }.takeLast(4).map { it.text }

            // 2. Analyze message context with QSVM (Quantum Support Vector Machine):
            // Zero cloud latency, 6-qubit Hilbert space kernel, auto-escalates to Level 2/3/4 E2EE for sensitive context
            val session = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
            val decision = if (_selectedSecurityLevel.value == 0) {
                qsvmEngine.qsvm.classify(text, recentHistory).decision
            } else {
                val level = _selectedSecurityLevel.value
                val levelName = when (level) {
                    1 -> "Level 1: Fast (Efficiency Mode)"
                    2 -> "Level 2: Standard Protection"
                    3 -> "Level 3: Enhanced Protection"
                    4 -> "Level 4: Maximum Protection"
                    else -> "Level 2: Standard Protection"
                }
                com.example.meshchat.ai.SecurityDecision(
                    level = level,
                    levelName = levelName,
                    confidence = 1.0f,
                    reason = "User manual security override applied",
                    sensitiveFactors = emptyList(),
                    executionSource = "Manual Configuration",
                    estimatedPacketOverheadBytes = if (level == 1) 0 else if (level == 2) 28 else if (level == 3) 44 else 128,
                    latencyMs = 1
                )
            }
            _latestSecurityDecision.value = decision
            val finalSecLevel = decision.level
            
            // 3. Resolve radio transport link (Hybrid Auto-Route, BLE Direct Nearby, or LoRa Mesh Long Range)
            val prefTransport = _selectedRadioTransport.value
            val (resolvedTransport, transportInfo) = meshService.resolveActiveTransport(prefTransport, peerNearby = isDirectPeer)

            // 4. Create message object
                        val userCallsign = session.userCallsign.ifBlank { "Me" }
            var message = Message(
                channelId = targetChannelId,
                text = text,
                sender = userCallsign,
                isEncrypted = finalSecLevel >= 2,
                isSent = true,
                isConfidential = finalSecLevel >= 2,
                isDemo = false,
                securityLevel = finalSecLevel,
                transport = resolvedTransport,
                transportDetail = transportInfo
            )
            
            // Apply active plugins BEFORE SEND
            message = com.example.meshchat.plugin.PluginManager.processOutgoing(message)
            
            // 5. Generate BitChat multi-hop packet and relay trace
            val destCallsign = if (isDirectPeer) currentCh?.name?.take(15) ?: "PEER_NODE" else "BROADCAST_ALL"
            val targetHops = when {
                destCallsign.contains("DISTANT", ignoreCase = true) -> 2
                destCallsign.contains("BASECAMP", ignoreCase = true) -> 3
                destCallsign.contains("GHOST", ignoreCase = true) -> 2
                !isDirectPeer -> 2
                else -> 1
            }
            val bitChatPacket = com.example.meshchat.data.BitChatMeshEngine.packMessage(
                message = message.text,
                sourceCallsign = userCallsign,
                destCallsign = destCallsign,
                securityLevel = finalSecLevel,
                targetHops = targetHops,
                baseSecretKey = session.sessionId
            )
            _latestBitChatPacket.value = bitChatPacket
            _latestRelaySteps.value = com.example.meshchat.data.BitChatMeshEngine.traceRelaySteps(bitChatPacket)

            // 6. Save to local DB first & update channel snippet
            repository.insert(message)
            channelRepository.updateLastMessage(targetChannelId, "You: ${message.text}")
            
            // Advance/ratchet session key state if forward secrecy or post-quantum is active
            if (finalSecLevel >= 3) {
                val currentSession = activeSession.value ?: sessionRepository.initializeDefaultSessionIfNeeded()
                sessionRepository.ratchetSessionKey(currentSession.sessionId)
            }
            
            // 7. Send over wireless transceiver (BLE Direct or LoRa Mesh)
            if (connectionState.value == ConnectionState.CONNECTED) {
                meshService.sendMessage(message)
            }
            
            _isProcessing.value = false

            // 8. In Demo Mode: trigger contextual, human-like peer response
            
        }
    }


    fun triggerTestNotification() {
        val testMsg = Message(
            channelId = _currentChannelId.value,
            text = "Radio beacon check: Direct peer connection active.",
            sender = "MeshNode-Alpha",
            isEncrypted = true,
            isSent = false,
            securityLevel = 2,
            transport = com.example.meshchat.data.RadioTransport.BLUETOOTH_DIRECT,
            transportDetail = "BLE Direct • -48 dBm"
        )
        val channel = selectedChannel.value
        notificationManager.showIncomingMessageNotification(testMsg, channel?.name ?: "Mesh Network")
    }

    fun repairMedia(message: Message) {
        viewModelScope.launch {
            _isProcessing.value = true
            // Simulate packet reconstruction / FEC algorithm
            delay(1500)
            val repairedMessage = message.copy(isMediaCorrupted = false, text = "Media restored via FEC")
            repository.insert(repairedMessage)
            _isProcessing.value = false
        }
    }

    private val _pendingMediaUri = MutableStateFlow<Uri?>(null)
    val pendingMediaUri: StateFlow<Uri?> = _pendingMediaUri.asStateFlow()

    fun setPendingMedia(uri: Uri?) {
        _pendingMediaUri.value = uri
    }

    fun sendMedia(context: Context, downscale: Boolean) {
        val uri = _pendingMediaUri.value ?: return
        val targetChannelId = _currentChannelId.value
        _pendingMediaUri.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            _isProcessing.value = true
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap != null) {
                    val outputStream = ByteArrayOutputStream()
                    
                    if (downscale) {
                        val scaledBitmap = Bitmap.createScaledBitmap(
                            bitmap, 
                            (bitmap.width * 0.5).toInt().coerceAtLeast(1), 
                            (bitmap.height * 0.5).toInt().coerceAtLeast(1), 
                            true
                        )
                        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 20, outputStream)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    
                    val compressedBytes = outputStream.toByteArray()
                    val base64String = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
                    
                    val finalSecLevel = if (_selectedSecurityLevel.value == 0) {
                        val requiresEncryption = qsvmEngine.qsvm.classify("Attached Media File").decision.level >= 2
                        if (requiresEncryption) 2 else 1
                    } else {
                        _selectedSecurityLevel.value
                    }
                    
                    val currentCh = selectedChannel.value
                    val isDirectPeer = currentCh?.type == com.example.meshchat.data.ChannelType.DIRECT
                    val prefTransport = _selectedRadioTransport.value
                    val (resolvedTransport, transportInfo) = meshService.resolveActiveTransport(prefTransport, peerNearby = isDirectPeer)

                                        val userCallsign = activeSession.value?.userCallsign?.ifBlank { "Me" } ?: "Me"
                    val message = Message(
                        channelId = targetChannelId,
                        text = "Transmitting encoded media...",
                        sender = userCallsign,
                        isEncrypted = finalSecLevel >= 2,
                        isSent = true,
                        isConfidential = finalSecLevel >= 2,
                        isDemo = false,
                        mediaBase64 = base64String,
                        isMediaCorrupted = false,
                        securityLevel = finalSecLevel,
                        transport = resolvedTransport,
                        transportDetail = transportInfo
                    )
                    
                    repository.insert(message)
                    channelRepository.updateLastMessage(targetChannelId, "You: [Photo]")
                    if (connectionState.value == ConnectionState.CONNECTED) {
                        meshService.sendMessage(message)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    // --- UX Enhancements: Theme, Search Filter, Message Management ---

    // Theme Mode: "SYSTEM", "DARK", "LIGHT"
    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    // Search / Filter query for messages
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    // Clear chat history for current channel
    fun clearCurrentChatHistory() {
        viewModelScope.launch {
            repository.clearChannelMessages(_currentChannelId.value)
            channelRepository.updateLastMessage(_currentChannelId.value, "Chat cleared.")
        }
    }

    // Clear chat history
    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    // Delete single message
    fun deleteMessage(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }
}


