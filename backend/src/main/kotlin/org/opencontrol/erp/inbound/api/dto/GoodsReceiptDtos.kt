package org.opencontrol.erp.inbound.api.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import org.opencontrol.erp.inbound.domain.model.GoodsReceipt
import org.opencontrol.erp.inbound.domain.model.GoodsReceiptItem
import org.opencontrol.erp.inbound.domain.model.GoodsReceiptStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class GoodsReceiptItemDto(
    @field:NotNull
    val productId: UUID,
    @field:NotNull
    @field:PositiveOrZero
    val expectedQuantity: BigDecimal,
    @field:NotNull
    @field:PositiveOrZero
    val receivedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    val lotNumber: String? = null,
    val fabricationDate: LocalDate? = null,
    val expirationDate: LocalDate? = null,
    val serialNumbers: List<String> = emptyList(),
)

data class SubmitConferenceRequest(
    @field:NotNull
    val branchId: UUID,
    @field:NotNull
    val warehouseId: UUID,
    val purchaseOrderId: UUID? = null,
    val inboundInvoiceId: UUID? = null,
    val conferenceNotes: String? = null,
    @field:NotEmpty
    val items: List<GoodsReceiptItemDto>,
)

data class GoodsReceiptItemResponse(
    val id: UUID,
    val productId: UUID,
    val productName: String? = null,
    val productSku: String? = null,
    val expectedQuantity: BigDecimal,
    val receivedQuantity: BigDecimal,
    val rejectedQuantity: BigDecimal,
    val hasDivergence: Boolean,
    val lotNumber: String?,
    val fabricationDate: LocalDate?,
    val expirationDate: LocalDate?,
    val serialNumbers: List<String>,
)

data class GoodsReceiptResponse(
    val id: UUID,
    val branchId: UUID,
    val warehouseId: UUID,
    val warehouseName: String? = null,
    val purchaseOrderId: UUID?,
    val inboundInvoiceId: UUID?,
    val receiptNumber: String,
    val receiptDate: LocalDate,
    val status: GoodsReceiptStatus,
    val conferenceNotes: String?,
    val items: List<GoodsReceiptItemResponse>,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(
            gr: GoodsReceipt,
            warehouseName: String? = null,
            productDetails: Map<UUID, Pair<String, String>> = emptyMap(),
        ) = GoodsReceiptResponse(
            id = gr.id,
            branchId = gr.branchId,
            warehouseId = gr.warehouseId,
            warehouseName = warehouseName,
            purchaseOrderId = gr.purchaseOrderId,
            inboundInvoiceId = gr.inboundInvoiceId,
            receiptNumber = gr.receiptNumber,
            receiptDate = gr.receiptDate,
            status = gr.status,
            conferenceNotes = gr.conferenceNotes,
            items = gr.items.map { it ->
                val prod = productDetails[it.productId]
                GoodsReceiptItemResponse(
                    id = it.id,
                    productId = it.productId,
                    productName = prod?.first,
                    productSku = prod?.second,
                    expectedQuantity = it.expectedQuantity,
                    receivedQuantity = it.receivedQuantity,
                    rejectedQuantity = it.rejectedQuantity,
                    hasDivergence = it.expectedQuantity.compareTo(it.receivedQuantity) != 0,
                    lotNumber = it.lotNumber,
                    fabricationDate = it.fabricationDate,
                    expirationDate = it.expirationDate,
                    serialNumbers = emptyList(),
                )
            },
            createdAt = gr.createdAt,
        )
    }
}
