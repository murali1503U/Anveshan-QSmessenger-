package com.example.meshchat.data

import kotlinx.coroutines.flow.Flow

class MessageRepository(private val messageDao: MessageDao) {
    val allMessages: Flow<List<Message>> = messageDao.getAllMessages()

    fun getMessagesByDemo(isDemo: Boolean): Flow<List<Message>> =
        messageDao.getMessagesByDemoFlow(isDemo)

    fun getMessagesForChannel(channelId: String): Flow<List<Message>> =
        messageDao.getMessagesForChannel(channelId)

    fun getMessagesForChannelAndDemo(channelId: String, isDemo: Boolean): Flow<List<Message>> =
        messageDao.getMessagesForChannelAndDemo(channelId, isDemo)

    fun getRecentChannelMessages(channelId: String, limit: Int = 10): Flow<List<Message>> =
        messageDao.getRecentMessagesForChannel(channelId, limit)

    fun getRecentChannelMessagesAndDemo(channelId: String, isDemo: Boolean, limit: Int = 10): Flow<List<Message>> =
        messageDao.getRecentMessagesForChannelAndDemo(channelId, isDemo, limit)

    suspend fun getLatestMessage(channelId: String): Message? =
        messageDao.getLatestMessageForChannel(channelId)

    suspend fun insert(message: Message) = messageDao.insertMessage(message)
    suspend fun deleteById(id: Int) = messageDao.deleteMessageById(id)
    suspend fun clearChannelMessages(channelId: String) = messageDao.clearMessagesForChannel(channelId)
    suspend fun clearMessagesByDemo(isDemo: Boolean) = messageDao.clearMessagesByDemo(isDemo)
    suspend fun clearAll() = messageDao.clearAllMessages()
}
