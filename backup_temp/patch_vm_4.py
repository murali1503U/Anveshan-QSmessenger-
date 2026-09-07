import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()
content = re.sub(r'private val peerResponder = MeshPeerResponder\(.*?\)[\s\n]*', '', content)
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
