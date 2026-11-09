package org.opencontrol.erp.inbound.api.dto

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.opencontrol.erp.inbound.domain.model.InboundInvoice
import org.opencontrol.erp.inbound.domain.model.InboundInvoiceItem
import org.opencontrol.erp.inbound.domain.model.InboundInvoiceStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class InboundInvoiceItemResponse(
    val id: UUID,
    val itemIndex: Int,
    val supplierItemCode: String,
    val supplierItemDescription: String,
    val matchedProductId: UUID?,
    val matchedProductName: String? = null,
    val matchedProductSku: String? = null,
    val supplierUom: String,
    val internalUom: String,
    val conversionFactor: BigDecimal,
    val invoicedQuantity: BigDecimal,
    val internalQuantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val allocatedFreight: BigDecimal,
    val allocatedInsurance: BigDecimal,
    val allocatedExpenses: BigDecimal,
    val icmsAmount: BigDecimal,
    val icmsCreditAmount: BigDecimal,
    val ipiAmount: BigDecimal,
    val pisAmount: BigDecimal,
    val pisCreditAmount: BigDecimal,
    val cofinsAmount: BigDecimal,
    val cofinsCreditAmount: BigDecimal,
    val finalLandedUnitCost: BigDecimal,
    val finalTotalCost: BigDecimal,
) {
    companion object {
        fun fromEntity(item: InboundInvoiceItem, productName: String? = null, productSku: String? = null) =
            InboundInvoiceItemResponse(
                id = item.id,
                itemIndex = item.itemIndex,
                supplierItemCode = item.supplierItemCode,
                supplierItemDescription = item.supplierItemDescription,
                matchedProductId = item.matchedProductId,
                matchedProductName = productName,
                matchedProductSku = productSku,
                supplierUom = item.supplierUom,
                internalUom = item.internalUom,
                conversionFactor = item.conversionFactor,
                invoicedQuantity = item.invoicedQuantity,
                internalQuantity = item.internalQuantity,
                unitPrice = item.unitPrice,
                totalPrice = item.totalPrice,
                allocatedFreight = item.allocatedFreight,
                allocatedInsurance = item.allocatedInsurance,
                allocatedExpenses = item.allocatedExpenses,
                icmsAmount = item.icmsAmount,
                icmsCreditAmount = item.icmsCreditAmount,
                ipiAmount = item.ipiAmount,
                pisAmount = item.pisAmount,
                pisCreditAmount = item.pisCreditAmount,
                cofinsAmount = item.cofinsAmount,
                cofinsCreditAmount = item.cofinsCreditAmount,
                finalLandedUnitCost = item.finalLandedUnitCost,
                finalTotalCost = item.finalTotalCost,
            )
    }
}

data class InboundInvoiceResponse(
    val id: UUID,
    val branchId: UUID,
    val supplierId: UUID,
    val supplierName: String? = null,
    val costFormulaProfileId: UUID?,
    val costFormulaProfileName: String? = null,
    val purchaseOrderId: UUID?,
    val accessKey: String?,
    val invoiceNumber: String,
    val series: String,
    val issuanceDate: LocalDate,
    val entryDate: LocalDate,
    val totalProducts: BigDecimal,
    val totalFreight: BigDecimal,
    val totalInsurance: BigDecimal,
    val totalExpenses: BigDecimal,
    val totalDiscount: BigDecimal,
    val totalInvoiceAmount: BigDecimal,
    val status: InboundInvoiceStatus,
    val items: List<InboundInvoiceItemResponse>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(
            inv: InboundInvoice,
            supplierName: String? = null,
            profileName: String? = null,
            itemDetails: Map<UUID, Pair<String, String>> = emptyMap(),
        ) = InboundInvoiceResponse(
            id = inv.id,
            branchId = inv.branchId,
            supplierId = inv.supplierId,
            supplierName = supplierName,
            costFormulaProfileId = inv.costFormulaProfileId,
            costFormulaProfileName = profileName,
            purchaseOrderId = inv.purchaseOrderId,
            accessKey = inv.accessKey,
            invoiceNumber = inv.invoiceNumber,
            series = inv.series,
            issuanceDate = inv.issuanceDate,
            entryDate = inv.entryDate,
            totalProducts = inv.totalProducts,
            totalFreight = inv.totalFreight,
            totalInsurance = inv.totalInsurance,
            totalExpenses = inv.totalExpenses,
            totalDiscount = inv.totalDiscount,
            totalInvoiceAmount = inv.totalInvoiceAmount,
            status = inv.status,
            items = inv.items.map {
                val detail = it.matchedProductId?.let { id -> itemDetails[id] }
                InboundInvoiceItemResponse.fromEntity(it, detail?.first, detail?.second)
            },
            createdAt = inv.createdAt,
            updatedAt = inv.updatedAt,
        )
    }
}

data class MatchProductRequest(
    @field:NotNull
    val productId: UUID,
    @field:NotNull
    @field:Positive
    val conversionFactor: BigDecimal = BigDecimal.ONE,
    val supplierBarcode: String? = null,
)

data class FinalizeInvoiceStockRequest(
    @field:NotNull
    val warehouseId: UUID,
    val conferenceNotes: String? = null,
)
