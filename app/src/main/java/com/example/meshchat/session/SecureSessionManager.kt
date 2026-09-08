package com.example.meshchat.session

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureSessionManager(
    private val sessionDao: SessionDao,
    private val keyStorageManager: KeyStorageManager
) {
    companion object {
        const val TIMEOUT_MS = 24 * 60 * 60 * 1000L // 24 hours
        const val ROTATION_MESSAGES = 100
        const val ROTATION_TIME_MS = 60 * 60 * 1000L // 1 hour
    }

    suspend fun createSession(peerCallsign: String, securityLevel: Int): SessionEntity = withContext(Dispatchers.IO) {
        val session = SessionEntity(
            peerCallsign = peerCallsign,
            securityLevel = securityLevel,
            mlKemPublicKey = null,
            mlDsaPublicKey = null,
            encryptedSessionKeyId = null
        )
        sessionDao.insertSession(session)
        session
    }

    suspend fun verifyPsk(sessionId: String, pskHash: ByteArray): Boolean = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession(sessionId) ?: return@withContext false
        val storedHash = keyStorageManager.getPskHash()
        if (storedHash != null && storedHash.contentEquals(pskHash)) {
            session.state = SessionState.PSK_VERIFIED
            session.lastUsedAt = System.currentTimeMillis()
            sessionDao.updateSession(session)
            return@withContext true
        }
        false
    }

    suspend fun completeMlKem(sessionId: String, peerMlKemPub: ByteArray, peerMlDsaPub: ByteArray, sessionKey: ByteArray) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession(sessionId) ?: throw IllegalArgumentException("Session not found")
        
        keyStorageManager.saveSessionKey(sessionId, sessionKey)
        
        session.mlKemPublicKey = Base64.encodeToString(peerMlKemPub, Base64.NO_WRAP)
        session.mlDsaPublicKey = Base64.encodeToString(peerMlDsaPub, Base64.NO_WRAP)
        session.encryptedSessionKeyId = sessionId
        session.state = SessionState.ACTIVE
        session.lastUsedAt = System.currentTimeMillis()
        
        sessionDao.updateSession(session)
    }

    suspend fun checkAndRotateKeyIfNeeded(sessionId: String, newSessionKey: ByteArray? = null): Boolean = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession(sessionId) ?: return@withContext false
        
        val timeSinceLastUsed = System.currentTimeMillis() - session.lastUsedAt
        val needsRotation = session.messageCount >= ROTATION_MESSAGES || timeSinceLastUsed >= ROTATION_TIME_MS
        
        if (needsRotation && newSessionKey != null) {
            keyStorageManager.saveSessionKey(sessionId, newSessionKey)
            session.messageCount = 0
            session.lastUsedAt = System.currentTimeMillis()
            sessionDao.updateSession(session)
            return@withContext true
        }
        false
    }

    suspend fun processMessage(sessionId: String) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSession(sessionId) ?: return@withContext
        
        if (isSessionExpired(session)) {
            session.state = SessionState.EXPIRED
            session.isExpired = true
            sessionDao.updateSession(session)
            throw IllegalStateException("Session expired, re-handshake required")
        }
        
        session.messageCount++
        session.sequenceCounter++
        session.lastUsedAt = System.currentTimeMillis()
        sessionDao.updateSession(session)
    }

    suspend fun getActiveSessions(): List<SessionEntity> = withContext(Dispatchers.IO) {
        val activeSessions = sessionDao.getActiveSessions()
        val validSessions = mutableListOf<SessionEntity>()
        
        for (session in activeSessions) {
            if (isSessionExpired(session)) {
                session.state = SessionState.EXPIRED
                session.isExpired = true
                sessionDao.updateSession(session)
            } else {
                validSessions.add(session)
            }
        }
        validSessions
    }
    
    private fun isSessionExpired(session: SessionEntity): Boolean {
        return (System.currentTimeMillis() - session.createdAt) > TIMEOUT_MS
    }
}
