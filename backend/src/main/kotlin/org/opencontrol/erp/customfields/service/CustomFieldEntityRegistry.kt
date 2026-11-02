package org.opencontrol.erp.customfields.service

import org.opencontrol.erp.customfields.exception.UnsupportedTargetEntityException
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Descriptor defining an entity type that supports dynamic custom fields.
 */
data class TargetEntityDescriptor(
    val entityCode: String,
    val displayName: String,
    val module: String,
    val description: String? = null,
)

/**
 * Decoupled SPI and Registry holding all entity types eligible for custom fields.
 * Core modules and installed plugins register their target entities here.
 */
@Component
class CustomFieldEntityRegistry {
    private val registry = ConcurrentHashMap<String, TargetEntityDescriptor>()

    init {
        // Core standard entities registered by default
        register(
            TargetEntityDescriptor(
                entityCode = "CUSTOMER",
                displayName = "Customer",
                module = "CRM",
                description = "Customer and client entity profiles",
            ),
        )
        register(
            TargetEntityDescriptor(
                entityCode = "SUPPLIER",
                displayName = "Supplier",
                module = "PURCHASING",
                description = "Vendor and supplier master records",
            ),
        )
        register(
            TargetEntityDescriptor(
                entityCode = "PRODUCT",
                displayName = "Product",
                module = "CATALOG",
                description = "Products, services, and inventory items",
            ),
        )
    }

    fun register(descriptor: TargetEntityDescriptor) {
        registry[descriptor.entityCode.uppercase()] = descriptor
    }

    fun listEntities(): List<TargetEntityDescriptor> = registry.values.sortedBy { it.displayName }

    fun getEntity(entityCode: String): TargetEntityDescriptor? = registry[entityCode.uppercase()]

    fun isRegistered(entityCode: String): Boolean = registry.containsKey(entityCode.uppercase())

    fun validateRegistered(entityCode: String) {
        if (!isRegistered(entityCode)) {
            throw UnsupportedTargetEntityException(entityCode)
        }
    }
}
