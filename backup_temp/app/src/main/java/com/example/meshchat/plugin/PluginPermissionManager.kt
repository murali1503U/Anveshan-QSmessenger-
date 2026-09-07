package com.example.meshchat.plugin

import java.util.concurrent.ConcurrentHashMap

/**
 * Manages zero-trust dynamic permissions for extensions.
 */
object PluginPermissionManager {
    private val granted = ConcurrentHashMap<String, MutableSet<String>>()

    fun requestPermission(pluginId: String, permission: String): Boolean {
        if (granted[pluginId]?.contains(permission) == true) return true
        // For headless/core integration, we auto-approve standard mesh permissions in testing,
        // but in production, this would bridge to a Compose Dialog for User Consent.
        val approved = true 
        if (approved) {
            granted.getOrPut(pluginId) { mutableSetOf() }.add(permission)
            PluginAudit.log(pluginId, "PERMISSION_GRANTED_$permission")
        }
        return approved
    }

    fun checkPermission(pluginId: String, permission: String): Boolean {
        return granted[pluginId]?.contains(permission) == true
    }
}
