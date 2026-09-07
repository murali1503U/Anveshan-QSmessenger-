package com.example.meshchat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatChannelDao {
    @Query("SELECT * FROM chat_channels WHERE isDemo = :isDemo ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getChannelsByDemoFlow(isDemo: Boolean): Flow<List<ChatChannel>>

    @Query("SELECT * FROM chat_channels ORDER BY isPinned DESC, lastMessageTimestamp DESC")
    fun getAllChannelsFlow(): Flow<List<ChatChannel>>

    @Query("SELECT * FROM chat_channels WHERE channelId = :channelId LIMIT 1")
    fun getChannelFlow(channelId: String): Flow<ChatChannel?>

    @Query("SELECT * FROM chat_channels WHERE channelId = :channelId LIMIT 1")
    suspend fun getChannel(channelId: String): ChatChannel?

    @Query("SELECT COUNT(*) FROM chat_channels WHERE isDemo = :isDemo")
    suspend fun getChannelCountByDemo(isDemo: Boolean): Int

    @Query("SELECT COUNT(*) FROM chat_channels")
    suspend fun getChannelCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(channel: ChatChannel)

    @Update
    suspend fun update(channel: ChatChannel)

    @Query("UPDATE chat_channels SET lastMessageText = :text, lastMessageTimestamp = :timestamp WHERE channelId = :channelId")
    suspend fun updateLastMessage(channelId: String, text: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE chat_channels SET unreadCount = 0 WHERE channelId = :channelId")
    suspend fun clearUnreadCount(channelId: String)

    @Query("UPDATE chat_channels SET unreadCount = unreadCount + 1 WHERE channelId = :channelId")
    suspend fun incrementUnreadCount(channelId: String)

    @Query("UPDATE chat_channels SET isPinned = :isPinned WHERE channelId = :channelId")
    suspend fun setPinned(channelId: String, isPinned: Boolean)

    @Query("DELETE FROM chat_channels WHERE channelId = :channelId")
    suspend fun deleteChannel(channelId: String)
}
