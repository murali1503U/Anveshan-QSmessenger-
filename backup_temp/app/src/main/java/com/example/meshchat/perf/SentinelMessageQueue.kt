package com.example.meshchat.perf

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/**
 * Ultra-low latency message queue using Kotlin Coroutine Channels.
 * Replaces LMAX Disruptor for Android API 24 compatibility while maintaining 
 * non-blocking, lock-free performance characteristics.
 */
class SentinelMessageQueue(
    private val scope: CoroutineScope,
    private val onMessage: (String) -> Unit
) {
    // UNLIMITED capacity channel behaves like an unbounded fast queue
    private val channel = Channel<String>(Channel.UNLIMITED)
    
    init {
        scope.launch(Dispatchers.Default) {
            for (message in channel) {
                onMessage(message)
            }
        }
    }
    
    fun publish(message: String) {
        channel.trySend(message)
    }
    
    fun shutdown() {
        channel.close()
    }
}
