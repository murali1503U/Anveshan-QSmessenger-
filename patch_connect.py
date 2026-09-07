import re

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

old_connect = """        scanJob?.cancel()
        bluetoothAdapter?.cancelDiscovery()
        val btDevice = bluetoothAdapter?.getRemoteDevice(device.id) ?: return@withContext false
        
        return@withContext realBluetoothManager.attemptConnect(btDevice, preSharedCode)"""

new_connect = """        scanJob?.cancel()
        val btDevice = try {
            bluetoothAdapter?.cancelDiscovery()
            bluetoothAdapter?.getRemoteDevice(device.id)
        } catch (e: SecurityException) {
            Log.e("LoRaMeshService", "Missing Bluetooth permission for connect")
            null
        } ?: return@withContext false
        
        return@withContext realBluetoothManager.attemptConnect(btDevice, preSharedCode)"""

content = content.replace(old_connect, new_connect)

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)
