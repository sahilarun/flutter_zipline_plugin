package com.example.flutter_zipline_plugin

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Flutter Zipline Plugin - Main plugin registration class.
 * 
 * This class handles plugin lifecycle and sets up communication channels
 * between Flutter and native Android code.
 * 
 */
class FlutterZiplinePlugin : FlutterPlugin {
    
    /// MethodChannel for method invocations
    private lateinit var methodChannel: MethodChannel
    
    /// EventChannel for streaming events
    private lateinit var eventChannel: EventChannel
    
    /// Handler for method calls and event streaming
    private lateinit var handler: ZiplinePluginHandler
    
    /// Coroutine scope for async operations
    private lateinit var coroutineScope: CoroutineScope
    
    /**
     * Called when the plugin is attached to the Flutter engine.
     * 
     */
    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        // Create coroutine scope with SupervisorJob for independent coroutine failure handling
        coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        
        handler = ZiplinePluginHandler(
            context = flutterPluginBinding.applicationContext,
            coroutineScope = coroutineScope
        )

        methodChannel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "com.zipline/methods"
        )
        methodChannel.setMethodCallHandler(handler)
        
        eventChannel = EventChannel(
            flutterPluginBinding.binaryMessenger,
            "com.zipline/events"
        )
        eventChannel.setStreamHandler(handler)
    }
    
    /**
     * Called when the plugin is detached from the Flutter engine.
     * 
     */
    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        
        coroutineScope.cancel()
    }
}
