import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement_messages = """
        messages = _currentChannelId
            .flatMapLatest { channelId ->
                if (channelId.isBlank()) {
                    flowOf(emptyList<Message>())
                } else {
                    repository.getMessagesForChannel(channelId)
                }
            }
"""

content = re.sub(r'messages = _activeSessionId\s*\.flatMapLatest \{ sessionId ->.*?\}', replacement_messages.strip(), content, flags=re.DOTALL)

replacement_channels = """
        channels = channelRepository.allChannels
"""
content = re.sub(r'channels = _activeSessionId\s*\.flatMapLatest \{ sessionId ->.*?\n            \}\s*\.stateIn\(.*?\)', replacement_channels.strip(), content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
