import re

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = re.sub(r'val processors = com\.example\.meshchat\.plugin\.PluginManager\.getActiveProcessors\(\)\s*processors\.forEach \{ processor ->\s*incoming = processor\.afterReceive\(incoming\)\s*\}', 'incoming = com.example.meshchat.plugin.PluginManager.processIncoming(incoming)', content)

content = re.sub(r'var finalMsg = newMessage\s*val processors = com\.example\.meshchat\.plugin\.PluginManager\.getActiveProcessors\(\)\s*processors\.forEach \{ processor ->\s*finalMsg = processor\.beforeSend\(finalMsg\)\s*\}', 'var finalMsg = com.example.meshchat.plugin.PluginManager.processOutgoing(newMessage)', content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
