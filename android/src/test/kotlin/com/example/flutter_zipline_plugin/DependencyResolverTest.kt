package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.models.ModuleMetadata
import com.example.flutter_zipline_plugin.errors.ZiplineError
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for DependencyResolver.
 * 
 * These tests verify that the dependency resolver correctly computes topological
 * ordering and detects circular dependencies.
 */
class DependencyResolverTest {
    
    private val resolver = DependencyResolver()
    
    @Test
    fun `resolveOrder with single module and no dependencies`() {
        // Arrange
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = emptyList()
            )
        )
        
        // Act
        val order = resolver.resolveOrder(modules)
        
        // Assert
        assertEquals(listOf("moduleA"), order)
    }
    
    @Test
    fun `resolveOrder with linear dependency chain`() {
        // Arrange: A depends on B, B depends on C
        // Expected order: C, B, A (dependencies first)
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = listOf("moduleB")
            ),
            "moduleB" to ModuleMetadata(
                url = "https://example.com/b.zipline",
                sha256 = "b".repeat(64),
                dependsOnIds = listOf("moduleC")
            ),
            "moduleC" to ModuleMetadata(
                url = "https://example.com/c.zipline",
                sha256 = "c".repeat(64),
                dependsOnIds = emptyList()
            )
        )
        
        // Act
        val order = resolver.resolveOrder(modules)
        
        // Assert
        assertEquals(3, order.size)
        assertTrue(order.indexOf("moduleC") < order.indexOf("moduleB"))
        assertTrue(order.indexOf("moduleB") < order.indexOf("moduleA"))
    }
    
    @Test
    fun `resolveOrder with diamond dependency`() {
        // Arrange: A depends on B and C, both B and C depend on D
        // Expected: D first, then B and C (in any order), then A
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = listOf("moduleB", "moduleC")
            ),
            "moduleB" to ModuleMetadata(
                url = "https://example.com/b.zipline",
                sha256 = "b".repeat(64),
                dependsOnIds = listOf("moduleD")
            ),
            "moduleC" to ModuleMetadata(
                url = "https://example.com/c.zipline",
                sha256 = "c".repeat(64),
                dependsOnIds = listOf("moduleD")
            ),
            "moduleD" to ModuleMetadata(
                url = "https://example.com/d.zipline",
                sha256 = "d".repeat(64),
                dependsOnIds = emptyList()
            )
        )
        
        // Act
        val order = resolver.resolveOrder(modules)
        
        // Assert
        assertEquals(4, order.size)
        // D must come first
        assertEquals("moduleD", order[0])
        // B and C must come before A
        assertTrue(order.indexOf("moduleB") < order.indexOf("moduleA"))
        assertTrue(order.indexOf("moduleC") < order.indexOf("moduleA"))
        // A must come last
        assertEquals("moduleA", order[3])
    }
    
    @Test
    fun `resolveOrder with multiple independent modules`() {
        // Arrange: Three modules with no dependencies
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = emptyList()
            ),
            "moduleB" to ModuleMetadata(
                url = "https://example.com/b.zipline",
                sha256 = "b".repeat(64),
                dependsOnIds = emptyList()
            ),
            "moduleC" to ModuleMetadata(
                url = "https://example.com/c.zipline",
                sha256 = "c".repeat(64),
                dependsOnIds = emptyList()
            )
        )
        
        // Act
        val order = resolver.resolveOrder(modules)
        
        // Assert
        assertEquals(3, order.size)
        assertTrue(order.containsAll(listOf("moduleA", "moduleB", "moduleC")))
    }
    
    @Test(expected = ZiplineError.CircularDependencyException::class)
    fun `resolveOrder throws CircularDependencyException for simple cycle`() {
        // Arrange: A depends on B, B depends on A (cycle)
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = listOf("moduleB")
            ),
            "moduleB" to ModuleMetadata(
                url = "https://example.com/b.zipline",
                sha256 = "b".repeat(64),
                dependsOnIds = listOf("moduleA")
            )
        )
        
        // Act & Assert (exception expected)
        resolver.resolveOrder(modules)
    }
    
    @Test(expected = ZiplineError.CircularDependencyException::class)
    fun `resolveOrder throws CircularDependencyException for three-node cycle`() {
        // Arrange: A depends on B, B depends on C, C depends on A (cycle)
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = listOf("moduleB")
            ),
            "moduleB" to ModuleMetadata(
                url = "https://example.com/b.zipline",
                sha256 = "b".repeat(64),
                dependsOnIds = listOf("moduleC")
            ),
            "moduleC" to ModuleMetadata(
                url = "https://example.com/c.zipline",
                sha256 = "c".repeat(64),
                dependsOnIds = listOf("moduleA")
            )
        )
        
        // Act & Assert (exception expected)
        resolver.resolveOrder(modules)
    }
    
    @Test
    fun `resolveOrder with complex graph`() {
        // Arrange: Complex dependency graph with multiple levels
        // E depends on D and C
        // D depends on B
        // C depends on B and A
        // B depends on A
        // A has no dependencies
        // Expected order: A, B, (C or D), (D or C), E
        val modules = mapOf(
            "moduleA" to ModuleMetadata(
                url = "https://example.com/a.zipline",
                sha256 = "a".repeat(64),
                dependsOnIds = emptyList()
            ),
            "moduleB" to ModuleMetadata(
                url = "https://example.com/b.zipline",
                sha256 = "b".repeat(64),
                dependsOnIds = listOf("moduleA")
            ),
            "moduleC" to ModuleMetadata(
                url = "https://example.com/c.zipline",
                sha256 = "c".repeat(64),
                dependsOnIds = listOf("moduleA", "moduleB")
            ),
            "moduleD" to ModuleMetadata(
                url = "https://example.com/d.zipline",
                sha256 = "d".repeat(64),
                dependsOnIds = listOf("moduleB")
            ),
            "moduleE" to ModuleMetadata(
                url = "https://example.com/e.zipline",
                sha256 = "e".repeat(64),
                dependsOnIds = listOf("moduleC", "moduleD")
            )
        )
        
        // Act
        val order = resolver.resolveOrder(modules)
        
        // Assert
        assertEquals(5, order.size)
        // A must come first
        assertEquals("moduleA", order[0])
        // B must come after A
        assertTrue(order.indexOf("moduleB") > order.indexOf("moduleA"))
        // C must come after A and B
        assertTrue(order.indexOf("moduleC") > order.indexOf("moduleA"))
        assertTrue(order.indexOf("moduleC") > order.indexOf("moduleB"))
        // D must come after B
        assertTrue(order.indexOf("moduleD") > order.indexOf("moduleB"))
        // E must come last (after C and D)
        assertEquals("moduleE", order[4])
        assertTrue(order.indexOf("moduleE") > order.indexOf("moduleC"))
        assertTrue(order.indexOf("moduleE") > order.indexOf("moduleD"))
    }
    
    @Test
    fun `resolveOrder with empty modules map`() {
        // Arrange
        val modules = emptyMap<String, ModuleMetadata>()
        
        // Act
        val order = resolver.resolveOrder(modules)
        
        // Assert
        assertTrue(order.isEmpty())
    }
}
