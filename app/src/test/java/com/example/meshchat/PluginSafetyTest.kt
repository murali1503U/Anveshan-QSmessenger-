package com.example.meshchat

import com.example.meshchat.data.Message
import com.example.meshchat.plugin.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PluginSafetyTest {

    private class CrashyPlugin : SentinelPlugin {
        override val id = "crashy_plugin"
        override val name = "Crashy Plugin"
        override val version = "1.0"

        override fun onInstall(context: PluginContext) {}
        override fun onEnable() {
            throw RuntimeException("Explosion on enable")
        }
        override fun onDisable() {}
        override fun onUninstall() {}

        override fun provideMessageProcessor(): MessageProcessor = object : MessageProcessor {
            override fun beforeSend(message: Message): Message {
                throw IllegalStateException("Crashing in beforeSend")
            }
            override fun afterReceive(message: Message): Message {
                throw ArithmeticException("Crashing in afterReceive")
            }
        }
    }

    @Test
    fun testCrashingPluginDoesNotCrashHostApp() {
        val plugin = CrashyPlugin()
        PluginManager.register(plugin)

        // Attempt enable which throws
        val enableSuccess = PluginManager.enable(plugin.id)
        assertFalse("Enabling a crashing plugin must return false safely without crashing host", enableSuccess)
    }

    @Test
    fun testCrashingMessageProcessorPreservesMessage() {
        val plugin = CrashyPlugin()
        PluginManager.register(plugin)
        val entry = PluginManager.getPluginEntry(plugin.id)
        assertNotNull(entry)

        // Process message through plugin pipeline
        val originalMsg = Message(channelId = "c1", text = "Safe Message", sender = "Alice")
        val processed = PluginManager.processOutgoing(originalMsg)

        // Even though plugin throws, host must catch error and return original uncorrupted message!
        assertEquals("Host must return uncorrupted message on plugin failure", originalMsg.text, processed.text)
    }

    @Test
    fun testPluginAutoDisablesAfterRepeatedCrashes() {
        val plugin = CrashyPlugin()
        PluginManager.register(plugin)
        val entry = PluginManager.getPluginEntry(plugin.id)
        assertNotNull(entry)

        // Trigger 3 crashes
        PluginManager.handlePluginCrash(plugin.id)
        PluginManager.handlePluginCrash(plugin.id)
        PluginManager.handlePluginCrash(plugin.id)

        assertFalse("Plugin must be disabled after 3 crashes", PluginManager.isPluginActive(plugin.id))
        assertTrue("Crash count must be >= 3", entry!!.crashCount >= 3)
    }
}
