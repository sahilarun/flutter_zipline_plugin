package com.example.flutter_zipline_plugin

import android.content.Context
import com.example.flutter_zipline_plugin.errors.ZiplineError
import com.example.flutter_zipline_plugin.models.Manifest
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ZiplinePluginHandler(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler {
    
    private val manifestDownloader = ManifestDownloader()
    private val dependencyResolver = DependencyResolver()
    private val moduleDownloader = ModuleDownloader()
    private val cacheManager = ModuleCacheManager(context)
    private val engineManager = ZiplineEngineManager()

    private var eventSink: EventChannel.EventSink? = null
    
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "loadExtension" -> handleLoadExtension(call, result)
            "callFunction" -> handleCallFunction(call, result)
            "unloadExtension" -> handleUnloadExtension(call, result)
            else -> result.notImplemented()
        }
    }
    
    private fun handleLoadExtension(call: MethodCall, result: MethodChannel.Result) {
        val manifestUrl = call.argument<String>("manifestUrl")
        
        if (manifestUrl == null) {
            result.error(
                "INVALID_ARGUMENT",
                "manifestUrl is required",
                null
            )
            return
        }

        coroutineScope.launch {
            try {
                val extensionId = loadExtensionAsync(manifestUrl)
                result.success(extensionId)
            } catch (e: ZiplineError) {
                val errorCode = e.javaClass.simpleName
                result.error(errorCode, e.message, null)
                
                emitEvent(mapOf(
                    "type" to "error",
                    "error" to e.message
                ))
            } catch (e: Exception) {
                result.error(
                    "UNEXPECTED_ERROR",
                    "Unexpected error during loadExtension: ${e.message}",
                    null
                )
                
                emitEvent(mapOf(
                    "type" to "error",
                    "error" to "Unexpected error: ${e.message}"
                ))
            }
        }
    }
    
    private suspend fun loadExtensionAsync(manifestUrl: String): String {
        emitEvent(mapOf(
            "type" to "manifest_downloading",
            "message" to "Downloading manifest from $manifestUrl"
        ))
        
        val manifest = manifestDownloader.download(manifestUrl)
        
        emitEvent(mapOf(
            "type" to "manifest_parsed",
            "message" to "Manifest parsed successfully, resolving dependencies"
        ))
        
        val loadOrder = dependencyResolver.resolveOrder(manifest.modules)
        val modules = mutableMapOf<String, ByteArray>()
        
        for (moduleId in loadOrder) {
            val metadata = manifest.modules[moduleId]!!
            
            val cachedModule = cacheManager.getCachedModule(
                version = manifest.version,
                moduleId = moduleId,
                expectedSha256 = metadata.sha256
            )
            
            if (cachedModule != null) {
                modules[moduleId] = cachedModule
                
                emitEvent(mapOf(
                    "type" to "module_loaded",
                    "moduleName" to moduleId,
                    "message" to "Module '$moduleId' loaded from cache"
                ))
            } else {
                emitEvent(mapOf(
                    "type" to "module_downloading",
                    "moduleName" to moduleId,
                    "message" to "Downloading module '$moduleId' from ${metadata.url}"
                ))
                
                val moduleBytes = moduleDownloader.download(
                    url = metadata.url,
                    expectedSha256 = metadata.sha256
                )
                
                cacheManager.cacheModule(
                    version = manifest.version,
                    moduleId = moduleId,
                    bytes = moduleBytes
                )
                
                modules[moduleId] = moduleBytes
                
                emitEvent(mapOf(
                    "type" to "module_loaded",
                    "moduleName" to moduleId,
                    "message" to "Module '$moduleId' downloaded and verified"
                ))
            }
        }
        
        val extensionId = engineManager.createExtension(
            manifest = manifest,
            modules = modules,
            loadOrder = loadOrder
        )
        
        emitEvent(mapOf(
            "type" to "extension_ready",
            "extensionId" to extensionId,
            "message" to "Extension loaded successfully"
        ))
        
        return extensionId
    }
    
    private fun handleCallFunction(call: MethodCall, result: MethodChannel.Result) {
        val extensionId = call.argument<String>("extensionId")
        val functionName = call.argument<String>("functionName")
        val arguments = call.argument<Map<String, Any?>>("arguments")
        
        if (extensionId == null || functionName == null || arguments == null) {
            result.error(
                "INVALID_ARGUMENT",
                "extensionId, functionName, and arguments are required",
                null
            )
            return
        }
        
        coroutineScope.launch {
            try {
                val functionResult = engineManager.callFunction(
                    extensionId = extensionId,
                    functionName = functionName,
                    arguments = arguments
                )
                result.success(functionResult)
            } catch (e: ZiplineError) {
                val errorCode = e.javaClass.simpleName
                result.error(errorCode, e.message, null)
            } catch (e: Exception) {
                result.error(
                    "UNEXPECTED_ERROR",
                    "Unexpected error during callFunction: ${e.message}",
                    null
                )
            }
        }
    }
    

    private fun handleUnloadExtension(call: MethodCall, result: MethodChannel.Result) {
        val extensionId = call.argument<String>("extensionId")
        
        if (extensionId == null) {
            result.error(
                "INVALID_ARGUMENT",
                "extensionId is required",
                null
            )
            return
        }
        
        coroutineScope.launch {
            try {
                engineManager.unloadExtension(extensionId)
                result.success(null)
            } catch (e: ZiplineError) {
                val errorCode = e.javaClass.simpleName
                result.error(errorCode, e.message, null)
            } catch (e: Exception) {
                result.error(
                    "UNEXPECTED_ERROR",
                    "Unexpected error during unloadExtension: ${e.message}",
                    null
                )
            }
        }
    }
    
    /**
     * EventChannel.StreamHandler implementation: called when Dart starts listening.
     */
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }
    
    /**
     * EventChannel.StreamHandler implementation: called when Dart stops listening.
     */
    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
    
    /**
     * Emit an event to the Dart layer through the EventChannel.
     * 
     */
    private fun emitEvent(event: Map<String, Any?>) {
        eventSink?.success(event)
    }
}
