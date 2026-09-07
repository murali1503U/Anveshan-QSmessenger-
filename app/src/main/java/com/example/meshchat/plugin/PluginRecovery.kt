package com.example.meshchat.plugin

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.Serializable
import java.util.concurrent.CopyOnWriteArrayList

object PluginRecovery {
    private val recoveryLog = CopyOnWriteArrayList<RecoveryEvent>()
    
    @Serializable
    data class RecoveryEvent(
        val pluginId: String,
        val timestamp: Long,
        val eventType: RecoveryType,
        val success: Boolean
    )
    
    enum class RecoveryType {
        CRASH_RECOVERY,
        MEMORY_RECOVERY,
        TIMEOUT_RECOVERY,
        MANUAL_RECOVERY
    }
    
    fun attemptRecovery(pluginId: String, context: Context): Boolean {
        val entry = PluginManager.getPluginEntry(pluginId) ?: return false
        
        return when {
            entry.crashCount >= 3 -> {
                logRecovery(pluginId, RecoveryType.CRASH_RECOVERY, false, context)
                false
            }
            entry.container.checkMemory() -> {
                PluginManager.disable(pluginId)
                Thread.sleep(1000)
                PluginManager.enable(pluginId)
                logRecovery(pluginId, RecoveryType.MEMORY_RECOVERY, true, context)
                true
            }
            else -> {
                logRecovery(pluginId, RecoveryType.TIMEOUT_RECOVERY, false, context)
                false
            }
        }
    }
    
    private fun logRecovery(pluginId: String, type: RecoveryType, success: Boolean, context: Context) {
        recoveryLog.add(RecoveryEvent(pluginId, System.currentTimeMillis(), type, success))
        
        val prefs = context.getSharedPreferences("recovery_log", Context.MODE_PRIVATE)
        val json = Json.encodeToString(recoveryLog.takeLast(100))
        prefs.edit().putString("recovery_log", json).apply()
    }
}
