package org.opencontrol.erp.kernel.spi.service

import org.opencontrol.erp.kernel.spi.ContextMatcher
import org.opencontrol.erp.kernel.spi.ExecutionContext
import org.opencontrol.erp.kernel.spi.ExtensionPoint
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

data class RegisteredProvider<T : ExtensionPoint>(
    val provider: T,
    val priority: Int,
    val pluginId: String,
    val contextMatcher: ContextMatcher? = null,
)

@Service
class ExtensionRegistry {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val providers = ConcurrentHashMap<Class<out ExtensionPoint>, CopyOnWriteArrayList<RegisteredProvider<out ExtensionPoint>>>()
    private val defaultFallbacks = ConcurrentHashMap<Class<out ExtensionPoint>, ExtensionPoint>()

    fun <T : ExtensionPoint> registerProvider(
        spiInterface: Class<T>,
        provider: T,
        priority: Int = 100,
        pluginId: String = "core",
        contextMatcher: ContextMatcher? = null
    ) {
        val list = providers.computeIfAbsent(spiInterface) { CopyOnWriteArrayList() }
        list.add(RegisteredProvider(provider, priority, pluginId, contextMatcher))
        logger.info("Registered provider '${provider::class.qualifiedName}' for SPI '${spiInterface.name}' with priority $priority (plugin: $pluginId)")
    }

    fun <T : ExtensionPoint> registerDefaultFallback(spiInterface: Class<T>, fallback: T) {
        defaultFallbacks[spiInterface] = fallback
        logger.info("Registered default fallback '${fallback::class.qualifiedName}' for SPI '${spiInterface.name}'")
    }

    fun unregisterProvidersByPluginId(pluginId: String) {
        providers.values.forEach { list ->
            list.removeIf { it.pluginId == pluginId }
        }
        logger.info("Unregistered all SPI providers for plugin '$pluginId'")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ExtensionPoint> resolve(spiInterface: Class<T>, context: ExecutionContext): T {
        val candidates = providers[spiInterface] as? List<RegisteredProvider<T>>

        val matched = candidates
            ?.filter { it.contextMatcher == null || it.contextMatcher.matches(context) }
            ?.maxByOrNull { it.priority }
            ?.provider

        if (matched != null) {
            return matched
        }

        val fallback = defaultFallbacks[spiInterface] as? T
        if (fallback != null) {
            logger.debug("Resolved default fallback for SPI '${spiInterface.name}'")
            return fallback
        }

        throw IllegalStateException("No active provider or default fallback found for SPI: ${spiInterface.name}")
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : ExtensionPoint> resolveAll(spiInterface: Class<T>, context: ExecutionContext): List<T> {
        val candidates = providers[spiInterface] as? List<RegisteredProvider<T>> ?: emptyList()
        return candidates
            .filter { it.contextMatcher == null || it.contextMatcher.matches(context) }
            .sortedByDescending { it.priority }
            .map { it.provider }
    }
}
