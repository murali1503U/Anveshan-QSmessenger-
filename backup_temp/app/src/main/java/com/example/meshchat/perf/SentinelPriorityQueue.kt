package com.example.meshchat.perf

import java.util.concurrent.PriorityBlockingQueue

/**
 * Priority-based non-blocking queue for mesh message scheduling.
 */
class SentinelPriorityQueue {
    private val criticalQueue = PriorityBlockingQueue<Runnable>(11) { a, b ->
        // Higher priority first
        val pA = (a as? PrioritizedTask)?.priority ?: 0
        val pB = (b as? PrioritizedTask)?.priority ?: 0
        pB - pA
    }
    
    fun submitCritical(task: () -> Unit) {
        criticalQueue.offer(PrioritizedTask(task, 10))
    }
    
    fun submitBackground(task: () -> Unit) {
        criticalQueue.offer(PrioritizedTask(task, 0))
    }
    
    fun poll(): Runnable? {
        return criticalQueue.poll()
    }
    
    private class PrioritizedTask(
        val task: () -> Unit,
        val priority: Int
    ) : Runnable, Comparable<PrioritizedTask> {
        override fun run() = task()
        override fun compareTo(other: PrioritizedTask): Int {
            return other.priority.compareTo(this.priority)
        }
    }
}
