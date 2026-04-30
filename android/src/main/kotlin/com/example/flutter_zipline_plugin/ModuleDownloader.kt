package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.errors.ZiplineError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit


class ModuleDownloader {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    /**
     * Download a Zipline module from a URL and verify its SHA256 hash.
     * 
     * @param url The URL to download the module from
     * @param expectedSha256 The expected SHA256 hash (64 hexadecimal characters)
     * @return The downloaded module bytes if verification succeeds
     * @throws ZiplineError.ModuleDownloadException if HTTP download fails
     * @throws ZiplineError.ModuleVerificationException if SHA256 hash doesn't match
     * 
     */
    suspend fun download(url: String, expectedSha256: String): ByteArray = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw ZiplineError.ModuleDownloadException(
                moduleId = extractModuleIdFromUrl(url),
                url = url,
                statusCode = null
            )
        }
        
        if (!response.isSuccessful) {
            throw ZiplineError.ModuleDownloadException(
                moduleId = extractModuleIdFromUrl(url),
                url = url,
                statusCode = response.code
            )
        }
        
        val bytes = response.body?.bytes() 
            ?: throw ZiplineError.ModuleDownloadException(
                moduleId = extractModuleIdFromUrl(url),
                url = url,
                statusCode = response.code
            )
        
        val actualSha256 = computeSha256(bytes)
        
        if (actualSha256 != expectedSha256.lowercase()) {
            throw ZiplineError.ModuleVerificationException(
                moduleId = extractModuleIdFromUrl(url),
                expectedSha256 = expectedSha256,
                actualSha256 = actualSha256
            )
        }
        
        bytes
    }
    
    /**
     * Compute the SHA256 hash of a byte array.
     * 
     * @param bytes The byte array to hash
     * @return The SHA256 hash as a 64-character lowercase hexadecimal string
     * 
     */
    private fun computeSha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(bytes)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Extract a module ID from a URL for error reporting.
     * 
     * This is a best-effort extraction for error messages. It takes the
     * filename without extension from the URL path.
     * 
     * Example: "https://example.com/modules/core.zipline" -> "core"
     * 
     * @param url The module URL
     * @return The extracted module ID or "unknown" if extraction fails
     */
    private fun extractModuleIdFromUrl(url: String): String {
        return try {
            val path = url.substringAfterLast('/')
            path.substringBeforeLast('.')
        } catch (e: Exception) {
            "unknown"
        }
    }
}
