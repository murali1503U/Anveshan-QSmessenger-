import re

with open("app/src/main/java/com/example/meshchat/plugin/PluginManager.kt", "r") as f:
    content = f.read()

# Add logging to processIncoming
content = content.replace(
    "processor.afterReceive(result)",
    """PluginAudit.log(id, "PROCESS_INCOMING")
                            processor.afterReceive(result)"""
)

# Add logging to processOutgoing
content = content.replace(
    "processor.beforeSend(result)",
    """PluginAudit.log(id, "PROCESS_OUTGOING")
                            processor.beforeSend(result)"""
)

with open("app/src/main/java/com/example/meshchat/plugin/PluginManager.kt", "w") as f:
    f.write(content)
