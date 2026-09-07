import re

with open("app/src/main/java/com/example/meshchat/plugin/PluginManager.kt", "r") as f:
    content = f.read()

target = "fun processOutgoing(message: Message): Message {"
repl = """fun processIncoming(message: Message): Message {
        var result = message
        for ((id, entry) in plugins) {
            if (activePlugins[id] == true) {
                val processor = entry.plugin.provideMessageProcessor()
                if (processor != null) {
                    runBlocking {
                        val processResult = entry.container.execute {
                            processor.afterReceive(result)
                        }
                        when (processResult) {
                            is PluginResult.Success -> result = processResult.value
                            else -> Log.e("PluginManager", "Plugin $id failed processing message")
                        }
                    }
                }
            }
        }
        return result
    }

    fun processOutgoing(message: Message): Message {"""

content = content.replace(target, repl)

with open("app/src/main/java/com/example/meshchat/plugin/PluginManager.kt", "w") as f:
    f.write(content)
