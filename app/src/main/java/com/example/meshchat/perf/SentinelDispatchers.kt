package com.example.meshchat.perf

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

object SentinelDispatchers {
    private val coreCount = Runtime.getRuntime().availableProcessors()
    
    // UI thread (Compose)
    val Main: CoroutineDispatcher = Dispatchers.Main
    
    // I/O dispatcher with auto-scaling (2-8 threads)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val IO: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(
        parallelism = (coreCount * 2).coerceIn(2, 8)
    )
    
    // CPU dispatcher with parallelism = cores
    val Default: CoroutineDispatcher = Dispatchers.Default
    
    // Dedicated Hardware/Crypto dispatcher (High Priority)
    val Crypto: CoroutineDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "Sentinel-Crypto").apply {
            priority = Thread.MAX_PRIORITY
            isDaemon = true
        }
    }.asCoroutineDispatcher()
    
    // Low-priority background
    val Background: CoroutineDispatcher = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "Sentinel-Background").apply {
            priority = Thread.MIN_PRIORITY
            isDaemon = true
        }
    }.asCoroutineDispatcher()
    
    // Lock-free state (atomic)
    private val activeTasks = AtomicInteger(0)
    
    fun monitorLoad(): Float {
        return activeTasks.get().toFloat() / (coreCount * 4)
    }
}
