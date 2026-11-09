package org.opencontrol.erp.inbound.api.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.opencontrol.erp.inbound.domain.model.StockTransfer
import org.opencontrol.erp.inbound.domain.model.StockTransferItem
import org.opencontrol.erp.inbound.domain.model.StockTransferStatus
import org.opencontrol.erp.inbound.domain.model.StockTransferType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CreateTransferItemRequest(
    @field:NotNull
    val productId: UUID,
    @field:NotNull
    @field:Positive
    val quantity: BigDecimal,
    val lotId: UUID? = null,
    val serialNumbers: List<String> = emptyList(),
)

data class CreateTransferRequest(
    val transferNumber: String? = null,
    @field:NotNull
    val transferType: StockTransferType = StockTransferType.INTERNAL_WAREHOUSE,
    @field:NotNull
    val originBranchId: UUID,
    @field:NotNull
    val originWarehouseId: UUID,
    @field:NotNull
    val destinationBranchId: UUID,
    @field:NotNull
    val destinationWarehouseId: UUID,
    val trackingNotes: String? = null,
    @field:NotEmpty
    val items: List<CreateTransferItemRequest>,
)

data class StockTransferItemResponse(
    val id: UUID,
    val productId: UUID,
    val productName: String? = null,
    val productSku: String? = null,
    val requestedQuantity: BigDecimal,
    val shippedQuantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val unitCost: BigDecimal,
    val lotId: UUID? = null,
    val lotNumber: String? = null,
    val serialNumbers: List<String> = emptyList(),
) {
    companion object {
        fun fromEntity(
            item: StockTransferItem,
            productName: String? = null,
            productSku: String? = null,
            lotNumber: String? = null,
            serialNumbers: List<String> = emptyList(),
        ) = StockTransferItemResponse(
            id = item.id,
            productId = item.productId,
            productName = productName,
            productSku = productSku,
            requestedQuantity = item.requestedQuantity,
            shippedQuantity = item.shippedQuantity,
            receivedQuantity = item.receivedQuantity,
            unitCost = item.unitCost,
            lotId = item.lotId,
            lotNumber = lotNumber,
            serialNumbers = serialNumbers,
        )
    }
}

data class StockTransferResponse(
    val id: UUID,
    val transferNumber: String,
    val transferType: StockTransferType,
    val originBranchId: UUID,
    val originBranchName: String? = null,
    val originWarehouseId: UUID,
    val originWarehouseName: String? = null,
    val destinationBranchId: UUID,
    val destinationBranchName: String? = null,
    val destinationWarehouseId: UUID,
    val destinationWarehouseName: String? = null,
    val status: StockTransferStatus,
    val dispatchedAt: Instant? = null,
    val receivedAt: Instant? = null,
    val trackingNotes: String? = null,
    val items: List<StockTransferItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
)
