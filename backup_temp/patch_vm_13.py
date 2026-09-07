with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement = """
    fun sendMessage(text: String) {
        if (text.isBlank()) return
        if (text.length > 4096) {
            // Poka-Yoke: prevent huge messages
            Timber.e("Message too long (max 4096 chars). Discarding.")
            return
        }
        val targetChannelId = _currentChannelId.value
"""
content = content.replace("    fun sendMessage(text: String) {\n        if (text.isBlank()) return\n        val targetChannelId = _currentChannelId.value", replacement)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
