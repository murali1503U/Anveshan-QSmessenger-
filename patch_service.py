import re

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

old_discovery = """            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
            bluetoothAdapter.startDiscovery()
            delay(12000)
            bluetoothAdapter.cancelDiscovery()
            try { context.unregisterReceiver(receiver) } catch (e: Exception) {}"""

new_discovery = """            try {
                context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
                if (bluetoothAdapter.isDiscovering) bluetoothAdapter.cancelDiscovery()
                bluetoothAdapter.startDiscovery()
                delay(12000)
                bluetoothAdapter.cancelDiscovery()
            } catch (e: SecurityException) {
                Log.e("LoRaMeshService", "Missing Bluetooth permissions", e)
            } catch (e: Exception) {
                Log.e("LoRaMeshService", "Discovery error", e)
            } finally {
                try { context.unregisterReceiver(receiver) } catch (e: Exception) {}
            }"""

content = content.replace(old_discovery, new_discovery)

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)
