import re

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "r") as f:
    content = f.read()

replacement = """
fun MeshPairingSheet(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
"""

content = re.sub(
    r'fun MeshPairingSheet\(\n\s*viewModel: ChatViewModel,\n\s*onDismiss: \(\) -> Unit\n\) \{',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/meshchat/ui/MeshPairingSheet.kt", "w") as f:
    f.write(content)
