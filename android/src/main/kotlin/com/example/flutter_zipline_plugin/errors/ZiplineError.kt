package com.example.flutter_zipline_plugin.errors

/**
 * Base sealed class for all Zipline plugin errors.
 * 
 * This provides a type-safe way to handle different error scenarios
 * and ensures all errors have descriptive messages with context.
 */
sealed class ZiplineError(message: String) : Exception(message) {
    
    /**
     * Error thrown when manifest download fails.
     * 
     * @property url The manifest URL that failed to download
     * @property statusCode HTTP status code if available
     */
    class ManifestDownloadException(
        val url: String,
        val statusCode: Int? = null,
        message: String = buildMessage(url, statusCode)
    ) : ZiplineError(message) {
        companion object {
            private fun buildMessage(url: String, statusCode: Int?): String {
                return if (statusCode != null) {
                    "Failed to download manifest from $url: HTTP $statusCode"
                } else {
                    "Failed to download manifest from $url: Network error"
                }
            }
        }
    }
    
    /**
     * Error thrown when manifest parsing fails.
     * 
     * @property url The manifest URL that was being parsed
     * @property cause The underlying parsing exception
     */
    class ManifestParseException(
        val url: String,
        cause: Throwable? = null,
        message: String = "Failed to parse manifest from $url: Invalid JSON format"
    ) : ZiplineError(message) {
        init {
            if (cause != null) {
                initCause(cause)
            }
        }
    }
    
    /**
     * Error thrown when circular dependencies are detected in the module graph.
     * 
     * @property moduleIds The module IDs involved in the cycle
     */
    class CircularDependencyException(
        val moduleIds: List<String> = emptyList(),
        message: String = "Circular dependency detected in module graph"
    ) : ZiplineError(message)
    
    /**
     * Error thrown when module download fails.
     * 
     * @property moduleId The module ID that failed to download
     * @property url The module URL that failed to download
     * @property statusCode HTTP status code if available
     */
    class ModuleDownloadException(
        val moduleId: String,
        val url: String,
        val statusCode: Int? = null,
        message: String = buildMessage(moduleId, url, statusCode)
    ) : ZiplineError(message) {
        companion object {
            private fun buildMessage(moduleId: String, url: String, statusCode: Int?): String {
                return if (statusCode != null) {
                    "Failed to download module '$moduleId' from $url: HTTP $statusCode"
                } else {
                    "Failed to download module '$moduleId' from $url: Network error"
                }
            }
        }
    }
    
    /**
     * Error thrown when module SHA256 verification fails.
     * 
     * @property moduleId The module ID that failed verification
     * @property expectedSha256 The expected SHA256 hash from the manifest
     * @property actualSha256 The actual SHA256 hash computed from downloaded bytes
     */
    class ModuleVerificationException(
        val moduleId: String,
        val expectedSha256: String,
        val actualSha256: String,
        message: String = "Module '$moduleId' verification failed: expected SHA256 $expectedSha256, got $actualSha256"
    ) : ZiplineError(message)
    
    /**
     * Error thrown when module loading into Zipline engine fails.
     * 
     * @property moduleId The module ID that failed to load
     */
    class ModuleLoadException(
        val moduleId: String,
        cause: Throwable? = null,
        message: String = "Failed to load module '$moduleId' into Zipline engine"
    ) : ZiplineError(message) {
        init {
            if (cause != null) {
                initCause(cause)
            }
        }
    }
    
    /**
     * Error thrown when a requested function is not found in the extension.
     * 
     * @property extensionId The extension ID
     * @property functionName The function name that was not found
     */
    class FunctionNotFoundException(
        val extensionId: String,
        val functionName: String,
        message: String = "Function '$functionName' not found in extension $extensionId"
    ) : ZiplineError(message)
    
    /**
     * Error thrown when function execution fails.
     * 
     * @property extensionId The extension ID
     * @property functionName The function name that failed
     */
    class FunctionExecutionException(
        val extensionId: String,
        val functionName: String,
        cause: Throwable? = null,
        message: String = "Function '$functionName' in extension $extensionId threw an exception"
    ) : ZiplineError(message) {
        init {
            if (cause != null) {
                initCause(cause)
            }
        }
    }
    
    /**
     * Error thrown when an extension ID is not found.
     * 
     * @property extensionId The extension ID that was not found
     */
    class ExtensionNotFoundException(
        val extensionId: String,
        message: String = "Extension $extensionId not found"
    ) : ZiplineError(message)
}
