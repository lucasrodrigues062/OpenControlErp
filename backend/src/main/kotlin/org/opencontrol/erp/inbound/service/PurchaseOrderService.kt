package org.opencontrol.erp.inbound.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.inbound.api.dto.CreatePurchaseOrderRequest
import org.opencontrol.erp.inbound.api.dto.PurchaseOrderResponse
import org.opencontrol.erp.inbound.domain.model.PurchaseOrder
import org.opencontrol.erp.inbound.domain.model.PurchaseOrderItem
import org.opencontrol.erp.inbound.domain.model.PurchaseOrderStatus
import org.opencontrol.erp.inbound.repository.PurchaseOrderRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class PurchaseOrderService(
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listPurchaseOrders(branchId: UUID? = null, status: String? = null): List<PurchaseOrderResponse> {
        val tenantId = getCurrentTenantId()
        val orders = if (branchId != null) {
            purchaseOrderRepository.findAllByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId, branchId)
        } else {
            purchaseOrderRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
        }

        val filtered = if (!status.isNullOrBlank()) {
            val s = PurchaseOrderStatus.valueOf(status.trim().uppercase())
            orders.filter { it.status == s }
        } else {
            orders
        }

        return filtered.map { mapToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getPurchaseOrderById(id: UUID): PurchaseOrderResponse {
        val tenantId = getCurrentTenantId()
        val po = purchaseOrderRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { IllegalArgumentException("Pedido de compra '$id' não encontrado.") }
        return mapToResponse(po)
    }

    fun createPurchaseOrder(request: CreatePurchaseOrderRequest): PurchaseOrderResponse {
        val tenantId = getCurrentTenantId()
        val orderNumber = request.orderNumber?.trim()?.ifBlank { null } ?: generateOrderNumber(tenantId)

        if (purchaseOrderRepository.existsByTenantIdAndOrderNumber(tenantId, orderNumber)) {
            throw IllegalArgumentException("Pedido de compra com número '$orderNumber' já existe.")
        }

        val purchaseOrder = PurchaseOrder(
            tenantId = tenantId,
            branchId = request.branchId,
            orderNumber = orderNumber,
            supplierId = request.supplierId,
            orderDate = request.orderDate,
            expectedDeliveryDate = request.expectedDeliveryDate,
            status = PurchaseOrderStatus.DRAFT,
            paymentTerms = request.paymentTerms,
            notes = request.notes,
        )

        var totalOrderAmount = BigDecimal.ZERO

        request.items.forEach { itemReq ->
            val lineTotal = (itemReq.unitPrice.multiply(itemReq.quantityOrdered))
                .subtract(itemReq.discountAmount)
                .setScale(4, java.math.RoundingMode.HALF_UP)
            val item = PurchaseOrderItem(
                tenantId = tenantId,
                purchaseOrder = purchaseOrder,
                productId = itemReq.productId,
                targetWarehouseId = itemReq.targetWarehouseId,
                quantityOrdered = itemReq.quantityOrdered,
                quantityReceived = BigDecimal.ZERO,
                unitPrice = itemReq.unitPrice,
                discountAmount = itemReq.discountAmount,
                totalPrice = lineTotal,
            )
            purchaseOrder.items.add(item)
            totalOrderAmount = totalOrderAmount.add(lineTotal)
        }

        purchaseOrder.totalAmount = totalOrderAmount.setScale(4, java.math.RoundingMode.HALF_UP)
        val saved = purchaseOrderRepository.save(purchaseOrder)

        auditService.recordAudit(
            entityType = "PurchaseOrder",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created Purchase Order ${saved.orderNumber} with total R$ ${saved.totalAmount}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    fun approvePurchaseOrder(id: UUID): PurchaseOrderResponse {
        val tenantId = getCurrentTenantId()
        val po = purchaseOrderRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { IllegalArgumentException("Pedido de compra '$id' não encontrado.") }

        if (po.status != PurchaseOrderStatus.DRAFT) {
            throw IllegalStateException("Apenas pedidos em rascunho (DRAFT) podem ser aprovados. Status atual: ${po.status}")
        }

        po.status = PurchaseOrderStatus.APPROVED
        po.updatedAt = Instant.now()
        val saved = purchaseOrderRepository.save(po)

        auditService.recordAudit(
            entityType = "PurchaseOrder",
            entityId = saved.id.toString(),
            action = "APPROVE",
            details = "Approved Purchase Order ${saved.orderNumber}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    fun cancelPurchaseOrder(id: UUID): PurchaseOrderResponse {
        val tenantId = getCurrentTenantId()
        val po = purchaseOrderRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { IllegalArgumentException("Pedido de compra '$id' não encontrado.") }

        if (po.status == PurchaseOrderStatus.FULLY_RECEIVED) {
            throw IllegalStateException("Pedidos já recebidos totalmente não podem ser cancelados.")
        }

        po.status = PurchaseOrderStatus.CANCELLED
        po.updatedAt = Instant.now()
        val saved = purchaseOrderRepository.save(po)

        auditService.recordAudit(
            entityType = "PurchaseOrder",
            entityId = saved.id.toString(),
            action = "CANCEL",
            details = "Cancelled Purchase Order ${saved.orderNumber}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    private fun mapToResponse(po: PurchaseOrder): PurchaseOrderResponse {
        val tenantId = po.tenantId
        val supplierName = customerRepository.findByIdAndTenantId(po.supplierId, tenantId).map { c: org.opencontrol.erp.crm.domain.model.Customer -> c.legalName }.orElse(null)
        val productIds = po.items.map { it.productId }.toSet()
        val products = productRepository.findAllById(productIds)
        val itemDetails = products.associate { it.id to Pair(it.name, it.sku) }
        return PurchaseOrderResponse.fromEntity(po, supplierName, itemDetails)
    }

    private fun generateOrderNumber(tenantId: UUID): String {
        val randomNum = SecureRandom().nextInt(900000) + 100000
        return "PC-$randomNum"
    }
}
