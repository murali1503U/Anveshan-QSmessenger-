package com.example.meshchat.plugin

import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList

/**
 * P0 Security: Tamper-evident hash chain for all plugin actions.
 * Provides forensic logging of plugin activity.
 */
object PluginAudit {
    private val chain = CopyOnWriteArrayList<AuditEntry>()
    private var prevHash = "00000000000000000000000000000000"

    data class AuditEntry(
        val pluginId: String,
        val action: String,
        val timestamp: Long,
        val prevHash: String,
        val hash: String
    )

    @Synchronized
    fun log(pluginId: String, action: String) {
        val timestamp = System.currentTimeMillis()
        val data = "$prevHash$pluginId$action$timestamp"
        val hash = hashData(data)
        val entry = AuditEntry(pluginId, action, timestamp, prevHash, hash)
        prevHash = hash
        chain.add(entry)
    }

    private fun hashData(data: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun verifyChain(): Boolean {
        var currentPrev = "00000000000000000000000000000000"
        for (entry in chain) {
            if (entry.prevHash != currentPrev) return false
            val data = "${entry.prevHash}${entry.pluginId}${entry.action}${entry.timestamp}"
            if (hashData(data) != entry.hash) return false
            currentPrev = entry.hash
        }
        return true
    }
    
    fun getChainSize() = chain.size
}
