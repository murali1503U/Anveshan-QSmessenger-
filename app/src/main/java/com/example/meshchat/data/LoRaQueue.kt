package com.example.meshchat.data

import java.util.PriorityQueue

// LoRaQueue.kt — Priority queue for LoRa transmission
class LoRaQueue {
    // We wrap the byte array and priority
    data class LoRaPacketData(val data: ByteArray, var priority: Int)

    private val priorityQueue = PriorityQueue<LoRaPacketData> { a, b ->
        b.priority - a.priority  // Higher priority first
    }
    private val capacity = 256
    private var size = 0

    fun enqueue(data: ByteArray, priority: Int) {
        if (size < capacity) {
            priorityQueue.offer(LoRaPacketData(data, priority))
            size++
        }
    }

    fun dequeue(): LoRaPacketData? {
        val item = priorityQueue.poll()
        if (item != null) size--
        return item
    }

    fun hasPending(): Boolean = priorityQueue.isNotEmpty()
}
