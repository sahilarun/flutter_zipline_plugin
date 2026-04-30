package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.errors.ZiplineError
import com.example.flutter_zipline_plugin.models.Manifest
import com.example.flutter_zipline_plugin.models.ModuleMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class ZiplineEngineManagerTest : StringSpec({
    
    "createExtension should throw ModuleLoadException when module not in map" {
        val manager = ZiplineEngineManager()
        
        val manifest = Manifest(
            version = "1.0.0",
            mainModuleId = "main",
            mainFunction = "initialize",
            modules = mapOf(
                "main" to ModuleMetadata(
                    url = "https://example.com/main.zipline",
                    sha256 = "abc123",
                    dependsOnIds = emptyList()
                )
            )
        )
        
        val modules = emptyMap<String, ByteArray>()
        val loadOrder = listOf("main")
        
        val exception = shouldThrow<ZiplineError.ModuleLoadException> {
            manager.createExtension(manifest, modules, loadOrder)
        }
        
        exception.moduleId shouldBe "main"
        exception.message shouldContain "not found in modules map"
    }
    
    "callFunction should throw ExtensionNotFoundException for invalid extension ID" {
        val manager = ZiplineEngineManager()
        
        val exception = shouldThrow<ZiplineError.ExtensionNotFoundException> {
            manager.callFunction(
                extensionId = "invalid-id",
                functionName = "test",
                arguments = emptyMap()
            )
        }
        
        exception.extensionId shouldBe "invalid-id"
        exception.message shouldContain "Extension invalid-id not found"
    }
    
    "unloadExtension should not throw for non-existent extension" {
        val manager = ZiplineEngineManager()
        
        manager.unloadExtension("non-existent-id")
    }
    
    "createExtension should generate unique extension IDs" {
        val manager = ZiplineEngineManager()
        
        val jsCode = """
            function initialize() {
                return "initialized";
            }
        """.trimIndent()
        
        val manifest = Manifest(
            version = "1.0.0",
            mainModuleId = "main",
            mainFunction = "initialize",
            modules = mapOf(
                "main" to ModuleMetadata(
                    url = "https://example.com/main.zipline",
                    sha256 = "abc123",
                    dependsOnIds = emptyList()
                )
            )
        )
        
    }
})
