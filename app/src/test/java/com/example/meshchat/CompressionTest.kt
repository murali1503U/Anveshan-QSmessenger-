package com.example.meshchat

import com.example.meshchat.data.SentinelCompression
import org.junit.Assert.*
import org.junit.Test

class CompressionTest {

    @Test
    fun testTextCompressionAndDecompressionRoundtrip() {
        val originalText = "Sentinel Offline Mesh Communications: Long text designed for compression testing. " +
                "Repeating pattern: ABCD 1234 EFGH 5678 ABCD 1234 EFGH 5678 ABCD 1234."
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val compressed = SentinelCompression.compress(originalBytes)
        assertNotNull(compressed)

        val decompressed = SentinelCompression.decompress(compressed)
        assertNotNull(decompressed)

        val recoveredText = String(decompressed, Charsets.UTF_8)
        assertEquals("Recovered text must match original", originalText, recoveredText)
    }

    @Test
    fun testEmptyDataHandlingDoesNotCrash() {
        val emptyBytes = ByteArray(0)
        val compressed = SentinelCompression.compress(emptyBytes)
        assertEquals(0, compressed.size)

        val decompressed = SentinelCompression.decompress(compressed)
        assertEquals(0, decompressed.size)
    }

    @Test
    fun testBase64EncodeDecodeRoundtrip() {
        val testData = "Binary payload 1234567890 !@#$%^&*()".toByteArray(Charsets.UTF_8)
        val base64Str = SentinelCompression.toBase64(testData)
        assertTrue("Base64 string must not be empty", base64Str.isNotEmpty())

        val decodedBytes = SentinelCompression.fromBase64(base64Str)
        assertNotNull(decodedBytes)
        assertArrayEquals(testData, decodedBytes)

        // Invalid Base64 should return null safely without throwing
        val invalidResult = SentinelCompression.fromBase64("!!!InvalidBase64###")
        assertNull(invalidResult)
    }

    @Test
    fun testCoordinatesDeltaCompressionRoundtrip() {
        val baseLat = 12.971598
        val baseLon = 80.270685
        val targetLat = 12.972100
        val targetLon = 80.271200

        val compressedBytes = SentinelCompression.compressCoordinates(targetLat, targetLon, baseLat, baseLon)
        assertEquals(8, compressedBytes.size)

        val recovered = SentinelCompression.decompressCoordinates(compressedBytes, baseLat, baseLon)
        assertNotNull(recovered)

        assertEquals(targetLat, recovered!!.first, 0.00001)
        assertEquals(targetLon, recovered.second, 0.00001)
    }
}
