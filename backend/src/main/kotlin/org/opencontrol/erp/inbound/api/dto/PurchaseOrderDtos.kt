package org.opencontrol.erp.inbound.api.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.opencontrol.erp.inbound.domain.model.PurchaseOrder
import org.opencontrol.erp.inbound.domain.model.PurchaseOrderItem
import org.opencontrol.erp.inbound.domain.model.PurchaseOrderStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreatePurchaseOrderItemRequest(
    @field:NotNull
    val productId: UUID,
    val targetWarehouseId: UUID? = null,
    @field:NotNull
    @field:Positive
    val quantityOrdered: BigDecimal,
    @field:NotNull
    @field:Positive
    val unitPrice: BigDecimal,
    val discountAmount: BigDecimal = BigDecimal.ZERO,
)

data class CreatePurchaseOrderRequest(
    val orderNumber: String? = null,
    @field:NotNull
    val branchId: UUID,
    @field:NotNull
    val supplierId: UUID,
    val orderDate: LocalDate = LocalDate.now(),
    val expectedDeliveryDate: LocalDate? = null,
    val paymentTerms: String? = null,
    val notes: String? = null,
    @field:NotEmpty
    val items: List<CreatePurchaseOrderItemRequest>,
)

data class PurchaseOrderItemResponse(
    val id: UUID,
    val productId: UUID,
    val productName: String? = null,
    val productSku: String? = null,
    val targetWarehouseId: UUID?,
    val quantityOrdered: BigDecimal,
    val quantityReceived: BigDecimal,
    val unitPrice: BigDecimal,
    val discountAmount: BigDecimal,
    val totalPrice: BigDecimal,
) {
    companion object {
        fun fromEntity(item: PurchaseOrderItem, productName: String? = null, productSku: String? = null) =
            PurchaseOrderItemResponse(
                id = item.id,
                productId = item.productId,
                productName = productName,
                productSku = productSku,
                targetWarehouseId = item.targetWarehouseId,
                quantityOrdered = item.quantityOrdered,
                quantityReceived = item.quantityReceived,
                unitPrice = item.unitPrice,
                discountAmount = item.discountAmount,
                totalPrice = item.totalPrice,
            )
    }
}

data class PurchaseOrderResponse(
    val id: UUID,
    val branchId: UUID,
    val orderNumber: String,
    val supplierId: UUID,
    val supplierName: String? = null,
    val orderDate: LocalDate,
    val expectedDeliveryDate: LocalDate?,
    val status: PurchaseOrderStatus,
    val totalAmount: BigDecimal,
    val currency: String,
    val paymentTerms: String?,
    val notes: String?,
    val items: List<PurchaseOrderItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(
            po: PurchaseOrder,
            supplierName: String? = null,
            itemDetails: Map<UUID, Pair<String, String>> = emptyMap(),
        ) = PurchaseOrderResponse(
            id = po.id,
            branchId = po.branchId,
            orderNumber = po.orderNumber,
            supplierId = po.supplierId,
            supplierName = supplierName,
            orderDate = po.orderDate,
            expectedDeliveryDate = po.expectedDeliveryDate,
            status = po.status,
            totalAmount = po.totalAmount,
            currency = po.currency,
            paymentTerms = po.paymentTerms,
            notes = po.notes,
            items = po.items.map {
                val detail = itemDetails[it.productId]
                PurchaseOrderItemResponse.fromEntity(it, detail?.first, detail?.second)
            },
            createdAt = po.createdAt,
            updatedAt = po.updatedAt,
        )
    }
}
