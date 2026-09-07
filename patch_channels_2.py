import re

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

# Lines 228-235: The Deployment Mode Pill in Top Bar, let's just remove the entire Surface that contains "DEPLOYED • PSK MANDATORY"
content = re.sub(r'Surface\(\n\s*shape = RoundedCornerShape\(6\.dp\).*?Text\([\s\S]*?\)\n\s*\}\n', '', content)

content = re.sub(r'\} else if \(isDemoMode\) \{\n\s*onAddChannel\([^\)]+\)\n\s*showCreateChannelDialog = false\n\s*\}', '', content)
content = re.sub(r'if \(isDemoMode\) "Pre-Shared Secret Code \(Optional for Simulation\)"[\s\n]*else "Pre-Shared Secret Code \(MANDATORY\)"', '"Pre-Shared Secret Code (MANDATORY)"', content)
content = re.sub(r'if \(isDemoMode\) "e.g\. 123456 \(optional\)"[\s\n]*else "e\.g\. 123456"', '"e.g. 123456"', content)

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
