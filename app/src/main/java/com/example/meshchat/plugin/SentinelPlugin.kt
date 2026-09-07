package com.example.meshchat.plugin

import androidx.compose.runtime.Composable
import com.example.meshchat.data.Message

class PluginContext

interface SentinelPlugin {
    val id: String
    val name: String
    val version: String
    
    fun onInstall(context: PluginContext) {}
    fun onUninstall() {}
    fun onEnable() {}
    fun onDisable() {}
    
    fun provideMessageProcessor(): MessageProcessor? = null
    fun provideUIComponents(): List<UIComponent>? = null
    fun provideSecurityProvider(): SecurityProvider? = null
    fun provideTransport(): TransportProvider? = null
}

interface MessageProcessor {
    fun beforeSend(message: Message): Message
    fun afterReceive(message: Message): Message
}

interface UIComponent {
    @Composable
    fun render()
    fun onAction(action: String, data: Any?) {}
}

interface SecurityProvider {
    fun encrypt(data: ByteArray): ByteArray
    fun decrypt(data: ByteArray): ByteArray
}

interface TransportProvider {
    fun send(data: ByteArray): Boolean
    fun receive(): ByteArray?
}
