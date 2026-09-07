import re

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

# Remove _isDemoMode and isDemoMode
content = re.sub(r'private val _isDemoMode = MutableStateFlow\(.*?\)[\s\n]*val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow\(\)\n', '', content)

content = re.sub(r'fun setDemoMode.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'fun toggleDemoMode.*?\}', '', content, flags=re.DOTALL)

# In createSession
content = re.sub(r'isDemoMode: Boolean = false,', '', content)
content = content.replace("setDemoMode(isDemoMode)", "")
content = re.sub(r'sessionRepository\.setDemoSimulation\(session\.sessionId, isDemoMode\)', '', content)

# Modify messages flow mapping (it was combining _isDemoMode)
# Actually, let's just replace the whole flow definition if needed, or simply let's rewrite the block:
# Look for:
#        messages = _isDemoMode
#            .flatMapLatest { isDemo -> ... }
# Let's replace flatMapLatest with the direct flow
messages_replace = """        messages = _activeSessionId
            .flatMapLatest { sessionId ->
                if (sessionId == null) {
                    flowOf(emptyList())
                } else {
                    messageRepository.getMessagesBySession(sessionId)
                }
            }"""
content = re.sub(r'messages = _isDemoMode.*?\.stateIn', messages_replace + '\n            .stateIn', content, flags=re.DOTALL)

channels_replace = """        channels = _activeSessionId
            .flatMapLatest { sessionId ->
                if (sessionId == null) {
                    flowOf(emptyList())
                } else {
                    sessionRepository.getChannelsBySession(sessionId)
                }
            }"""
content = re.sub(r'channels = _isDemoMode.*?\.stateIn', channels_replace + '\n            .stateIn', content, flags=re.DOTALL)

# In setActiveSessionId
content = re.sub(r'val initialIsDemo = sessionRepository\.isDemoSimulation\(id\)[\s\n]*_isDemoMode\.value = initialIsDemo', '', content)

# In sendMessage
content = content.replace("val currentIsDemo = _isDemoMode.value\n", "")
content = content.replace("val isSimulated = currentIsDemo", "val isSimulated = false")

# In receiveMessage
content = content.replace("val currentIsDemo = _isDemoMode.value\n", "")
# the param `isSimulated = currentIsDemo` -> `isSimulated = false`
content = content.replace("isSimulated = currentIsDemo", "isSimulated = false")

# In getChannels
content = content.replace('val fallback = if (_isDemoMode.value) "demo_broadcast" else "global_broadcast"', 'val fallback = "global_broadcast"')

# In handlePairingAttempt
content = content.replace("val currentIsDemo = _isDemoMode.value\n", "")

# In createChannel
content = content.replace("val currentIsDemo = _isDemoMode.value\n", "")

# In simulateReceivingMessage (we might want to remove this or keep it as a test function, but user says "No fake message injection")
# Actually, the user says "No simulated peers. No fake message injection."
# So let's delete simulateReceivingMessage entirely.
content = re.sub(r'fun simulateReceivingMessage.*?// Optionally add some artificial delay.*?\}', '', content, flags=re.DOTALL)
content = re.sub(r'fun simulateReceivingMessage.*?\}', '', content, flags=re.DOTALL)

# In refreshMockNodes
content = re.sub(r'fun refreshMockNodes.*?\}', '', content, flags=re.DOTALL)

# In startFakeIncomingMessageLoop
content = re.sub(r'fun startFakeIncomingMessageLoop.*?\}', '', content, flags=re.DOTALL)

# Also remove calls to startFakeIncomingMessageLoop if any
content = re.sub(r'startFakeIncomingMessageLoop\(\)', '', content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
