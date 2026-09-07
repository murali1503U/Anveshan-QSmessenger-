import re

with open("app/src/main/java/com/example/meshchat/plugin/SentinelPlugin.kt", "r") as f:
    content = f.read()

content = content.replace("    var isEnabled: Boolean\n", "")

with open("app/src/main/java/com/example/meshchat/plugin/SentinelPlugin.kt", "w") as f:
    f.write(content)


with open("app/src/main/java/com/example/meshchat/plugin/PluginManager.kt", "r") as f:
    content = f.read()

content = content.replace("val plugin = entry.plugin", "val plugin = entry.plugin\n        val isEnabled = isPluginActive(id)")
content = content.replace("if (plugin.isEnabled)", "if (isEnabled)")
content = content.replace("plugin.isEnabled = true", "")
content = content.replace("plugin.isEnabled = false", "")

with open("app/src/main/java/com/example/meshchat/plugin/PluginManager.kt", "w") as f:
    f.write(content)
