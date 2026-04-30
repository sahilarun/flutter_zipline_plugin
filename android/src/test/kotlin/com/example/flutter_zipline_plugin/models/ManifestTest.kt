package com.example.flutter_zipline_plugin.models

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for Manifest data model and JSON serialization.
 * 
 * Tests cover:
 * - JSON parsing from valid manifest
 * - JSON serialization round-trip
 * - Parsing with missing required fields
 * - Parsing with invalid JSON
 */
class ManifestTest {
    
    @Test
    fun `test parse valid manifest JSON`() {
        val json = """
            {
              "version": "1.0.0",
              "mainModuleId": "main",
              "mainFunction": "initialize",
              "modules": {
                "core": {
                  "url": "https://example.com/core.zipline",
                  "sha256": "abc123def456",
                  "dependsOnIds": []
                },
                "main": {
                  "url": "https://example.com/main.zipline",
                  "sha256": "ghi789jkl012",
                  "dependsOnIds": ["core"]
                }
              }
            }
        """.trimIndent()
        
        val manifest = Manifest.fromJson(json)
        
        assertEquals("1.0.0", manifest.version)
        assertEquals("main", manifest.mainModuleId)
        assertEquals("initialize", manifest.mainFunction)
        assertEquals(2, manifest.modules.size)
        
        val coreModule = manifest.modules["core"]
        assertNotNull(coreModule)
        assertEquals("https://example.com/core.zipline", coreModule?.url)
        assertEquals("abc123def456", coreModule?.sha256)
        assertEquals(0, coreModule?.dependsOnIds?.size)
        
        val mainModule = manifest.modules["main"]
        assertNotNull(mainModule)
        assertEquals("https://example.com/main.zipline", mainModule?.url)
        assertEquals("ghi789jkl012", mainModule?.sha256)
        assertEquals(1, mainModule?.dependsOnIds?.size)
        assertEquals("core", mainModule?.dependsOnIds?.get(0))
    }
    
    @Test
    fun `test JSON serialization round-trip`() {
        val original = Manifest(
            version = "2.0.0",
            mainModuleId = "app",
            mainFunction = "start",
            modules = mapOf(
                "utils" to ModuleMetadata(
                    url = "https://example.com/utils.zipline",
                    sha256 = "1234567890abcdef",
                    dependsOnIds = emptyList()
                ),
                "app" to ModuleMetadata(
                    url = "https://example.com/app.zipline",
                    sha256 = "fedcba0987654321",
                    dependsOnIds = listOf("utils")
                )
            )
        )
        
        val json = original.toJson()
        val parsed = Manifest.fromJson(json)
        
        assertEquals(original.version, parsed.version)
        assertEquals(original.mainModuleId, parsed.mainModuleId)
        assertEquals(original.mainFunction, parsed.mainFunction)
        assertEquals(original.modules.size, parsed.modules.size)
        
        original.modules.forEach { (id, metadata) ->
            val parsedMetadata = parsed.modules[id]
            assertNotNull(parsedMetadata)
            assertEquals(metadata.url, parsedMetadata?.url)
            assertEquals(metadata.sha256, parsedMetadata?.sha256)
            assertEquals(metadata.dependsOnIds, parsedMetadata?.dependsOnIds)
        }
    }
    
    @Test
    fun `test parse manifest with empty modules`() {
        val json = """
            {
              "version": "1.0.0",
              "mainModuleId": "main",
              "mainFunction": "initialize",
              "modules": {}
            }
        """.trimIndent()
        
        val manifest = Manifest.fromJson(json)
        
        assertEquals("1.0.0", manifest.version)
        assertEquals("main", manifest.mainModuleId)
        assertEquals("initialize", manifest.mainFunction)
        assertEquals(0, manifest.modules.size)
    }
    
    @Test
    fun `test parse manifest with single module`() {
        val json = """
            {
              "version": "1.0.0",
              "mainModuleId": "single",
              "mainFunction": "run",
              "modules": {
                "single": {
                  "url": "https://example.com/single.zipline",
                  "sha256": "abcdef1234567890",
                  "dependsOnIds": []
                }
              }
            }
        """.trimIndent()
        
        val manifest = Manifest.fromJson(json)
        
        assertEquals(1, manifest.modules.size)
        val module = manifest.modules["single"]
        assertNotNull(module)
        assertEquals("https://example.com/single.zipline", module?.url)
    }
    
    @Test
    fun `test parse invalid JSON throws exception`() {
        val invalidJson = "{ invalid json }"
        
        assertThrows(Exception::class.java) {
            Manifest.fromJson(invalidJson)
        }
    }
    
    @Test
    fun `test parse JSON with missing required fields throws exception`() {
        val incompleteJson = """
            {
              "version": "1.0.0",
              "mainModuleId": "main"
            }
        """.trimIndent()
        
        assertThrows(Exception::class.java) {
            Manifest.fromJson(incompleteJson)
        }
    }
}
