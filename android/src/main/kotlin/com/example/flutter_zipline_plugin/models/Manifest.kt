package com.example.flutter_zipline_plugin.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Represents a Zipline manifest file containing module metadata and dependencies.
 * 
 */
@Serializable
data class Manifest(
    val version: String,
    val mainModuleId: String,
    val mainFunction: String,
    val modules: Map<String, ModuleMetadata>
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        /**
         * Parse a JSON string into a Manifest object.
         * 
         * @param jsonString The JSON string to parse
         * @return The parsed Manifest object
         * @throws kotlinx.serialization.SerializationException if JSON is invalid
         */
        fun fromJson(jsonString: String): Manifest {
            return json.decodeFromString<Manifest>(jsonString)
        }
    }
    
    /**
     * Serialize this Manifest to a JSON string.
     * 
     * @return JSON string representation of this manifest
     */
    fun toJson(): String {
        return json.encodeToString(this)
    }
}

/**
 * Metadata for a single Zipline module.
 * 
 * @property url URL to download the .zipline file
 * @property sha256 SHA256 hash of the module file (64 hexadecimal characters)
 * @property dependsOnIds List of module IDs this module depends on
 */
@Serializable
data class ModuleMetadata(
    val url: String,
    val sha256: String,
    val dependsOnIds: List<String>
)
