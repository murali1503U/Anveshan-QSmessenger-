import re

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "r") as f:
    content = f.read()

old_code = r'if \(try \{ bluetoothAdapter\?\.isEnabled != true \} catch \(e: SecurityException\) \{ true \}\) \{'
new_code = r"""if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) {"""

content = re.sub(old_code, new_code, content)

with open("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/meshchat/data/RealBluetoothManager.kt", "r") as f:
    content = f.read()

old_code = r'if \(try \{ bluetoothAdapter\?\.isEnabled != true \} catch \(e: SecurityException\) \{ true \}\) return'
new_code = r"""if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) return"""

content = re.sub(old_code, new_code, content)

with open("app/src/main/java/com/example/meshchat/data/RealBluetoothManager.kt", "w") as f:
    f.write(content)
