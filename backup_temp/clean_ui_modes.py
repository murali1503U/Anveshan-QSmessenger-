import re

# 1. SetupWizardScreen
with open("app/src/main/java/com/example/meshchat/ui/SetupWizardScreen.kt", "r") as f:
    setup = f.read()
# Setup wizard had radio buttons for Simulation vs Deployment. We should remove that whole section.
# Let's just remove the block from 'Text("Operating Mode"' down to the end of the radio buttons.
setup = re.sub(r'Text\(\s*"Operating Mode"[\s\S]*?Spacer\(Modifier\.height\(32\.dp\)\)', 'Spacer(Modifier.height(32.dp))', setup)
with open("app/src/main/java/com/example/meshchat/ui/SetupWizardScreen.kt", "w") as f:
    f.write(setup)

# 2. MeshPairingSheet
with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "r") as f:
    pairing = f.read()
pairing = re.sub(r'val isDemoMode by viewModel\.isDemoMode\.collectAsStateWithLifecycle\(\)\n', '', pairing)
# The whole OutlinedCard for Deployment Security Policy / Simulation Mode
pairing = re.sub(r'OutlinedCard\(\s*shape = RoundedCornerShape\(16\.dp\)[\s\S]*?Spacer\(Modifier\.height\(24\.dp\)\)', 'Spacer(Modifier.height(24.dp))', pairing)
pairing = pairing.replace("if (isDemoMode) {", "if (false) {") # Disable bypass
with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "w") as f:
    f.write(pairing)

# 3. MeshSettingsSheet
with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "r") as f:
    settings = f.read()
settings = re.sub(r'val isDemoMode by viewModel\.isDemoMode\.collectAsStateWithLifecycle\(\)\n', '', settings)
# Remove the Deployment Mode Card
settings = re.sub(r'OutlinedCard\([\s\S]*?Deployment Mode[\s\S]*?Switch\([\s\S]*?\}\n\s*\)', '', settings)
# Remove "Read Setup Wizard" if present
settings = re.sub(r'OutlinedCard\([\s\S]*?Open Setup Wizard[\s\S]*?\}\n\s*\)', '', settings)
# Remove "System Notification Test" if present
settings = re.sub(r'OutlinedCard\([\s\S]*?System Notification Test[\s\S]*?\}\n\s*\)', '', settings)
with open("app/src/main/java/com/example/meshchat/ui/MeshSettingsSheet.kt", "w") as f:
    f.write(settings)

# 4. ChannelsListScreen
with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "r") as f:
    channels = f.read()
channels = re.sub(r'color = if \(!isDemoMode\) MaterialTheme\.colorScheme\.primary\.copy\(alpha = 0\.15f\)\n\s*else MaterialTheme\.colorScheme\.secondary\.copy\(alpha = 0\.15f\)', 'color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)', channels)
channels = re.sub(r'if \(!isDemoMode\) "DEPLOYED • PSK MANDATORY" else "SIMULATION • PSK OPTIONAL"', '"DEPLOYED • PSK MANDATORY"', channels)
channels = re.sub(r'color = if \(!isDemoMode\) MaterialTheme\.colorScheme\.primary else MaterialTheme\.colorScheme\.secondary,', 'color = MaterialTheme.colorScheme.primary,', channels)
channels = re.sub(r'if \(isDemoMode\) "Pre-Shared Secret Code \(Optional for Simulation\)"\n\s*else "Pre-Shared Secret Code \(MANDATORY\)"', '"Pre-Shared Secret Code (MANDATORY)"', channels)
channels = re.sub(r'if \(isDemoMode\) "e\.g\. 123456 \(optional\)"\n\s*else "e\.g\. 123456"', '"e.g. 123456"', channels)
with open("app/src/main/java/com/example/meshchat/ui/ChannelsListScreen.kt", "w") as f:
    f.write(channels)

print("UI Cleaned")
