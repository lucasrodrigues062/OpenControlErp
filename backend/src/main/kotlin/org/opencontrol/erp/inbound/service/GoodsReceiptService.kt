package org.opencontrol.erp.inbound.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.inbound.api.dto.GoodsReceiptResponse
import org.opencontrol.erp.inbound.api.dto.SubmitConferenceRequest
import org.opencontrol.erp.inbound.domain.model.GoodsReceipt
import org.opencontrol.erp.inbound.domain.model.GoodsReceiptItem
import org.opencontrol.erp.inbound.domain.model.GoodsReceiptStatus
import org.opencontrol.erp.inbound.repository.GoodsReceiptRepository
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.ProductSerialNumber
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class GoodsReceiptService(
    private val goodsReceiptRepository: GoodsReceiptRepository,
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val productLotRepository: ProductLotRepository,
    private val productSerialNumberRepository: ProductSerialNumberRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listGoodsReceipts(): List<GoodsReceiptResponse> {
        val tenantId = getCurrentTenantId()
        return goodsReceiptRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId).map { mapToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): GoodsReceiptResponse {
        val tenantId = getCurrentTenantId()
        val gr = goodsReceiptRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { IllegalArgumentException("Conferência de recebimento '$id' não encontrada.") }
        return mapToResponse(gr)
    }

    fun submitConference(request: SubmitConferenceRequest): GoodsReceiptResponse {
        val tenantId = getCurrentTenantId()
        val receiptNumber = "REC-" + (SecureRandom().nextInt(900000) + 100000)

        var hasDivergence = false

        val receipt = GoodsReceipt(
            tenantId = tenantId,
            branchId = request.branchId,
            warehouseId = request.warehouseId,
            purchaseOrderId = request.purchaseOrderId,
            inboundInvoiceId = request.inboundInvoiceId,
            receiptNumber = receiptNumber,
            conferenceNotes = request.conferenceNotes,
        )

        request.items.forEach { itReq ->
            if (itReq.expectedQuantity.compareTo(itReq.receivedQuantity) != 0) {
                hasDivergence = true
            }

            val item = GoodsReceiptItem(
                tenantId = tenantId,
                goodsReceipt = receipt,
                productId = itReq.productId,
                expectedQuantity = itReq.expectedQuantity,
                receivedQuantity = itReq.receivedQuantity,
                rejectedQuantity = itReq.rejectedQuantity,
                lotNumber = itReq.lotNumber,
                fabricationDate = itReq.fabricationDate,
                expirationDate = itReq.expirationDate,
            )
            receipt.items.add(item)

            // Register lot if provided
            if (!itReq.lotNumber.isNullOrBlank()) {
                val existingLot = productLotRepository.findByTenantIdAndProductIdAndLotNumber(
                    tenantId, itReq.productId, itReq.lotNumber
                )
                if (existingLot.isEmpty) {
                    val newLot = ProductLot(
                        tenantId = tenantId,
                        warehouseId = request.warehouseId,
                        productId = itReq.productId,
                        lotNumber = itReq.lotNumber.trim().uppercase(),
                        manufacturingDate = itReq.fabricationDate ?: LocalDate.now(),
                        expirationDate = itReq.expirationDate ?: LocalDate.now().plusYears(1),
                        quantityOnHand = itReq.receivedQuantity,
                        status = LotStatus.RELEASED,
                    )
                    productLotRepository.save(newLot)
                }
            }

            // Register serial numbers if provided
            itReq.serialNumbers.forEach { snStr ->
                if (snStr.isNotBlank()) {
                    val existingSerial = productSerialNumberRepository.findByTenantIdAndProductIdAndSerialNumber(
                        tenantId, itReq.productId, snStr.trim().uppercase()
                    )
                    if (existingSerial.isEmpty) {
                        val newSerial = ProductSerialNumber(
                            tenantId = tenantId,
                            productId = itReq.productId,
                            serialNumber = snStr.trim().uppercase(),
                            warehouseId = request.warehouseId,
                            status = SerialNumberStatus.IN_STOCK,
                        )
                        productSerialNumberRepository.save(newSerial)
                    }
                }
            }
        }

        receipt.status = if (hasDivergence) {
            GoodsReceiptStatus.CONFERRED_WITH_DIVERGENCE
        } else {
            GoodsReceiptStatus.CONFERRED_OK
        }

        val saved = goodsReceiptRepository.save(receipt)

        auditService.recordAudit(
            entityType = "GoodsReceipt",
            entityId = saved.id.toString(),
            action = "SUBMIT_CONFERENCE",
            details = "Completed physical conference ${saved.receiptNumber} with status ${saved.status}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    private fun mapToResponse(gr: GoodsReceipt): GoodsReceiptResponse {
        val tenantId = gr.tenantId
        val warehouseName = warehouseRepository.findByIdAndTenantId(gr.warehouseId, tenantId).map { it.name }.orElse(null)
        val productIds = gr.items.map { it.productId }.toSet()
        val products = productRepository.findAllById(productIds)
        val productDetails = products.associate { it.id to Pair(it.name, it.sku) }
        return GoodsReceiptResponse.fromEntity(gr, warehouseName, productDetails)
    }
}
