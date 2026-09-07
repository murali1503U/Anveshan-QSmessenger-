import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Replace the single permission request with multiple permissions
old_code = r"""val notificationPermissionLauncher = rememberLauncherForActivityResult\(
                contract = ActivityResultContracts\.RequestPermission\(\)
            \) \{ /\* Permission granted or denied \*/ \}
            LaunchedEffect\(Unit\) \{
                if \(Build\.VERSION\.SDK_INT >= Build\.VERSION_CODES\.TIRAMISU\) \{
                    if \(ContextCompat\.checkSelfPermission\(context, Manifest\.permission\.POST_NOTIFICATIONS\) != PackageManager\.PERMISSION_GRANTED\) \{
                        notificationPermissionLauncher\.launch\(Manifest\.permission\.POST_NOTIFICATIONS\)
                    \}
                \}
            \}"""

new_code = """val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Permissions handled */ }
            LaunchedEffect(Unit) {
                val perms = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    perms.add(Manifest.permission.BLUETOOTH_SCAN)
                    perms.add(Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    perms.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    perms.add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                val toRequest = perms.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (toRequest.isNotEmpty()) {
                    permissionLauncher.launch(toRequest.toTypedArray())
                }
            }"""

content = re.sub(old_code, new_code, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
