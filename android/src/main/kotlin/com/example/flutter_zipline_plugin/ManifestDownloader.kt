package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.errors.ZiplineError
import com.example.flutter_zipline_plugin.models.Manifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Downloads and parses Zipline manifest files from remote URLs.
 * 
 */
class ManifestDownloader {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Download and parse a manifest from the given URL.
     * 
     * @param url The manifest URL to download
     * @return The parsed Manifest object
     * @throws ZiplineError.ManifestDownloadException if download fails
     * @throws ZiplineError.ManifestParseException if JSON parsing fails
     */
    suspend fun download(url: String): Manifest = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = try {
                httpClient.newCall(request).execute()
            } catch (e: IOException) {
                throw ZiplineError.ManifestDownloadException(
                    url = url,
                    statusCode = null,
                    message = "Failed to download manifest from $url: ${e.message}"
                )
            }
            
            if (!response.isSuccessful) {
                throw ZiplineError.ManifestDownloadException(
                    url = url,
                    statusCode = response.code
                )
            }
            
            val json = response.body?.string()
                ?: throw ZiplineError.ManifestDownloadException(
                    url = url,
                    statusCode = response.code,
                    message = "Failed to download manifest from $url: Empty response body"
                )
            
            try {
                Manifest.fromJson(json)
            } catch (e: Exception) {
                throw ZiplineError.ManifestParseException(
                    url = url,
                    cause = e
                )
            }
        } catch (e: ZiplineError) {
            throw e
        } catch (e: Exception) {
            throw ZiplineError.ManifestDownloadException(
                url = url,
                statusCode = null,
                message = "Unexpected error downloading manifest from $url: ${e.message}"
            )
        }
    }
}
