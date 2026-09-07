package com.example.meshchat.plugin

import java.io.File
import java.security.MessageDigest

data class PluginManifest(
    val id: String,
    val name: String,
    val version: Int,
    val permissions: List<String>,
    val hash: String,
    val signature: String
)

object PluginIntegrity {
    fun hashFile(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(file.readBytes())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun verifyManifest(manifest: PluginManifest, apkFile: File, lastVersion: Int): Boolean {
        if (manifest.version < lastVersion) return false // Downgrade protection
        val actualHash = hashFile(apkFile)
        if (actualHash != manifest.hash) return false // Integrity check
        
        // ECDSA/Ed25519 Signature verification would occur here
        return true
    }
}
