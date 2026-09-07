import re

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

old_scan = r"""    fun startScanning\(\) \{
        viewModelScope\.launch \{
            meshService\.startScanning\(\)
        \}
    \}"""

new_scan = """    fun startScanning() {
        viewModelScope.launch {
            meshService.startScanning()
        }
    }"""
content = re.sub(old_scan, new_scan, content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
