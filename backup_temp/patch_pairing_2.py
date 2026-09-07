import re

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "r") as f:
    content = f.read()

replacement = """
                                        deviceToPair = device
                                        preSharedCodeInput = ""
                                        hasAttemptedSubmit = false
"""
content = re.sub(
    r'if \(false\) \{.*?\} else \{.*?(deviceToPair = device.*?)\}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "w") as f:
    f.write(content)
