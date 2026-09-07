import re
with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

content = content.replace("else \"Pre-Shared Secret Code * (Mandatory for Deployment)\"", "")
content = content.replace("else \"Required cryptographic key / PIN\"", "")
content = content.replace("Text(\"Optional in simulation mode.\", color = MaterialTheme.colorScheme.onSurfaceVariant)", "Text(\"Required cryptographic key / PIN\", color = MaterialTheme.colorScheme.onSurfaceVariant)")

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
