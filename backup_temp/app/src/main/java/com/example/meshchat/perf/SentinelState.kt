package com.example.meshchat.perf

import com.example.meshchat.data.PacketType
import com.example.meshchat.data.SentinelPacket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object SentinelState {
    // Priority queue for SOS, LinkedQueue for standard messages
    private val messageQueue = ConcurrentLinkedQueue<SentinelPacket>()
    
    // Lock-free map
    private val nodeMap = ConcurrentHashMap<String, Long>()
    
    // Atomic counters (no locks)
    private val packetCounter = AtomicLong(0)
    private val activeConnections = AtomicInteger(0)
    private val lastActivity = AtomicLong(System.currentTimeMillis())
    
    fun enqueue(packet: SentinelPacket) {
        messageQueue.offer(packet)
        packetCounter.incrementAndGet()
        lastActivity.set(System.currentTimeMillis())
    }
    
    fun dequeue(): SentinelPacket? = messageQueue.poll()
    
    fun dequeueBatch(maxSize: Int = 16): List<SentinelPacket> {
        val result = mutableListOf<SentinelPacket>()
        repeat(maxSize) {
            dequeue()?.let { result.add(it) }
        }
        return result
    }
    
    fun addNode(id: String, lastSeen: Long) {
        nodeMap[id] = lastSeen
        activeConnections.incrementAndGet()
    }
    
    fun removeNode(id: String) {
        nodeMap.remove(id)
        activeConnections.decrementAndGet()
    }
}
