import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement = """
        messages = _currentChannelId
            .flatMapLatest { channelId ->
                if (channelId.isBlank()) {
                    flowOf(emptyList<Message>())
                } else {
                    repository.getMessagesForChannel(channelId)
                }
            }
            .stateIn(
"""
content = re.sub(r'messages = _currentChannelId.*?\.stateIn\(', replacement.strip() + '(', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
