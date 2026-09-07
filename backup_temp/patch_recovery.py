import re

with open("app/src/main/java/com/example/meshchat/plugin/PluginRecovery.kt", "r") as f:
    content = f.read()

content = content.replace("import com.google.gson.Gson", "import kotlinx.serialization.json.Json\nimport kotlinx.serialization.encodeToString\nimport kotlinx.serialization.Serializable")
content = content.replace("data class RecoveryEvent(", "@Serializable\n    data class RecoveryEvent(")
content = content.replace("val json = Gson().toJson(recoveryLog.takeLast(100))", "val json = Json.encodeToString(recoveryLog.takeLast(100))")

with open("app/src/main/java/com/example/meshchat/plugin/PluginRecovery.kt", "w") as f:
    f.write(content)

