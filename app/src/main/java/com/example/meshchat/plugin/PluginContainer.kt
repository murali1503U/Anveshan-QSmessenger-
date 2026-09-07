package com.example.meshchat.plugin

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import android.util.Log
import java.util.concurrent.Executors

sealed class PluginResult<out T> {
    data class Success<T>(val value: T) : PluginResult<T>()
    data class Error(val exception: Throwable) : PluginResult<Nothing>()
    object Timeout : PluginResult<Nothing>()
}

class PluginContainer(private val plugin: SentinelPlugin) {
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "Plugin-${plugin.id}").apply {
            setUncaughtExceptionHandler { _, e ->
                Log.e("PluginContainer", "Plugin ${plugin.id} crashed", e)
                PluginManager.handlePluginCrash(plugin.id)
            }
        }
    }
    
    private val memoryLimit = 50 * 1024 * 1024  // 50MB per plugin
    
    suspend fun <T> execute(action: suspend () -> T): PluginResult<T> {
        return try {
            val result = withTimeout(5000L) {  // 5 second timeout
                withContext(executor.asCoroutineDispatcher()) {
                    action()
                }
            }
            PluginResult.Success(result)
        } catch (e: TimeoutCancellationException) {
            PluginResult.Timeout
        } catch (e: Exception) {
            PluginResult.Error(e)
        }
    }
    
    fun checkMemory(): Boolean {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        return used < memoryLimit
    }
}
