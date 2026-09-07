import re

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "r") as f:
    content = f.read()

content = re.sub(r'val finalMsg = PluginManager\.getActiveProcessors\(\)\.fold\(msg\) \{ acc, processor ->\s+processor\.afterReceive\(acc\)\s+\}', 'val finalMsg = msg // TODO PluginManager.processIncoming(msg)', content)
content = re.sub(r'var finalMsg = newMessage\s+PluginManager\.getActiveProcessors\(\)\.forEach \{ processor ->\s+finalMsg = processor\.beforeSend\(finalMsg\)\s+\}', 'var finalMsg = PluginManager.processOutgoing(newMessage)', content)

with open("app/src/main/java/com/example/meshchat/ui/ChatViewModel.kt", "w") as f:
    f.write(content)
