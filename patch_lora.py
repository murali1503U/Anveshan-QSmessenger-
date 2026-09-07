with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

import re

# Remove isDemoSimulationEnabled
content = re.sub(r'private val _isDemoSimulationEnabled.*?asStateFlow\(\)', '', content, flags=re.DOTALL)
content = re.sub(r'fun setDemoSimulation.*?\}', '', content, flags=re.DOTALL)

# In connectToDevice
content = content.replace("if (!_isDemoSimulationEnabled.value && preSharedCode.isBlank())", "if (preSharedCode.isBlank())")
content = re.sub(r'simulationJob\?\.cancel\(\)\n\s*simulationJob = CoroutineScope\(Dispatchers\.IO\)\.launch \{ simulateIncomingMessages\(\) \}', '', content)

# In disconnect
content = content.replace("simulationJob?.cancel()", "")

# Remove simulateIncomingMessages entirely
content = re.sub(r'private suspend fun simulateIncomingMessages\(\) \{[\s\S]*?\}\n    \}', '', content)
content = re.sub(r'private suspend fun simulateIncomingMessages\(\) \{[\s\S]*?\}', '', content) # might need a stronger regex if nested

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)
