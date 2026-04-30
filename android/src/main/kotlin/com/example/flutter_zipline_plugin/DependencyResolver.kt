package com.example.flutter_zipline_plugin

import com.example.flutter_zipline_plugin.models.ModuleMetadata
import com.example.flutter_zipline_plugin.errors.ZiplineError
import java.util.ArrayDeque

/**
 * Resolves module dependencies and computes topological ordering.
 * 
 */
class DependencyResolver {
    
    /**
     * Resolve the loading order for a set of modules.
     * 
     * This method computes a topological ordering of the module graph using Kahn's algorithm.
     * The returned list ensures that for every module M with dependency D, D appears before M.
     * 
     * @param modules Map of module IDs to their metadata
     * @return List of module IDs in topological order (dependencies before dependents)
     * @throws ZiplineError.CircularDependencyException if a circular dependency is detected
     * 
     */
    fun resolveOrder(modules: Map<String, ModuleMetadata>): List<String> {
        val graph = buildGraph(modules)
        val order = topologicalSort(graph)
        
        if (order == null) {
            throw ZiplineError.CircularDependencyException(
                moduleIds = modules.keys.toList(),
                message = "Circular dependency detected in module graph"
            )
        }
        
        return order
    }
    
    /**
     * Build a dependency graph from module metadata.
     * 
     * @param modules Map of module IDs to their metadata
     * @return Adjacency list representation of the dependency graph
     */
    private fun buildGraph(modules: Map<String, ModuleMetadata>): Map<String, List<String>> {
        return modules.mapValues { (_, metadata) -> 
            metadata.dependsOnIds 
        }
    }
    
    /**
     * Perform topological sort using Kahn's algorithm.
     * 
     * @param graph Adjacency list representation of the dependency graph
     * @return List of module IDs in topological order, or null if a cycle is detected
     */
    private fun topologicalSort(graph: Map<String, List<String>>): List<String>? {
        val inDegree = mutableMapOf<String, Int>()
        
        graph.forEach { (module, dependencies) ->
            inDegree[module] = dependencies.size
        }
        
        val queue = ArrayDeque<String>()

        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }
        
        val result = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)
            
            graph.forEach { (module, dependencies) ->
                if (dependencies.contains(node)) {
                    inDegree[module] = inDegree[module]!! - 1
                    
                    if (inDegree[module] == 0) {
                        queue.add(module)
                    }
                }
            }
        }
        
        return if (result.size == graph.size) result else null
    }
}
