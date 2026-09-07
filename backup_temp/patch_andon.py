import re

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

replacement = """                                    .background(
                                        when (connectionState) {
                                            ConnectionState.CONNECTED -> Color(0xFF4CAF50) // Green
                                            ConnectionState.SCANNING, ConnectionState.CONNECTING -> Color(0xFFFFC107) // Yellow
                                            else -> Color(0xFFF44336) // Red
                                        }
                                    )"""

content = re.sub(
    r'\.background\([^)]*if \(connectionState == ConnectionState\.CONNECTED\)[^)]*\)',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
