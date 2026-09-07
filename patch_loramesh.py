import re

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

old_scan = r"""                                name = it.name \?: "Unknown Device",
                                rssi = intent.getShortExtra\(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE\).toInt\(\),"""

new_scan = """                                name = try { it.name ?: "Unknown Device" } catch (e: SecurityException) { "Unknown Device" },
                                rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt(),"""
content = re.sub(old_scan, new_scan, content)

old_receive = r"""if \(current\.none \{ d -> d\.id == newDevice\.id \}\) \{
                                current\.add\(newDevice\)
                                _discoveredDevices\.value = current
                            \}"""

new_receive = """if (current.none { d -> d.id == newDevice.id }) {
                                current.add(newDevice)
                                _discoveredDevices.value = current
                                Log.d("BluetoothScan", "Device found: ${newDevice.name} (${newDevice.id})")
                            }"""
content = re.sub(old_receive, new_receive, content)

old_start = r"""bluetoothAdapter.startDiscovery\(\)
                delay\(12000\)"""

new_start = """Log.d("BluetoothScan", "Scanning started")
                bluetoothAdapter.startDiscovery()
                delay(12000)"""
content = re.sub(old_start, new_start, content)

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)
