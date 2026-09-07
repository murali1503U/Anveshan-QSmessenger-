import re

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "r") as f:
    content = f.read()

replacement = """
                                if (preSharedCodeInput.isNotBlank()) {
                                    viewModel.connectToDevice(targetDevice, preSharedCodeInput.trim()) { success ->
                                        if (!success) {
                                            android.widget.Toast.makeText(context, "Connection failed. Verify PSK.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    deviceToPair = null
"""

content = re.sub(
    r'if \(preSharedCodeInput\.isNotBlank\(\)\) \{\n\s*viewModel\.connectToDevice\(targetDevice,\s*preSharedCodeInput\.trim\(\)\)\n\s*deviceToPair = null',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "w") as f:
    f.write(content)
