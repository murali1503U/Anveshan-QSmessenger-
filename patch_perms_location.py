import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

old_code = r"""if \(Build\.VERSION\.SDK_INT >= Build\.VERSION_CODES\.S\) \{
                    perms\.add\(Manifest\.permission\.BLUETOOTH_SCAN\)
                    perms\.add\(Manifest\.permission\.BLUETOOTH_CONNECT\)
                \} else \{
                    perms\.add\(Manifest\.permission\.ACCESS_FINE_LOCATION\)
                    perms\.add\(Manifest\.permission\.ACCESS_COARSE_LOCATION\)
                \}"""

new_code = """if (Build.VERSION.SDK_INT >= Build.VERSION.CODES.S) {
                    perms.add(Manifest.permission.BLUETOOTH_SCAN)
                    perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                }
                perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)"""

content = re.sub(old_code, new_code, content)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
