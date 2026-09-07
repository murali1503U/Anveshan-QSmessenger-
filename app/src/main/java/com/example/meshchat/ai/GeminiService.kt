package com.example.meshchat.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(val parts: List<Part>)

@Serializable
data class Part(val text: String? = null)

@Serializable
data class GenerateContentResponse(val candidates: List<Candidate>)

@Serializable
data class Candidate(val content: Content)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiSecurityAnalyzer {
    /**
     * Uses AI model to decide whether a message requires encryption.
     * Returns true if it requires encryption, false otherwise.
     */
    suspend fun requiresEncryption(message: String, forceLocal: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val decision = determinePriorityLevel(message, forceLocal)
        decision.level >= 2
    }

    suspend fun determinePriorityLevel(message: String, forceLocal: Boolean = false): SecurityDecision = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = "AIzaSySentinelTestKey"
        
        // If force local or no key, use zero-latency rule engine
        if (forceLocal || apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext evaluateLightweightPriority(message, startTime, "Lightweight Heuristics (Offline)")
        }
        
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = "Payload text: \"$message\"")))
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(text = "You are the automated security priority decision engine for a LoRa Mesh Network. Classify the message into one of four priority levels: 1 (Plaintext/Speed for public/greetings), 2 (AES-256 for private/confidential), 3 (Forward Secrecy Ephemeral keys for passwords/credentials/coordinates/keys), 4 (Post-Quantum Kyber-768 for critical/crypto/master-keys). Output strictly in this format:\nLEVEL: <1|2|3|4>\nREASON: <concise reason under 12 words>\nFACTORS: <comma-separated key factors or none>")
                )
            )
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val reply = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val parsedLevel = Regex("LEVEL:\\s*([1-4])", RegexOption.IGNORE_CASE).find(reply)?.groupValues?.get(1)?.toIntOrNull()
            val reasonMatch = Regex("REASON:\\s*(.*)", RegexOption.IGNORE_CASE).find(reply)?.groupValues?.get(1)?.trim()
            val factorsMatch = Regex("FACTORS:\\s*(.*)", RegexOption.IGNORE_CASE).find(reply)?.groupValues?.get(1)?.trim()
            
            if (parsedLevel != null) {
                val duration = System.currentTimeMillis() - startTime
                val levelName = when (parsedLevel) {
                    1 -> "Level 1: Plaintext (Max Speed)"
                    2 -> "Level 2: Standard E2EE (AES-256)"
                    3 -> "Level 3: Forward Secrecy (Rotating Keys)"
                    4 -> "Level 4: Post-Quantum (Kyber-768)"
                    else -> "Level 2: Standard E2EE (AES-256)"
                }
                val overhead = when (parsedLevel) {
                    1 -> 0
                    2 -> 28
                    3 -> 44
                    4 -> 128
                    else -> 28
                }
                val factorsList = factorsMatch?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() && it.lowercase() != "none" } ?: emptyList()
                return@withContext SecurityDecision(
                    level = parsedLevel,
                    levelName = levelName,
                    confidence = 0.97f,
                    reason = reasonMatch ?: "Classified by Cloud Gemini 3.5 Flash",
                    sensitiveFactors = factorsList,
                    executionSource = "Cloud Gemini 3.5 Flash",
                    estimatedPacketOverheadBytes = overhead,
                    latencyMs = duration
                )
            }
            evaluateLightweightPriority(message, startTime, "Cloud AI Fallback (Local Rules)")
        } catch (e: Exception) {
            e.printStackTrace()
            evaluateLightweightPriority(message, startTime, "Offline Fallback Rule-Engine")
        }
    }

    private fun evaluateLightweightPriority(message: String, startTime: Long, source: String): SecurityDecision {
        val lower = message.lowercase()
        val duration = System.currentTimeMillis() - startTime
        
        val pqKeywords = listOf("quantum", "kyber", "lattice", "master_key", "topsecret", "top secret", "root cert", "nuclear", "cipher_override", "defense_clearance")
        if (pqKeywords.any { lower.contains(it) }) {
            return SecurityDecision(
                level = 4,
                levelName = "Level 4: Post-Quantum (Kyber-768)",
                confidence = 0.98f,
                reason = "Detected quantum-targeted or high-threat cryptographic keywords.",
                sensitiveFactors = listOf("Quantum/High-threat keywords"),
                executionSource = source,
                estimatedPacketOverheadBytes = 128,
                latencyMs = duration.coerceAtLeast(1)
            )
        }
        
        val p3Keywords = listOf("password", "passwd", "pin", "token", "otp", "secret", "rendezvous", "lat:", "lon:", "gps", "coordinate", "api_key", "auth", "private_key", "bearer")
        val hasCoordinates = Regex("[-+]?[0-9]{1,3}\\.[0-9]{3,7}").containsMatchIn(message)
        val hasPotentialHexOrToken = Regex("0x[0-9a-fA-F]{6,}|[A-Za-z0-9+/]{20,}={0,2}").containsMatchIn(message)
        
        if (p3Keywords.any { lower.contains(it) } || hasCoordinates || hasPotentialHexOrToken) {
            return SecurityDecision(
                level = 3,
                levelName = "Level 3: Forward Secrecy (Rotating Keys)",
                confidence = 0.94f,
                reason = "Detected credentials, high-entropy tokens, coordinates, or rotating session identifiers.",
                sensitiveFactors = if (hasCoordinates) listOf("GPS coordinates pattern") else listOf("Credentials/Tokens"),
                executionSource = source,
                estimatedPacketOverheadBytes = 44,
                latencyMs = duration.coerceAtLeast(1)
            )
        }
        
        val p2Keywords = listOf("confidential", "internal", "urgent", "alpha", "beta", "important", "media", "attachment", "private", "personal", "finance", "medical", "patient", "classified")
        val hasEmail = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}").containsMatchIn(message)
        val hasPhone = Regex("\\+?[0-9]{10,13}").containsMatchIn(message)
        
        if (p2Keywords.any { lower.contains(it) } || hasEmail || hasPhone || message.length > 180) {
            return SecurityDecision(
                level = 2,
                levelName = "Level 2: Standard E2EE (AES-256)",
                confidence = 0.89f,
                reason = "Detected private user content, contact identifiers, or confidential keywords.",
                sensitiveFactors = if (hasEmail) listOf("Email address pattern") else listOf("Confidential/Private text"),
                executionSource = source,
                estimatedPacketOverheadBytes = 28,
                latencyMs = duration.coerceAtLeast(1)
            )
        }
        
        return SecurityDecision(
            level = 1,
            levelName = "Level 1: Plaintext (Max Speed)",
            confidence = 0.96f,
            reason = "No sensitive keywords, high-entropy tokens, or PII detected. Optimized for LoRa duty-cycle.",
            sensitiveFactors = emptyList(),
            executionSource = source,
            estimatedPacketOverheadBytes = 0,
            latencyMs = duration.coerceAtLeast(1)
        )
    }
}

