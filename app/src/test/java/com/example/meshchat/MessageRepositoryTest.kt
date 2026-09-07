package com.example.meshchat

import com.example.meshchat.data.Message
import com.example.meshchat.data.MessageDao
import com.example.meshchat.data.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MessageRepositoryTest {

    private class MockMessageDao : MessageDao {
        private val messageList = mutableListOf<Message>()
        private var autoId = 1

        override fun getMessagesByDemoFlow(isDemo: Boolean): Flow<List<Message>> =
            flowOf(messageList.filter { it.isDemo == isDemo })

        override fun getAllMessages(): Flow<List<Message>> = flowOf(messageList)

        override fun getMessagesForChannel(channelId: String): Flow<List<Message>> =
            flowOf(messageList.filter { it.channelId == channelId })

        override fun getMessagesForChannelAndDemo(channelId: String, isDemo: Boolean): Flow<List<Message>> =
            flowOf(messageList.filter { it.channelId == channelId && it.isDemo == isDemo })

        override suspend fun getLatestMessageForChannel(channelId: String): Message? =
            messageList.filter { it.channelId == channelId }.maxByOrNull { it.timestamp }

        override fun getRecentMessagesForChannel(channelId: String, limit: Int): Flow<List<Message>> =
            flowOf(messageList.filter { it.channelId == channelId }.takeLast(limit))

        override fun getRecentMessagesForChannelAndDemo(channelId: String, isDemo: Boolean, limit: Int): Flow<List<Message>> =
            flowOf(messageList.filter { it.channelId == channelId && it.isDemo == isDemo }.takeLast(limit))

        override suspend fun insertMessage(message: Message) {
            val assignedId = if (message.id == 0) autoId++ else message.id
            messageList.removeAll { it.id == assignedId }
            messageList.add(message.copy(id = assignedId))
        }

        override suspend fun deleteMessageById(id: Int) {
            messageList.removeAll { it.id == id }
        }

        override suspend fun clearMessagesForChannel(channelId: String) {
            messageList.removeAll { it.channelId == channelId }
        }

        override suspend fun clearMessagesByDemo(isDemo: Boolean) {
            messageList.removeAll { it.isDemo == isDemo }
        }

        override suspend fun clearAllMessages() {
            messageList.clear()
        }
    }

    private lateinit var dao: MockMessageDao
    private lateinit var repository: MessageRepository

    @Before
    fun setup() {
        dao = MockMessageDao()
        repository = MessageRepository(dao)
    }

    @Test
    fun testInsertAndRetrieveMessage() = runBlocking {
        val msg = Message(channelId = "general", text = "Unit test message", sender = "Alice")
        repository.insert(msg)

        val retrieved = repository.getMessagesForChannel("general").first()
        assertEquals(1, retrieved.size)
        assertEquals("Unit test message", retrieved[0].text)
        assertEquals("Alice", retrieved[0].sender)
    }

    @Test
    fun testClearChannelMessages() = runBlocking {
        repository.insert(Message(channelId = "c1", text = "Message 1", sender = "Bob"))
        repository.insert(Message(channelId = "c2", text = "Message 2", sender = "Charlie"))

        repository.clearChannelMessages("c1")

        val c1Messages = repository.getMessagesForChannel("c1").first()
        val c2Messages = repository.getMessagesForChannel("c2").first()

        assertEquals(0, c1Messages.size)
        assertEquals(1, c2Messages.size)
    }

    @Test
    fun testGetLatestMessage() = runBlocking {
        repository.insert(Message(channelId = "c1", text = "Old Msg", sender = "Alice", timestamp = 1000L))
        repository.insert(Message(channelId = "c1", text = "New Msg", sender = "Alice", timestamp = 2000L))

        val latest = repository.getLatestMessage("c1")
        assertNotNull(latest)
        assertEquals("New Msg", latest?.text)
    }
}
