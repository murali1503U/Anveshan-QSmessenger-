package com.example.meshchat

import com.example.meshchat.session.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeSessionDao : SessionDao {
    private val sessions = mutableMapOf<String, SessionEntity>()

    override suspend fun insertSession(session: SessionEntity) {
        sessions[session.sessionId] = session.copy()
    }

    override suspend fun updateSession(session: SessionEntity) {
        sessions[session.sessionId] = session.copy()
    }

    override suspend fun getSession(sessionId: String): SessionEntity? {
        return sessions[sessionId]?.copy()
    }

    override suspend fun getActiveSessions(): List<SessionEntity> {
        return sessions.values.toList().map { it.copy() }
    }

    override suspend fun deleteSession(sessionId: String) {
        sessions.remove(sessionId)
    }
}

class FakeKeyStorageManager : KeyStorageManager {
    val savedKeys = mutableMapOf<String, ByteArray>()
    var _pskHash: ByteArray? = null

    override fun saveMlKemPrivateKey(key: ByteArray) {}
    override fun getMlKemPrivateKey(): ByteArray? = null
    override fun saveMlDsaPrivateKey(key: ByteArray) {}
    override fun getMlDsaPrivateKey(): ByteArray? = null

    override fun saveSessionKey(sessionId: String, key: ByteArray) {
        savedKeys[sessionId] = key
    }

    override fun getSessionKey(sessionId: String): ByteArray? {
        return savedKeys[sessionId]
    }

    override fun savePskHash(hash: ByteArray) {
        _pskHash = hash
    }

    override fun getPskHash(): ByteArray? {
        return _pskHash
    }

    override fun getKeyFingerprint(key: ByteArray): String = ""
}

class SecureSessionManagerTest {

    private lateinit var sessionDao: FakeSessionDao
    private lateinit var keyStorageManager: FakeKeyStorageManager
    private lateinit var secureSessionManager: SecureSessionManager

    @Before
    fun setup() {
        sessionDao = FakeSessionDao()
        keyStorageManager = FakeKeyStorageManager()
        secureSessionManager = SecureSessionManager(sessionDao, keyStorageManager)
    }

    @Test
    fun testSessionLifecycle() = runBlocking {
        val sessionId = "test-session-123"
        val pskHash = byteArrayOf(1, 2, 3)
        keyStorageManager.savePskHash(pskHash)
        
        val session = SessionEntity(
            sessionId = sessionId,
            peerCallsign = "PEER1",
            securityLevel = 1,
            state = SessionState.CREATED
        )
        sessionDao.insertSession(session)
        
        val verified = secureSessionManager.verifyPsk(sessionId, pskHash)
        assertTrue(verified)
        
        val updatedSession1 = sessionDao.getSession(sessionId)!!
        assertEquals(SessionState.PSK_VERIFIED, updatedSession1.state)

        secureSessionManager.completeMlKem(
            sessionId, 
            ByteArray(2400), 
            ByteArray(4032), 
            byteArrayOf(9, 9, 9)
        )
        
        val updatedSession2 = sessionDao.getSession(sessionId)!!
        assertEquals(SessionState.ACTIVE, updatedSession2.state)
        assertArrayEquals(byteArrayOf(9, 9, 9), keyStorageManager.getSessionKey(sessionId))
    }

    @Test
    fun testKeyRotationAfter100Messages() = runBlocking {
        val sessionId = "test-session-456"
        val session = SessionEntity(
            sessionId = sessionId,
            peerCallsign = "PEER2",
            securityLevel = 1,
            messageCount = 100,
            state = SessionState.ACTIVE
        )
        sessionDao.insertSession(session)
        
        val rotated = secureSessionManager.checkAndRotateKeyIfNeeded(sessionId, byteArrayOf(8, 8, 8))
        assertTrue(rotated)
        
        val updatedSession = sessionDao.getSession(sessionId)!!
        assertEquals(0, updatedSession.messageCount)
        assertArrayEquals(byteArrayOf(8, 8, 8), keyStorageManager.getSessionKey(sessionId))
    }

    @Test
    fun testSessionTimeoutAfter24Hours() = runBlocking {
        val sessionId = "test-session-789"
        val oldTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago
        val session = SessionEntity(
            sessionId = sessionId,
            peerCallsign = "PEER3",
            securityLevel = 1,
            createdAt = oldTime,
            state = SessionState.ACTIVE
        )
        sessionDao.insertSession(session)
        
        try {
            secureSessionManager.processMessage(sessionId)
            fail("Should throw IllegalStateException")
        } catch (e: IllegalStateException) {
            val updatedSession = sessionDao.getSession(sessionId)!!
            assertEquals(SessionState.EXPIRED, updatedSession.state)
            assertTrue(updatedSession.isExpired)
        }
    }

    @Test
    fun testSessionRestore() = runBlocking {
        val validSession = SessionEntity(
            sessionId = "V1_ID",
            peerCallsign = "V1", 
            securityLevel = 1, 
            createdAt = System.currentTimeMillis(),
            state = SessionState.ACTIVE
        )
        val expiredSession = SessionEntity(
            sessionId = "E1_ID",
            peerCallsign = "E1", 
            securityLevel = 1, 
            createdAt = System.currentTimeMillis() - (25 * 60 * 60 * 1000L),
            state = SessionState.ACTIVE
        )
        
        sessionDao.insertSession(validSession)
        sessionDao.insertSession(expiredSession)
        
        val restored = secureSessionManager.getActiveSessions()
        assertEquals(1, restored.size)
        assertEquals("V1", restored[0].peerCallsign)
        
        val updatedExpired = sessionDao.getSession("E1_ID")!!
        assertTrue(updatedExpired.isExpired)
        assertEquals(SessionState.EXPIRED, updatedExpired.state)
    }
}
