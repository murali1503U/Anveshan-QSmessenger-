package com.example.meshchat.plugin

import java.util.concurrent.ConcurrentHashMap

object PluginSecurity {
    private val permissionRegistry = ConcurrentHashMap<String, Set<String>>()
    
    enum class Permission {
        ACCESS_BLUETOOTH,
        ACCESS_GPS,
        ACCESS_FILES,
        ACCESS_NETWORK,
        SEND_MESSAGES,
        RECEIVE_MESSAGES,
        ACCESS_CAMERA,
        ACCESS_MICROPHONE
    }
    
    fun requestPermission(pluginId: String, permission: Permission): Boolean {
        val permissions = permissionRegistry.getOrDefault(pluginId, emptySet())
        return permissions.contains(permission.name)
    }
    
    fun grantPermission(pluginId: String, permission: Permission) {
        permissionRegistry.compute(pluginId) { _, perms ->
            (perms ?: emptySet()) + permission.name
        }
    }
    
    fun revokePermission(pluginId: String, permission: Permission) {
        permissionRegistry.compute(pluginId) { _, perms ->
            (perms ?: emptySet()) - permission.name
        }
    }
}
