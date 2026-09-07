package com.example.meshchat.data

import kotlinx.coroutines.flow.Flow
import java.security.SecureRandom

class SecureSessionRepository(private val secureSessionDao: SecureSessionDao) {
    val activeSession: Flow<SecureSession?> = secureSessionDao.getActiveSessionFlow()
    val allSessions: Flow<List<SecureSession>> = secureSessionDao.getAllSessionsFlow()

    suspend fun initializeDefaultSessionIfNeeded(): SecureSession {
        val existing = secureSessionDao.getActiveSession()
        if (existing != null) {
            return existing
        }
        val defaultSession = SecureSession(
            sessionId = "mesh_primary_session",
            userCallsign = generateUniqueCallsign(),
            nodeId = generateUniqueNodeId(),
            peerId = "global_mesh",
            peerName = "Broadcast Mesh",
            securityLevel = 0, // Auto AI Managed by default
            isQsvmActive = true,
            sessionKey = generateRandomHexKey(16),
            ratchetCounter = 0,
            pqSharedSecret = "kyber768_seed_" + generateRandomHexKey(8),
            isActive = true
        )
        secureSessionDao.insertOrUpdate(defaultSession)
        return defaultSession
    }

    suspend fun updateUserCallsign(sessionId: String, callsign: String) {
        secureSessionDao.updateUserCallsign(sessionId, callsign)
    }

    suspend fun updateNodeIdentity(sessionId: String, callsign: String, nodeId: String) {
        secureSessionDao.updateNodeIdentity(sessionId, callsign, nodeId)
    }

    fun generateUniqueCallsign(): String {
        val prefixes = listOf("SENTINEL", "PHANTOM", "VIPER", "ECHO", "GHOST", "APEX", "NOMAD", "CYBER", "DELTA", "ORBIT")
        val randomPrefix = prefixes.random()
        val randomHex = (1000..9999).random().toString(16).uppercase().padStart(4, '0')
        return "$randomPrefix-$randomHex"
    }

    fun generateUniqueNodeId(): String {
        val randomHex = (100000..999999).random().toString(16).uppercase()
        return "0x$randomHex"
    }

    suspend fun saveSession(session: SecureSession) {
        secureSessionDao.insertOrUpdate(session)
    }

    suspend fun setSecurityLevel(sessionId: String, level: Int) {
        secureSessionDao.updateSecurityLevel(sessionId, level)
    }

    suspend fun setQsvmActive(sessionId: String, isActive: Boolean) {
        secureSessionDao.updateQsvmActive(sessionId, isActive)
    }

    suspend fun setQsvmConfig(sessionId: String, mode: String, endpoint: String, model: String) {
        secureSessionDao.updateQsvmConfig(sessionId, mode, endpoint, model)
    }

    suspend fun setQsvmReady(sessionId: String, downloaded: Boolean, isActive: Boolean) {
        secureSessionDao.updateQsvmReadyStatus(sessionId, downloaded, isActive)
    }

    suspend fun setSetupCompleted(sessionId: String, completed: Boolean) {
        secureSessionDao.updateSetupCompleted(sessionId, completed)
    }

    suspend fun setDeveloperMode(sessionId: String, enabled: Boolean) {
        secureSessionDao.updateDeveloperMode(sessionId, enabled)
    }

    suspend fun setDemoSimulation(sessionId: String, enabled: Boolean) {
    }

    suspend fun updateModelSelection(sessionId: String, modelName: String, mode: String, isActive: Boolean, isDownloaded: Boolean) {
        secureSessionDao.updateModelSelection(sessionId, modelName, mode, isActive, isDownloaded)
    }

    suspend fun ratchetSessionKey(sessionId: String) {
        val newKey = generateRandomHexKey(16)
        secureSessionDao.ratchetKey(sessionId, newKey)
    }

    private fun generateRandomHexKey(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        SecureRandom().nextBytes(bytes)
        return "0x" + bytes.joinToString("") { "%02X".format(it) }
    }
}
