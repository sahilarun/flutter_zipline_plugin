package com.example.flutter_zipline_plugin

import android.content.Context
import java.io.File
import java.security.MessageDigest


class ModuleCacheManager(private val context: Context) {
    
    /**
     * Get the cache directory for a specific manifest version.
     * 
     * Creates the directory structure if it doesn't exist:
     * - Base directory: <app_internal_storage>/zipline_cache
     * - Version directory: <base_directory>/<version>
     * 
     * @param version The manifest version string
     * @return The File object representing the version-specific cache directory
     * 
     */
    fun getCacheDir(version: String): File {
        val baseDir = File(context.filesDir, "zipline_cache")
        val versionDir = File(baseDir, version)
        
        if (!versionDir.exists()) {
            versionDir.mkdirs()
        }
        
        return versionDir
    }
    
    /**
     * Retrieve a cached module if it exists and passes SHA256 verification.
     * 
     * @param version The manifest version string
     * @param moduleId The module ID (used as filename without extension)
     * @param expectedSha256 The expected SHA256 hash (64 hexadecimal characters)
     * @return The cached module bytes if found and verified, null otherwise
     */
    fun getCachedModule(
        version: String,
        moduleId: String,
        expectedSha256: String
    ): ByteArray? {
        val file = File(getCacheDir(version), "$moduleId.zipline")
        
        if (!file.exists()) {
            return null
        }
        
        return try {
            val bytes = file.readBytes()
            
            val actualSha256 = computeSha256(bytes)
            
            if (actualSha256 == expectedSha256.lowercase()) {
                bytes
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Store a module in the cache.
     * 
     * Writes the module bytes to a file in the version-specific cache directory.
     * The file is named: <moduleId>.zipline
     * 
     * Creates the cache directory if it doesn't exist.
     * 
     * @param version The manifest version string
     * @param moduleId The module ID (used as filename without extension)
     * @param bytes The module bytes to cache
     * 
     */
    fun cacheModule(
        version: String,
        moduleId: String,
        bytes: ByteArray
    ) {
        val file = File(getCacheDir(version), "$moduleId.zipline")
        
        try {
            file.writeBytes(bytes)
        } catch (e: Exception) {
        }
    }
    
    /**
     * Compute the SHA256 hash of a byte array.
     * 
     * This is the same implementation as ModuleDownloader.computeSha256()
     * to ensure consistent hash computation across the plugin.
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
}
