import re
with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = re.sub(r'_isDemoMode\.value = initialIsDemo', '', content)
content = re.sub(r'if \(_isDemoMode\.value\) \{.*?\}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
