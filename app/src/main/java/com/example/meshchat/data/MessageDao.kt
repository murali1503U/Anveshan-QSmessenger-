package com.example.meshchat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE isDemo = :isDemo ORDER BY timestamp ASC")
    fun getMessagesByDemoFlow(isDemo: Boolean): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp ASC")
    fun getMessagesForChannel(channelId: String): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId AND isDemo = :isDemo ORDER BY timestamp ASC")
    fun getMessagesForChannelAndDemo(channelId: String, isDemo: Boolean): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForChannel(channelId: String): Message?

    @Query("SELECT * FROM messages WHERE channelId = :channelId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessagesForChannel(channelId: String, limit: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE channelId = :channelId AND isDemo = :isDemo ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessagesForChannelAndDemo(channelId: String, isDemo: Boolean, limit: Int): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM messages WHERE channelId = :channelId")
    suspend fun clearMessagesForChannel(channelId: String)

    @Query("DELETE FROM messages WHERE isDemo = :isDemo")
    suspend fun clearMessagesByDemo(isDemo: Boolean)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