class MeshAssistant {
    suspend fun getResponse(query: String, history: List<Pair<String, Boolean>>): String = withContext(Dispatchers.IO) {
        val apiKey = "AIzaSySentinelTestKey"
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Please configure your Gemini API key in the AI Studio secrets panel to use the Mesh Assistant."
        }
        
        // Build conversation history for the prompt
        val historyContext = history.joinToString("\n") { (text, isUser) ->
            if (isUser) "User: $text" else "Assistant: $text"
        }
        
        val fullPrompt = if (historyContext.isNotBlank()) {
            "Conversation History:\n$historyContext\n\nUser: $query"
        } else {
            "User: $query"
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = fullPrompt)))
            ),
            systemInstruction = Content(
                parts = listOf(Part(text = "You are the Mesh Guide, an AI assistant embedded directly inside a LoRa Mesh Network Chat app. Your goal is to guide users on using the app, troubleshoot ESP32/BLE hardware pairing, and explain the security levels (Plaintext, AES-256, Forward Secrecy Session Keys, and Kyber Post-Quantum). Be concise, technical but accessible, and extremely helpful. Keep responses relatively short.")))
        )
        
        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "I couldn't process that request."
        } catch (e: Exception) {
            e.printStackTrace()
            "Connection error: Unable to reach the AI cloud. Please ensure you have internet access (this assistant requires the cloud, unlike the local mesh encryption)."
        }
    }
}
