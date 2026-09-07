import re

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

target = """            val processors = com.example.meshchat.plugin.PluginManager.getActiveProcessors()
            processors.forEach { processor ->
                message = processor.beforeSend(message)
            }"""
repl = """            message = com.example.meshchat.plugin.PluginManager.processOutgoing(message)"""

content = content.replace(target, repl)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
