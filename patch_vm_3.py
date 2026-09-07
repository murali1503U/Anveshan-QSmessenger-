import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = re.sub(r'import com\.example\.meshchat\.ai\.MeshPeerResponder\n', '', content)
content = re.sub(r'private val responder = MeshPeerResponder\(\)\n', '', content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
