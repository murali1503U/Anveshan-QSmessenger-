with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "fun startScanning() {",
    "val scannedDevices = meshService.scannedDevices\n\n    fun startScanning() {"
)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
