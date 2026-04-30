package com.example.flutter_zipline_plugin

import app.cash.zipline.Zipline
import com.example.flutter_zipline_plugin.errors.ZiplineError
import com.example.flutter_zipline_plugin.models.Manifest
import kotlinx.coroutines.Dispatchers
import java.util.UUID

class ZiplineEngineManager {
    
    /**
     * Map of extension IDs to their ExtensionInstance objects.
     * Thread-safe access is ensured by using a synchronized map.
     */
    private val extensions = mutableMapOf<String, ExtensionInstance>()
    
    /**
     * Creates a new extension by loading modules into a Zipline engine.
     * 
     * @param manifest The manifest containing module metadata and main function info
     * @param modules Map of module IDs to their bytecode
     * @param loadOrder List of module IDs in topological order (dependencies first)
     * @return Unique extension ID for subsequent operations
     * @throws ZiplineError.ModuleLoadException if module loading fails
     */
    fun createExtension(
        manifest: Manifest,
        modules: Map<String, ByteArray>,
        loadOrder: List<String>
    ): String {
        val zipline = Zipline.create(dispatcher = Dispatchers.IO)
        
        try {
            loadOrder.forEach { moduleId ->
                val moduleBytes = modules[moduleId]
                    ?: throw ZiplineError.ModuleLoadException(
                        moduleId = moduleId,
                        message = "Module '$moduleId' not found in modules map"
                    )
                
                try {
                    zipline.loadJsModule(moduleBytes, moduleId)
                } catch (e: Exception) {
                    throw ZiplineError.ModuleLoadException(
                        moduleId = moduleId,
                        cause = e,
                        message = "Failed to load module '$moduleId' into Zipline engine: ${e.message}"
                    )
                }
            }
            
            val mainFunction = manifest.mainFunction
            try {
                zipline.quickJs.evaluate("$mainFunction()")
            } catch (e: Exception) {
                throw ZiplineError.ModuleLoadException(
                    moduleId = manifest.mainModuleId,
                    cause = e,
                    message = "Failed to execute main function '$mainFunction': ${e.message}"
                )
            }
    
            val extensionId = UUID.randomUUID().toString()
            
            val instance = ExtensionInstance(
                id = extensionId,
                zipline = zipline,
                manifest = manifest
            )
            
            synchronized(extensions) {
                extensions[extensionId] = instance
            }
            
            return extensionId
            
        } catch (e: Exception) {
            try {
                zipline.close()
            } catch (closeException: Exception) {
            }
            throw e
        }
    }
    
    /**
     * Calls a function in a loaded extension.
     * @param extensionId The unique ID of the extension
     * @param functionName The name of the function to call
     * @param arguments Map of argument names to values
     * @return The function result (deserialized from JSON)
     * @throws ZiplineError.ExtensionNotFoundException if extension ID not found
     * @throws ZiplineError.FunctionExecutionException if function execution fails
     */
    fun callFunction(
        extensionId: String,
        functionName: String,
        arguments: Map<String, Any?>
    ): Any? {
        val instance = synchronized(extensions) {
            extensions[extensionId]
        } ?: throw ZiplineError.ExtensionNotFoundException(extensionId)
        
        try {
            val jsonElement = convertMapToJsonElement(arguments)
            val argsJson = kotlinx.serialization.json.Json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                jsonElement
            )
            
            val resultJson = instance.zipline.quickJs.evaluate(
                "$functionName($argsJson)"
            ) as? String ?: return null
            
            val resultElement = kotlinx.serialization.json.Json.parseToJsonElement(resultJson)
            return convertJsonElementToAny(resultElement)
            
        } catch (e: ZiplineError) {
            throw e
        } catch (e: Exception) {
            throw ZiplineError.FunctionExecutionException(
                extensionId = extensionId,
                functionName = functionName,
                cause = e,
                message = "Function '$functionName' in extension $extensionId threw an exception: ${e.message}"
            )
        }
    }
    
    /**
     * Unloads an extension and releases its resources.
     * 
     * @param extensionId The unique ID of the extension to unload
     */
    fun unloadExtension(extensionId: String) {
        val instance = synchronized(extensions) {
            extensions.remove(extensionId)
        }
        
        instance?.zipline?.close()
    }
    
    /**
     * Helper function to convert a Map to JsonElement.
     */
    private fun convertMapToJsonElement(map: Map<String, Any?>): kotlinx.serialization.json.JsonElement {
        return kotlinx.serialization.json.JsonObject(
            map.mapValues { (_, value) -> convertValueToJsonElement(value) }
        )
    }
    
    /**
     * Helper function to convert any value to JsonElement.
     */
    private fun convertValueToJsonElement(value: Any?): kotlinx.serialization.json.JsonElement {
        return when (value) {
            null -> kotlinx.serialization.json.JsonNull
            is String -> kotlinx.serialization.json.JsonPrimitive(value)
            is Number -> kotlinx.serialization.json.JsonPrimitive(value)
            is Boolean -> kotlinx.serialization.json.JsonPrimitive(value)
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                convertMapToJsonElement(value as Map<String, Any?>)
            }
            is List<*> -> {
                kotlinx.serialization.json.JsonArray(
                    value.map { convertValueToJsonElement(it) }
                )
            }
            else -> kotlinx.serialization.json.JsonPrimitive(value.toString())
        }
    }
    
    /**
     * Helper function to convert JsonElement back to Any?.
     */
    private fun convertJsonElementToAny(element: kotlinx.serialization.json.JsonElement): Any? {
        return when (element) {
            is kotlinx.serialization.json.JsonNull -> null
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    element.content.toIntOrNull() != null -> element.content.toInt()
                    element.content.toLongOrNull() != null -> element.content.toLong()
                    element.content.toDoubleOrNull() != null -> element.content.toDouble()
                    else -> element.content
                }
            }
            is kotlinx.serialization.json.JsonArray -> {
                element.map { convertJsonElementToAny(it) }
            }
            is kotlinx.serialization.json.JsonObject -> {
                element.mapValues { (_, value) -> convertJsonElementToAny(value) }
            }
        }
    }
}

/**
 * Represents a single loaded extension instance.
 * 
 * @property id Unique identifier for this extension
 * @property zipline The Zipline engine instance running this extension's code
 * @property manifest The manifest that was used to load this extension
 */
data class ExtensionInstance(
    val id: String,
    val zipline: Zipline,
    val manifest: Manifest
)
