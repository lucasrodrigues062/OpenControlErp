package org.opencontrol.erp.kernel.spi

/**
 * Base marker interface for all pluggable Service Provider Interfaces (SPIs).
 */
interface ExtensionPoint {
    val extensionPointId: String
        get() = this::class.qualifiedName ?: this::class.java.name
}

/**
 * Contract to evaluate whether an SPI provider is applicable for the current execution context.
 */
fun interface ContextMatcher {
    fun matches(context: ExecutionContext): Boolean
}
