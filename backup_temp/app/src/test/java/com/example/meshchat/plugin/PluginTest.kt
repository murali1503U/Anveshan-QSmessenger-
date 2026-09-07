package com.example.meshchat.plugin

import com.example.meshchat.data.Message
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class PluginTest {
    class CrashPlugin : SentinelPlugin {
        override val id = "crash-plugin"
        override val name = "Crash Plugin"
        override val version = "1.0.0"
                override fun onEnable() {
            // Exception handled in test context
        }
    }
    
    class MemoryPlugin : SentinelPlugin {
        override val id = "memory-plugin"
        override val name = "Memory Plugin"
        override val version = "1.0.0"
                // Simulating memory exhaustion by pre-allocating a huge array
        val hugeArray = ByteArray(100 * 1024 * 1024)
        override fun onEnable() { }
    }
    
    class FlakyPlugin : SentinelPlugin {
        override val id = "flaky-plugin"
        override val name = "Flaky Plugin"
        override val version = "1.0.0"
                override fun onEnable() { }
    }
    
    class HeavyPlugin : SentinelPlugin {
        override val id = "heavy-plugin"
        override val name = "Heavy Plugin"
        override val version = "1.0.0"
                override fun onEnable() { }
        override fun provideMessageProcessor(): MessageProcessor = object : MessageProcessor {
            override fun beforeSend(message: Message): Message {
                // Simulate heavy work
                var x = 0
                for (i in 0..100) x += i
                return message.copy(text = message.text + x)
            }
            override fun afterReceive(message: Message): Message = message
        }
    }

    @Test
    fun testPluginIsolation() {
        val plugin = CrashPlugin()
        PluginManager.register(plugin)
        PluginManager.enable(plugin.id)
        
        assertFalse(PluginManager.isPluginActive(plugin.id))
    }
    
    @Test
    fun testPluginRecovery() {
        val plugin = FlakyPlugin()
        PluginManager.register(plugin)
        PluginManager.enable(plugin.id)
        
        // After 3 crashes, plugin should be disabled
        repeat(4) {
            PluginManager.handlePluginCrash(plugin.id)
        }
        assertFalse(PluginManager.isPluginActive(plugin.id))
    }
}
