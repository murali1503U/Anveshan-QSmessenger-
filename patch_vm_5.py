import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement = """
    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSession: StateFlow<SecureSession?>
"""
content = content.replace("    val activeSession: StateFlow<SecureSession?>", replacement)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
