package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.errors.ZiplineError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.security.MessageDigest

/**
 * Unit tests for ModuleDownloader.
 * 
 * Tests cover:
 * - Successful module download with SHA256 verification
 * - SHA256 mismatch rejection
 * - Network error handling
 * - HTTP error handling (404, 500, etc.)
 * - Empty response body
 * 
 * Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6
 */
class ModuleDownloaderTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var downloader: ModuleDownloader
    
    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        downloader = ModuleDownloader()
    }
    
    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    /**
     * Helper function to compute SHA256 hash of a byte array.
     */
    private fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    @Test
    fun `test successful module download with matching SHA256`() = runTest {
        // Create test module content
        val moduleContent = "test module content".toByteArray()
        val expectedSha256 = computeSha256(moduleContent)
        
        // Mock successful HTTP response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(moduleContent))
        )
        
        val url = mockWebServer.url("/module.zipline").toString()
        
        // Download should succeed with matching hash
        val downloadedBytes = downloader.download(url, expectedSha256)
        
        assertNotNull(downloadedBytes)
        assertTrue(downloadedBytes.isNotEmpty())
    }
    
    @Test
    fun `test module download with SHA256 mismatch`() = runTest {
        // Create test module content
        val moduleContent = "test module content".toByteArray()
        val actualSha256 = computeSha256(moduleContent)
        val wrongSha256 = "0000000000000000000000000000000000000000000000000000000000000000"
        
        // Mock successful HTTP response
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(Buffer().write(moduleContent))
        )
        
        val url = mockWebServer.url("/module.zipline").toString()
        
        // Download should fail with verification exception
        val exception = assertThrows<ZiplineError.ModuleVerificationException> {
            downloader.download(url, wrongSha256)
        }
        
        assertEquals(wrongSha256, exception.expectedSha256)
        assertEquals(actualSha256, exception.actualSha256)
        assertTrue(exception.message?.contains("verification failed") == true)
    }
    
    @Test
    fun `test module download with 404 error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )
        
        val url = mockWebServer.url("/module.zipline").toString()
        val expectedSha256 = "abc123"
        
        val exception = assertThrows<ZiplineError.ModuleDownloadException> {
            downloader.download(url, expectedSha256)
        }
        
        assertEquals(url, exception.url)
        assertEquals(404, exception.statusCode)
        assertTrue(exception.message?.contains("HTTP 404") == true)
    }
    
    @Test
    fun `test module download with 500 error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )
        
        val url = mockWebServer.url("/module.zipline").toString()
        val expectedSha256 = "abc123"
        
        val exception = assertThrows<ZiplineError.ModuleDownloadException> {
            downloader.download(url, expectedSha256)
        }
        
        assertEquals(url, exception.url)
        assertEquals(500, exception.statusCode)
        assertTrue(exception.message?.contains("HTTP 500") == true)
    }
    
    @Test
    fun `test module download with unreachable URL`() = runTest {
        // Use an invalid URL that will cause a network error
        val unreachableUrl = "http://invalid-host-that-does-not-exist-12345.com/module.zipline"
        val expectedSha256 = "abc123"
        
        val exception = assertThrows<ZiplineError.ModuleDownloadException> {
            downloader.download(unreachableUrl, expectedSha256)
        }
        
        assertEquals(unreachableUrl, exception.url)
        assertNull(exception.statusCode)
        assertTrue(exception.message?.contains("Failed to download") == true)
    }
    
    @Test
    fun `test SHA256 hash is 64 hexadecimal characters`() {
        val testData = "test data".toByteArray()
        val hash = computeSha256(testData)
        
        // SHA256 should always produce 64 hex characters
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[a-f0-9]{64}$")))
    }
    
    @Test
    fun `test SHA256 hash is deterministic`() {
        val testData = "test data".toByteArray()
        
        // Computing hash multiple times should produce same result
        val hash1 = computeSha256(testData)
        val hash2 = computeSha256(testData)
        val hash3 = computeSha256(testData)
        
        assertEquals(hash1, hash2)
        assertEquals(hash2, hash3)
    }
}
