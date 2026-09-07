package com.example.meshchat.scripting

import android.util.LruCache
import com.example.meshchat.data.Message
import com.example.meshchat.plugin.MessageProcessor
import com.example.meshchat.plugin.PluginManager
import com.example.meshchat.plugin.SentinelPlugin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.Callable

class DynamicScriptPlugin(
    override val id: String,
    private val engine: ScriptEngine
) : SentinelPlugin {
    override val name = "Script: $id"
    override val version = "1.1"
    
    override fun provideMessageProcessor(): MessageProcessor {
        return object : MessageProcessor {
            override fun beforeSend(message: Message): Message {
                val processedText = executeSafely { engine.process(message.text) } ?: message.text
                return message.copy(text = processedText)
            }
            override fun afterReceive(message: Message): Message {
                val processedText = executeSafely { engine.process(message.text) } ?: message.text
                return message.copy(text = processedText)
            }
            
            // Execute in ForkJoinPool common pool for background performance
            private fun executeSafely(task: () -> String): String? {
                return try {
                    ForkJoinPool.commonPool().submit(Callable { task() }).get()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

object ScriptManager {
    // Extreme optimization: LRU Cache for precompiled scripts
    private val scriptCache = LruCache<String, Pair<ScriptType, ScriptEngine>>(20)
    private val scriptsSource = mutableMapOf<String, Pair<ScriptType, String>>()
    
    private val _loadedScripts = MutableStateFlow<List<String>>(emptyList())
    val loadedScripts: StateFlow<List<String>> = _loadedScripts.asStateFlow()
    
    fun load(name: String, source: String, type: ScriptType = ScriptType.LUA) {
        scriptsSource[name] = type to source
        compileAndRegister(name, source, type)
        _loadedScripts.value = scriptsSource.keys.toList()
    }
    
    private fun compileAndRegister(name: String, source: String, type: ScriptType) {
        val engine = when (type) {
            ScriptType.LUA -> LuaEngine()
            ScriptType.JAVA -> JaninoEngine()
        }
        
        try {
            engine.compile(source)
            scriptCache.put(name, type to engine)
            
            PluginManager.register(DynamicScriptPlugin(name, engine))
            PluginManager.enable(name)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun unload(name: String) {
        PluginManager.uninstall(name)
        scriptsSource.remove(name)
        scriptCache.remove(name)
        _loadedScripts.value = scriptsSource.keys.toList()
    }
    
    fun getPrebuiltScripts(): Map<String, Pair<ScriptType, String>> {
        return mapOf(
            "gps-tagger" to (ScriptType.LUA to """
                function processMessage(msg)
                    return msg .. " | 📍 [13.0827, 80.2707]"
                end
            """.trimIndent()),
            "auto-encrypt" to (ScriptType.LUA to """
                local sensitive = {"password", "bank", "pin", "sos"}
                function processMessage(msg)
                    local lowerMsg = string.lower(msg)
                    for _, word in ipairs(sensitive) do
                        if string.find(lowerMsg, word) then
                            return "🔐 " .. msg
                        end
                    end
                    return msg
                end
            """.trimIndent()),
            "sos-alert" to (ScriptType.LUA to """
                function processMessage(msg)
                    local lowerMsg = string.lower(msg)
                    if string.find(lowerMsg, "sos") or string.find(lowerMsg, "help") then
                        return "🚨 [EMERGENCY ALERT] 🚨 " .. msg
                    end
                    return msg
                end
            """.trimIndent()),
            "java-hello" to (ScriptType.JAVA to """
                public class Script {
                    public String processMessage(String msg) {
                        return "☕ Java Mod: " + msg;
                    }
                }
            """.trimIndent())
        )
    }
}
