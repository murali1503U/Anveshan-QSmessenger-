import re

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

old_scan_start = r"""        _discoveredDevices\.value = emptyList\(\)

        scanJob\?\.cancel\(\)"""

new_scan_start = """        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.SCANNING

        scanJob?.cancel()"""

content = re.sub(old_scan_start, new_scan_start, content)

old_scan_end = r"""                delay\(12000\)
                bluetoothAdapter.cancelDiscovery\(\)
            \} catch"""

new_scan_end = """                delay(12000)
                bluetoothAdapter.cancelDiscovery()
                if (_connectionState.value == ConnectionState.SCANNING) {
                    _connectionState.value = ConnectionState.DISCONNECTED
                }
            } catch"""

content = re.sub(old_scan_end, new_scan_end, content)

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)
