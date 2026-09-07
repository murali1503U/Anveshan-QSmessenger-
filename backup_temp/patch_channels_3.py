with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    content = f.read()

import re

# Remove the specific Surface block using manual searching
lines = content.split('\n')
new_lines = []
skip = False
for line in lines:
    if "DEPLOYED • PSK MANDATORY" in line:
        pass # we are already in the block
    if "color = if (!isDemoMode)" in line and "primary.copy" in line:
        pass
    if "if (!isDemoMode) \"DEPLOYED • PSK MANDATORY\"" in line:
        pass
    if "} else if (isDemoMode) {" in line:
        pass # actually this needs replacing
        
content = content.replace("color = if (!isDemoMode) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)\n                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)", "color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)")
content = content.replace("if (!isDemoMode) \"DEPLOYED • PSK MANDATORY\" else \"SIMULATION • PSK OPTIONAL\"", "\"DEPLOYED • PSK MANDATORY\"")
content = content.replace("color = if (!isDemoMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,", "color = MaterialTheme.colorScheme.primary,")

content = content.replace('if (isDemoMode) "Pre-Shared Secret Code (Optional for Simulation)"\n                                else "Pre-Shared Secret Code (MANDATORY)"', '"Pre-Shared Secret Code (MANDATORY)"')
content = content.replace('if (isDemoMode) "e.g. 123456 (optional)"\n                                else "e.g. 123456"', '"e.g. 123456"')

content = re.sub(r'\} else if \(isDemoMode\) \{\n\s*onAddChannel.*?\}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(content)
