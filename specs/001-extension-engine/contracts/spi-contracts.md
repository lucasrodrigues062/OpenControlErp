# Core SPI Contracts & Interfaces

This document specifies the primary Kotlin/JVM interfaces forming the Service Provider Interface (SPI) foundation of OpenControl ERP. All contracts are 100% callable from both Kotlin and Java plugins.

## 1. Marker and Lifecycle Contracts

### `ExtensionPoint`
All SPI contracts in OpenControl ERP must extend this marker interface.

```kotlin
package org.opencontrol.erp.kernel.spi

/**
 * Marker interface for all pluggable Service Provider Interfaces (SPIs).
 */
interface ExtensionPoint {
    /**
     * Unique identifier for this extension contract.
     */
    val extensionPointId: String
        get() = this::class.qualifiedName ?: this::class.java.name
}
```

### `PluginLifecycle`
Optional interface implemented by an extension's main class to execute custom startup and shutdown routines.

```kotlin
package org.opencontrol.erp.kernel.plugin

interface PluginLifecycle {
    /**
     * Invoked when the extension is activated by the administrator.
     */
    fun onActivate(context: PluginContext)

    /**
     * Invoked when the extension is deactivated. Resources must be released.
     */
    fun onDeactivate(context: PluginContext)
}
```

---

## 2. Contextual Resolution

### `ContextMatcher`
Evaluates whether a registered provider matches the current business execution context.

```kotlin
package org.opencontrol.erp.kernel.spi

interface ContextMatcher {
    /**
     * Returns true if this provider is applicable for the given execution context.
     *
     * @param context Contextual properties (e.g., country="BR", tenantId=..., currency="BRL")
     */
    fun matches(context: ExecutionContext): Boolean
}
```

---

## 3. Example Pluggable Domain SPIs

### `FiscalEngineSPI`
Pluggable contract implemented by national fiscal adapters (e.g., Brazilian DFe adapter).

```kotlin
package org.opencontrol.erp.fiscal.spi

import org.opencontrol.erp.kernel.spi.ExtensionPoint
import org.opencontrol.erp.kernel.spi.ContextMatcher

interface FiscalEngineSPI : ExtensionPoint, ContextMatcher {

    /**
     * Dispatches an invoice/document for statutory fiscal authorization.
     */
    fun issueDocument(request: FiscalDocumentRequest): FiscalIssuanceResult

    /**
     * Requests document cancellation with the tax authority.
     */
    fun cancelDocument(request: FiscalCancellationRequest): FiscalCancellationResult

    /**
     * Checks current availability status of the external tax authority endpoint.
     */
    fun checkStatus(context: FiscalContext): FiscalHealthStatus
}
```

### `DomainEventListener`
Interface for event bus consumers.

```kotlin
package org.opencontrol.erp.kernel.event

interface DomainEventListener<in T : DomainEvent> {

    /**
     * Handles the published domain event.
     */
    fun onEvent(event: T)

    /**
     * Execution mode: SYNC_PRE_COMMIT or ASYNC_POST_COMMIT.
     */
    val mode: EventMode
        get() = EventMode.ASYNC_POST_COMMIT
}
```
