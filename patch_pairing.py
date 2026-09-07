import re

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "r") as f:
    content = f.read()

replacement = """
    val availableDevices by viewModel.scannedDevices.collectAsStateWithLifecycle(emptyList())
    var deviceToPair by remember { mutableStateOf<IoTDevice?>(null) }
"""
content = re.sub(
    r'val availableDevices = listOf\([^\]]+?\]\n.*?var deviceToPair by remember \{ mutableStateOf<IoTDevice\?>\(null\) \}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "w") as f:
    f.write(content)
