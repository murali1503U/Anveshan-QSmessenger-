import re

with open("app/build.gradle.kts", "r") as f:
    content = f.read()

if "libs.okhttp" not in content:
    content = content.replace("implementation(libs.retrofit)", "implementation(libs.retrofit)\n    implementation(libs.okhttp)")

with open("app/build.gradle.kts", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/meshchat/ai/GeminiService.kt", "r") as f:
    content = f.read()
if "import okhttp3.MediaType.Companion.toMediaType" not in content:
    content = content.replace("import kotlinx.serialization.json.Json", "import kotlinx.serialization.json.Json\nimport okhttp3.MediaType.Companion.toMediaType")
if "import com.example.meshchat.BuildConfig" not in content:
    content = content.replace("package com.example.meshchat.ai", "package com.example.meshchat.ai\nimport com.example.meshchat.BuildConfig")

with open("app/src/main/java/com/example/meshchat/ai/GeminiService.kt", "w") as f:
    f.write(content)
    
with open("app/src/main/java/com/example/meshchat/data/MeshNotificationManager.kt", "r") as f:
    content = f.read()
if "import com.example.meshchat.R" not in content:
    content = content.replace("package com.example.meshchat.data", "package com.example.meshchat.data\nimport com.example.meshchat.R")
with open("app/src/main/java/com/example/meshchat/data/MeshNotificationManager.kt", "w") as f:
    f.write(content)
