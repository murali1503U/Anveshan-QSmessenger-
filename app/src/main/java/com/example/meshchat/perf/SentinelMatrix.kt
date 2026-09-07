package com.example.meshchat.perf

/**
 * Fast math matrix and vector operations.
 * Uses ForkJoin parallel execution for CPU acceleration without massive C++ NDK binaries.
 */
object SentinelMatrix {
    // Fast parallel sigmoid
    fun sigmoid(data: FloatArray): FloatArray {
        // Break into chunks and process in parallel
        val result = FloatArray(data.size)
        val chunks = data.indices.chunked(1024)
        
        val tasks = chunks.map { chunk ->
            {
                for (i in chunk) {
                    result[i] = 1f / (1f + Math.exp(-data[i].toDouble()).toFloat())
                }
            }
        }
        
        SentinelExecutor.submitParallel(tasks)
        return result
    }
    
    // Matrix multiplication A * B
    // A: m x n, B: n x k
    fun multiply(a: FloatArray, b: FloatArray, m: Int, n: Int, k: Int): FloatArray {
        val c = FloatArray(m * k)
        
        val tasks = (0 until m).map { row ->
            {
                for (col in 0 until k) {
                    var sum = 0f
                    for (i in 0 until n) {
                        sum += a[row * n + i] * b[i * k + col]
                    }
                    c[row * k + col] = sum
                }
            }
        }
        
        SentinelExecutor.submitParallel(tasks)
        return c
    }
}
