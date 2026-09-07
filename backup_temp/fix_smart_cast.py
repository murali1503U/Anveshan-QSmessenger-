import re

def fix_file(path, is_lora):
    with open(path, "r") as f:
        content = f.read()

    old_code = r'val isAdapterEnabled = try \{ bluetoothAdapter\?\.isEnabled == true \} catch \(e: SecurityException\) \{ false \}\s*if \(!isAdapterEnabled\) \{'
    if not is_lora:
        old_code = r'val isAdapterEnabled = try \{ bluetoothAdapter\?\.isEnabled == true \} catch \(e: SecurityException\) \{ false \}\s*if \(!isAdapterEnabled\) return'
    
    new_code = r"""if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) {"""
    
    if not is_lora:
        new_code = r"""if (bluetoothAdapter == null) return
        val isAdapterEnabled = try { bluetoothAdapter.isEnabled } catch (e: SecurityException) { false }
        if (!isAdapterEnabled) return"""
    
    content = re.sub(old_code, new_code, content)
    
    with open(path, "w") as f:
        f.write(content)

fix_file("app/src/main/java/com/example/meshchat/data/LoRaMeshService.kt", True)
fix_file("app/src/main/java/com/example/meshchat/data/RealBluetoothManager.kt", False)
