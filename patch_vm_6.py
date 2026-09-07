import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "messages = _activeSessionId\n            .flatMapLatest { sessionId ->\n                if (sessionId == null) {\n                    flowOf(emptyList<com.example.meshchat.data.ChatChannel>())",
    "messages = _activeSessionId\n            .flatMapLatest { sessionId ->\n                if (sessionId == null) {\n                    flowOf(emptyList<Message>())"
)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
