package org.opencontrol.erp.kernel.plugin.service

import org.opencontrol.erp.kernel.exception.PluginDependencyException
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.springframework.stereotype.Component

@Component
class DependencyResolver {
    /**
     * Resolves activation order using Kahn's algorithm for topological sorting.
     * Throws PluginDependencyException if a circular dependency is detected.
     */
    fun resolveActivationOrder(plugins: List<ExtensionPlugin>): List<ExtensionPlugin> {
        val pluginMap = plugins.associateBy { it.pluginId }
        val inDegree = mutableMapOf<String, Int>()
        val adjList = mutableMapOf<String, MutableList<String>>()

        plugins.forEach { plugin ->
            inDegree[plugin.pluginId] = 0
            adjList[plugin.pluginId] = mutableListOf()
        }

        // Build directed graph: dependency -> dependent
        plugins.forEach { plugin ->
            plugin.dependencies.filter { !it.optional }.forEach { dep ->
                val depId = dep.dependsOnPluginId
                if (!pluginMap.containsKey(depId)) {
                    throw PluginDependencyException("Missing mandatory dependency '$depId' for plugin '${plugin.pluginId}'")
                }
                adjList[depId]?.add(plugin.pluginId)
                inDegree[plugin.pluginId] = (inDegree[plugin.pluginId] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        val sortedOrder = mutableListOf<ExtensionPlugin>()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            pluginMap[current]?.let { sortedOrder.add(it) }

            adjList[current]?.forEach { dependent ->
                val newDegree = (inDegree[dependent] ?: 1) - 1
                inDegree[dependent] = newDegree
                if (newDegree == 0) {
                    queue.add(dependent)
                }
            }
        }

        if (sortedOrder.size != plugins.size) {
            val cycleParticipants = inDegree.filter { it.value > 0 }.keys
            throw PluginDependencyException("Circular dependency detected involving plugins: ${cycleParticipants.joinToString()}")
        }

        return sortedOrder
    }

    /**
     * Validates that an active plugin has no other active plugins depending on it before deactivation.
     */
    fun validateCanDeactivate(
        pluginToDeactivate: ExtensionPlugin,
        allActivePlugins: List<ExtensionPlugin>,
    ) {
        val activeDependents =
            allActivePlugins.filter { active ->
                active.pluginId != pluginToDeactivate.pluginId &&
                    active.dependencies.any { dep -> !dep.optional && dep.dependsOnPluginId == pluginToDeactivate.pluginId }
            }

        if (activeDependents.isNotEmpty()) {
            val names = activeDependents.joinToString { "'${it.name}' (${it.pluginId})" }
            throw PluginDependencyException(
                "Cannot deactivate '${pluginToDeactivate.name}' because the following active plugins depend on it: $names",
            )
        }
    }
}
