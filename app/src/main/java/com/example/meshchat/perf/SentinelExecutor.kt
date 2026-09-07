package com.example.meshchat.perf

import java.util.concurrent.ForkJoinPool

/**
 * Auto-scaling executor for extreme parallel performance.
 * Utilizes Java's ForkJoinPool for work-stealing concurrency.
 */
object SentinelExecutor {
    private val coreCount = Runtime.getRuntime().availableProcessors()
    private val executor = ForkJoinPool(
        coreCount,
        ForkJoinPool.defaultForkJoinWorkerThreadFactory,
        null,
        true  // asyncMode = true (FIFO for responsiveness)
    )
    
    fun submit(task: () -> Unit) {
        executor.submit { task() }
    }
    
    fun <T> submitParallel(tasks: List<() -> T>): List<T> {
        val futures = tasks.map { task ->
            executor.submit<T> { task() }
        }
        return futures.map { it.get() }
    }
}
