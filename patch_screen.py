import re

with open("app/src/main/java/com/example/meshchat/ui/PluginScreen.kt", "r") as f:
    content = f.read()

content = content.replace("plugin.isEnabled", "PluginManager.isPluginActive(plugin.id)")

with open("app/src/main/java/com/example/meshchat/ui/PluginScreen.kt", "w") as f:
    f.write(content)


with open("app/src/test/java/com/example/meshchat/plugin/PluginTest.kt", "r") as f:
    content = f.read()

content = content.replace("override var isEnabled = false\n", "")

with open("app/src/test/java/com/example/meshchat/plugin/PluginTest.kt", "w") as f:
    f.write(content)

