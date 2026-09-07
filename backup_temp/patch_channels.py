import re

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

content = re.sub(r'val isDemoMode by viewModel\.isDemoMode\.collectAsStateWithLifecycle\(\)\n', '', content)

# Look for Deployment/Simulation Mode Switch in ChannelsListScreen
content = re.sub(r'// Deployment / Simulation Mode Switch.*?Spacer\(modifier = Modifier\.height\(24\.dp\)\)', '', content, flags=re.DOTALL)

# In showCreateChannelDialog
content = content.replace("isDemoMode = isDemoMode,", "")

# in CreateChannelDialog signature
content = re.sub(r'isDemoMode: Boolean,\n', '', content)

# inside CreateChannelDialog logic
content = re.sub(r'\} else if \(isDemoMode\) \{\n\s*// In demo mode.*?\} else \{', '} else {', content, flags=re.DOTALL)
content = content.replace("val isPresharedCodeRequired = !isDemoMode", "val isPresharedCodeRequired = true")

# label logic
content = content.replace('if (isDemoMode) "Pre-Shared Secret Code (Optional for Simulation)"\n                                else "Pre-Shared Secret Code (MANDATORY)"', '"Pre-Shared Secret Code (MANDATORY)"')
content = content.replace('if (isDemoMode) "e.g. 123456 (optional)"\n                                else "e.g. 123456"', '"e.g. 123456"')

content = re.sub(r'\} else if \(!isDemoMode\) \{\n.*?\}', '}', content, flags=re.DOTALL)

content = content.replace("enabled = peerName.isNotBlank() && (isDemoMode || sharedSecret.isNotBlank())", "enabled = peerName.isNotBlank() && sharedSecret.isNotBlank()")
content = content.replace("if (peerName.isNotBlank() && (isDemoMode || sharedSecret.isNotBlank()))", "if (peerName.isNotBlank() && sharedSecret.isNotBlank())")

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
