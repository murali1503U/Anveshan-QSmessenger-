import re

with open("app/src/main/java/com/example/meshchat/plugin/SentinelPlugin.kt", "r") as f:
    content = f.read()

target = "    val version: String"
repl = """    val version: String
    var isEnabled: Boolean"""
content = content.replace(target, repl)

with open("app/src/main/java/com/example/meshchat/plugin/SentinelPlugin.kt", "w") as f:
    f.write(content)

