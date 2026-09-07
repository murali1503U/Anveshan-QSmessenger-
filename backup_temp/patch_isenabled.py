import re

def fix_file(path):
    with open(path, "r") as f:
        content = f.read()

    old_code = r'if \(bluetoothAdapter == null || !bluetoothAdapter\.isEnabled\) \{?'
    new_code = r"""val isAdapterEnabled = try { bluetoothAdapter?.isEnabled == true } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) {"""

    # Using re.sub to replace it, but let's be careful about braces
    # In LoRaMeshService: "if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {"
    # In RealBluetoothManager: "if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return"
    
    # We will just replace "bluetoothAdapter == null || !bluetoothAdapter.isEnabled" with "try { bluetoothAdapter?.isEnabled != true } catch (e: SecurityException) { true }"
    content = content.replace("bluetoothAdapter == null || !bluetoothAdapter.isEnabled",
                              "try { bluetoothAdapter?.isEnabled != true } catch (e: SecurityException) { true }")
                              
    with open(path, "w") as f:
        f.write(content)

fix_file("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt")
fix_file("app/src/main/java/com/example/meshchat/data/RealBluetoothManager.kt")
