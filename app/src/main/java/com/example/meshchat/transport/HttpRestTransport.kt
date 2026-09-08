package com.example.meshchat.transport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

class HttpRestTransport : QsTransport {
    private val currentState = AtomicReference(TransportState.DISCONNECTED)
    private var receiveCallback: ((ByteArray) -> Unit)? = null
    private val baseUrl = "http://192.168.4.1"
    private var isPolling = false

    override suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        currentState.set(TransportState.CONNECTING)
        try {
            val url = URL("$baseUrl/")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            conn.responseCode
            currentState.set(TransportState.CONNECTED)
            startPolling()
            Result.success(Unit)
        } catch (e: Exception) {
            currentState.set(TransportState.ERROR)
            Result.failure(e)
        }
    }

    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        thread {
            while (isPolling && currentState.get() == TransportState.CONNECTED) {
                try {
                    val url = URL("$baseUrl/message")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.requestMethod = "GET"
                    if (conn.responseCode == 200) {
                        val input = conn.inputStream.readBytes()
                        if (input.isNotEmpty()) {
                            receiveCallback?.invoke(input)
                        }
                    }
                } catch (e: Exception) {
                }
                Thread.sleep(1000)
            }
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        isPolling = false
        currentState.set(TransportState.DISCONNECTED)
    }

    override suspend fun send(packet: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val base64 = Base64.getEncoder().encodeToString(packet)
            val url = URL("$baseUrl/send?msg=$base64")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                Result.success(Unit)
            } else {
                Result.failure(IOException("HTTP ${conn.responseCode}"))
            }
        } catch (e: Exception) {
            currentState.set(TransportState.ERROR)
            Result.failure(e)
        }
    }

    override fun onReceive(callback: (ByteArray) -> Unit) {
        this.receiveCallback = callback
    }

    override fun state(): TransportState = currentState.get()
}
