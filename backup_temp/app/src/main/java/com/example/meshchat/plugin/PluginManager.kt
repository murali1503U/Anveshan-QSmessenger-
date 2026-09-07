package com.example.meshchat.plugin

import android.util.Log
import com.example.meshchat.data.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

object PluginManager {
    private val plugins = ConcurrentHashMap<String, PluginEntry>()
    private val activePlugins = ConcurrentHashMap<String, Boolean>()
    private val healthCheckScheduler = ScheduledThreadPoolExecutor(1)
    
    private val _activePluginsFlow = MutableStateFlow<Set<String>>(emptySet())
    val activePluginsFlow: StateFlow<Set<String>> = _activePluginsFlow.asStateFlow()
    
    data class PluginEntry(
        val plugin: SentinelPlugin,
        val container: PluginContainer,
        val installedAt: Long = System.currentTimeMillis(),
        var lastHealthCheck: Long = System.currentTimeMillis(),
        var crashCount: Int = 0
    )
    
    fun register(plugin: SentinelPlugin) {
        val container = PluginContainer(plugin)
        val entry = PluginEntry(plugin, container)
        plugins[plugin.id] = entry
        plugin.onInstall(PluginContext())
        
        if (healthCheckScheduler.poolSize == 1 && healthCheckScheduler.queue.isEmpty()) {
            healthCheckScheduler.scheduleAtFixedRate({
                checkAllHealth()
            }, 30, 30, TimeUnit.SECONDS)
        }
    }
    
    fun enable(id: String): Boolean {
        val entry = plugins[id] ?: return false
        val plugin = entry.plugin
        val isEnabled = isPluginActive(id)
        
        return if (isEnabled) {
            true
        } else {
            try {
                if (!entry.container.checkMemory()) {
                    return false
                }
                
                runBlocking {
                    val result = entry.container.execute {
                        plugin.onEnable()
                    }
                    if (result is PluginResult.Success) {
                        
                        activePlugins[id] = true
                        updateFlow()
                        true
                    } else false
                }
            } catch (e: Exception) {
                entry.crashCount++
                false
            }
        }
    }
    
    fun disable(id: String) {
        plugins[id]?.let { entry ->
            val plugin = entry.plugin
        val isEnabled = isPluginActive(id)
            runBlocking {
                entry.container.execute {
                    plugin.onDisable()
                }
            }
            
            activePlugins[id] = false
            updateFlow()
        }
    }
    
    fun uninstall(id: String) {
        disable(id)
        plugins[id]?.plugin?.onUninstall()
        plugins.remove(id)
        activePlugins.remove(id)
        updateFlow()
    }
    
    private fun checkAllHealth() {
        plugins.keys.forEach { checkPluginHealth(it) }
    }
    
    private fun checkPluginHealth(id: String) {
        val entry = plugins[id] ?: return
        val plugin = entry.plugin
        val isEnabled = isPluginActive(id)
        
        if (isEnabled) {
            if (!entry.container.checkMemory()) {
                disable(id)
                Log.e("PluginManager", "Plugin ${plugin.name} disabled: memory limit exceeded")
            }
            
            val start = System.currentTimeMillis()
            runBlocking {
                entry.container.execute {
                    // Health check empty block
                }
            }
            val duration = System.currentTimeMillis() - start
            if (duration > 500) {
                Log.w("PluginManager", "Plugin ${plugin.id} is slow: ${duration}ms")
            }
        }
    }
    
    fun handlePluginCrash(id: String) {
        val entry = plugins[id] ?: return
        entry.crashCount++
        
        if (entry.crashCount >= 3) {
            disable(id)
            Log.e("PluginManager", "Plugin ${entry.plugin.name} disabled due to multiple crashes")
        } else {
            enable(id)
        }
    }
    
    fun processIncoming(message: Message): Message {
        var result = message
        for ((id, entry) in plugins) {
            if (activePlugins[id] == true) {
                val processor = entry.plugin.provideMessageProcessor()
                if (processor != null) {
                    runBlocking {
                        val processResult = entry.container.execute {
                            PluginAudit.log(id, "PROCESS_INCOMING")
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

    fun processOutgoing(message: Message): Message {
        var result = message
        for ((id, entry) in plugins) {
            if (activePlugins[id] == true) {
                val processor = entry.plugin.provideMessageProcessor()
                if (processor != null) {
                    runBlocking {
                        val processResult = entry.container.execute {
                            PluginAudit.log(id, "PROCESS_OUTGOING")
                            processor.beforeSend(result)
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

    private fun updateFlow() {
        _activePluginsFlow.value = activePlugins.filterValues { it }.keys.toSet()
    }
    
    fun getAll(): List<SentinelPlugin> = plugins.values.map { it.plugin }
    fun getAllPlugins(): List<PluginEntry> = plugins.values.toList()
    fun getPluginEntry(id: String): PluginEntry? = plugins[id]
    fun isPluginActive(id: String): Boolean = activePlugins[id] == true
}
