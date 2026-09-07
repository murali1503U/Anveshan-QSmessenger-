import re

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

old_scan_start = r"""        _discoveredDevices\.value = emptyList\(\)
        _connectionState\.value = ConnectionState\.SCANNING"""

new_scan_start = """        _discoveredDevices.value = emptyList()
        realBluetoothManager.setConnectionState(ConnectionState.SCANNING)"""

content = re.sub(old_scan_start, new_scan_start, content)

old_scan_end = r"""                if \(_connectionState\.value == ConnectionState\.SCANNING\) \{
                    _connectionState\.value = ConnectionState\.DISCONNECTED
                \}"""

new_scan_end = """                if (realBluetoothManager.connectionState.value == ConnectionState.SCANNING) {
                    realBluetoothManager.setConnectionState(ConnectionState.DISCONNECTED)
                }"""

content = re.sub(old_scan_end, new_scan_end, content)

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/meshchat/data/RealBluetoothManager.kt", "r") as f:
    content = f.read()

content = content.replace("fun disconnect() {", """fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }
    
    fun disconnect() {""")

with open("app/src/main/java/com/example/meshchat/data/RealBluetoothManager.kt", "w") as f:
    f.write(content)

