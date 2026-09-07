package com.example.meshchat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureSessionDao {
    @Query("SELECT * FROM secure_sessions WHERE isActive = 1 LIMIT 1")
    fun getActiveSessionFlow(): Flow<SecureSession?>

    @Query("SELECT * FROM secure_sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): SecureSession?

    @Query("SELECT * FROM secure_sessions ORDER BY lastRotatedAt DESC")
    fun getAllSessionsFlow(): Flow<List<SecureSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: SecureSession)

    @Update
    suspend fun update(session: SecureSession)

    @Query("UPDATE secure_sessions SET isActive = 0")
    suspend fun deactivateAllSessions()

    @Query("UPDATE secure_sessions SET isActive = 1 WHERE sessionId = :sessionId")
    suspend fun setActiveSession(sessionId: String)

    @Query("UPDATE secure_sessions SET securityLevel = :level, lastRotatedAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun updateSecurityLevel(sessionId: String, level: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE secure_sessions SET isQsvmActive = :isActive WHERE sessionId = :sessionId")
    suspend fun updateQsvmActive(sessionId: String, isActive: Boolean)

    @Query("UPDATE secure_sessions SET securityEngineMode = :mode, termuxEndpoint = :endpoint, termuxSelectedModel = :model WHERE sessionId = :sessionId")
    suspend fun updateQsvmConfig(sessionId: String, mode: String, endpoint: String, model: String)

    @Query("UPDATE secure_sessions SET isQsvmReady = :downloaded, isQsvmActive = :isActive WHERE sessionId = :sessionId")
    suspend fun updateQsvmReadyStatus(sessionId: String, downloaded: Boolean, isActive: Boolean)

    @Query("UPDATE secure_sessions SET isSetupCompleted = :completed WHERE sessionId = :sessionId")
    suspend fun updateSetupCompleted(sessionId: String, completed: Boolean)

    @Query("UPDATE secure_sessions SET isDeveloperMode = :enabled WHERE sessionId = :sessionId")
    suspend fun updateDeveloperMode(sessionId: String, enabled: Boolean)


    @Query("UPDATE secure_sessions SET selectedModelName = :modelName, securityEngineMode = :mode, isQsvmActive = :isActive, isQsvmReady = :isDownloaded WHERE sessionId = :sessionId")
    suspend fun updateModelSelection(sessionId: String, modelName: String, mode: String, isActive: Boolean, isDownloaded: Boolean)

    @Query("UPDATE secure_sessions SET sessionKey = :newKey, ratchetCounter = ratchetCounter + 1, lastRotatedAt = :timestamp WHERE sessionId = :sessionId")
    suspend fun ratchetKey(sessionId: String, newKey: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE secure_sessions SET userCallsign = :callsign WHERE sessionId = :sessionId")
    suspend fun updateUserCallsign(sessionId: String, callsign: String)

    @Query("UPDATE secure_sessions SET userCallsign = :callsign, nodeId = :nodeId WHERE sessionId = :sessionId")
    suspend fun updateNodeIdentity(sessionId: String, callsign: String, nodeId: String)

    @Query("DELETE FROM secure_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)
}
