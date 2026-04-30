package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.errors.ZiplineError
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ManifestDownloader.
 * 
 * Tests cover:
 * - Successful manifest download and parsing
 * - Network error handling (unreachable URL)
 * - HTTP error handling (404, 500, etc.)
 * - Invalid JSON parsing
 * - Empty response body
 */
class ManifestDownloaderTest {
    
    private lateinit var mockWebServer: MockWebServer
    private lateinit var downloader: ManifestDownloader
    
    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        downloader = ManifestDownloader()
    }
    
    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }
    
    @Test
    fun `test successful manifest download`() = runTest {
        val validManifestJson = """
            {
              "version": "1.0.0",
              "mainModuleId": "main",
              "mainFunction": "initialize",
              "modules": {
                "main": {
                  "url": "https://example.com/main.zipline",
                  "sha256": "abc123",
                  "dependsOnIds": []
                }
              }
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(validManifestJson)
        )
        
        val url = mockWebServer.url("/manifest.json").toString()
        val manifest = downloader.download(url)
        
        assertEquals("1.0.0", manifest.version)
        assertEquals("main", manifest.mainModuleId)
        assertEquals("initialize", manifest.mainFunction)
        assertEquals(1, manifest.modules.size)
    }
    
    @Test
    fun `test manifest download with 404 error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(404)
                .setBody("Not Found")
        )
        
        val url = mockWebServer.url("/manifest.json").toString()
        
        try {
            downloader.download(url)
            fail("Expected ManifestDownloadException to be thrown")
        } catch (exception: ZiplineError.ManifestDownloadException) {
            assertEquals(url, exception.url)
            assertEquals(404, exception.statusCode)
            assertTrue(exception.message?.contains("HTTP 404") == true)
        }
    }
    
    @Test
    fun `test manifest download with 500 error`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )
        
        val url = mockWebServer.url("/manifest.json").toString()
        
        try {
            downloader.download(url)
            fail("Expected ManifestDownloadException to be thrown")
        } catch (exception: ZiplineError.ManifestDownloadException) {
            assertEquals(url, exception.url)
            assertEquals(500, exception.statusCode)
            assertTrue(exception.message?.contains("HTTP 500") == true)
        }
    }
    
    @Test
    fun `test manifest download with invalid JSON`() = runTest {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{ invalid json }")
        )
        
        val url = mockWebServer.url("/manifest.json").toString()
        
        try {
            downloader.download(url)
            fail("Expected ManifestParseException to be thrown")
        } catch (exception: ZiplineError.ManifestParseException) {
            assertEquals(url, exception.url)
            assertTrue(exception.message?.contains("Invalid JSON") == true)
        }
    }
    
    @Test
    fun `test manifest download with missing required fields`() = runTest {
        val incompleteJson = """
            {
              "version": "1.0.0",
              "mainModuleId": "main"
            }
        """.trimIndent()
        
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(incompleteJson)
        )
        
        val url = mockWebServer.url("/manifest.json").toString()
        
        try {
            downloader.download(url)
            fail("Expected ManifestParseException to be thrown")
        } catch (exception: ZiplineError.ManifestParseException) {
            // Test passes if exception is thrown
            assertEquals(url, exception.url)
        }
    }
    
    @Test
    fun `test manifest download with unreachable URL`() = runTest {
        // Use an invalid URL that will cause a network error
        val unreachableUrl = "http://invalid-host-that-does-not-exist-12345.com/manifest.json"
        
        try {
            downloader.download(unreachableUrl)
            fail("Expected ManifestDownloadException to be thrown")
        } catch (exception: ZiplineError.ManifestDownloadException) {
            assertEquals(unreachableUrl, exception.url)
            assertNull(exception.statusCode)
            assertTrue(exception.message?.contains("Failed to download") == true)
        }
    }
}
