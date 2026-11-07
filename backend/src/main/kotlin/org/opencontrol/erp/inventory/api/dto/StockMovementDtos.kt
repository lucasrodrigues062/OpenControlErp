package org.opencontrol.erp.inventory.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.StockMovement
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class StockBalanceResponse(
    val id: UUID,
    val warehouseId: UUID,
    val warehouseName: String? = null,
    val productId: UUID,
    val productSku: String? = null,
    val productName: String? = null,
    val locationId: UUID?,
    val locationAddress: String? = null,
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val quantityAvailable: BigDecimal,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(
            b: StockBalance,
            warehouseName: String? = null,
            productSku: String? = null,
            productName: String? = null,
            locationAddress: String? = null,
        ) = StockBalanceResponse(
            id = b.id,
            warehouseId = b.warehouseId,
            warehouseName = warehouseName,
            productId = b.productId,
            productSku = productSku,
            productName = productName,
            locationId = b.locationId,
            locationAddress = locationAddress,
            quantityOnHand = b.quantityOnHand,
            quantityReserved = b.quantityReserved,
            quantityAvailable = b.quantityAvailable,
            updatedAt = b.updatedAt,
        )
    }
}

data class CreateStockMovementRequest(
    @field:NotNull
    val branchId: UUID,
    @field:NotNull
    val movementType: StockMovementType,
    @field:NotNull
    val productId: UUID,
    val sourceWarehouseId: UUID? = null,
    val destinationWarehouseId: UUID? = null,
    val locationId: UUID? = null,
    val lotId: UUID? = null,
    val serialNumberId: UUID? = null,
    @field:NotNull
    @field:Positive
    val quantity: BigDecimal,
    val unitCost: BigDecimal? = null,
    val referenceDocument: String? = null,
    val reasonCode: String? = null,
    val notes: String? = null,
)

data class StockMovementResponse(
    val id: UUID,
    val branchId: UUID,
    val movementType: StockMovementType,
    val productId: UUID,
    val productSku: String? = null,
    val productName: String? = null,
    val sourceWarehouseId: UUID?,
    val sourceWarehouseName: String? = null,
    val destinationWarehouseId: UUID?,
    val destinationWarehouseName: String? = null,
    val locationId: UUID?,
    val lotId: UUID?,
    val serialNumberId: UUID?,
    val quantity: BigDecimal,
    val unitCost: BigDecimal,
    val balanceAfter: BigDecimal,
    val referenceDocument: String?,
    val reasonCode: String?,
    val notes: String?,
    val createdByUserId: UUID?,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(
            m: StockMovement,
            productSku: String? = null,
            productName: String? = null,
            sourceWarehouseName: String? = null,
            destinationWarehouseName: String? = null,
        ) = StockMovementResponse(
            id = m.id,
            branchId = m.branchId,
            movementType = m.movementType,
            productId = m.productId,
            productSku = productSku,
            productName = productName,
            sourceWarehouseId = m.sourceWarehouseId,
            sourceWarehouseName = sourceWarehouseName,
            destinationWarehouseId = m.destinationWarehouseId,
            destinationWarehouseName = destinationWarehouseName,
            locationId = m.locationId,
            lotId = m.lotId,
            serialNumberId = m.serialNumberId,
            quantity = m.quantity,
            unitCost = m.unitCost,
            balanceAfter = m.balanceAfter,
            referenceDocument = m.referenceDocument,
            reasonCode = m.reasonCode,
            notes = m.notes,
            createdByUserId = m.createdByUserId,
            createdAt = m.createdAt,
        )
    }
}

data class StockMovementPageResponse(
    val items: List<StockMovementResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)
