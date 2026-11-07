package org.opencontrol.erp.inventory.domain.model

/**
 * Storage warehouse functional classification.
 */
enum class WarehouseType {
    COMMERCIAL,
    STORAGE,
    QUARANTINE,
    TRANSIT,
}

/**
 * Lifecycle and dispatch status for batch/lot traceability.
 */
enum class LotStatus {
    RELEASED,
    QUARANTINE,
    BLOCKED,
    EXPIRED,
}

/**
 * Individual asset lifecycle status for serialized products.
 */
enum class SerialNumberStatus {
    IN_STOCK,
    RESERVED,
    DISPATCHED_SOLD,
    RMA_MAINTENANCE,
    WRITTEN_OFF,
}

/**
 * Double-entry Kardex stock transaction types.
 */
enum class StockMovementType(
    val isInbound: Boolean,
) {
    INBOUND_PURCHASE(true),
    OUTBOUND_SALE(false),
    TRANSFER_OUT(false),
    TRANSFER_IN(true),
    ADJUSTMENT_INVENTORY(false),
    RETURN_CUSTOMER(true),
    RETURN_SUPPLIER(false),
}
