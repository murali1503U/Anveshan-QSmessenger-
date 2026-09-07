import re

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement = """
    val loRaState: StateFlow<ConnectionState> = meshService.loRaState
    val connectionState: StateFlow<ConnectionState> = meshService.connectionState
"""
content = re.sub(r'val connectionState: StateFlow<ConnectionState> = meshService.connectionState', replacement.strip(), content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
