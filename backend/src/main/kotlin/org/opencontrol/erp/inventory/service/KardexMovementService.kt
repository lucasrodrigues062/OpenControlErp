package org.opencontrol.erp.inventory.service

import jakarta.persistence.criteria.Predicate
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.service.PricingService
import org.opencontrol.erp.inventory.api.dto.CreateStockMovementRequest
import org.opencontrol.erp.inventory.api.dto.StockMovementPageResponse
import org.opencontrol.erp.inventory.api.dto.StockMovementResponse
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.opencontrol.erp.inventory.domain.model.StockMovement
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.exception.LotNotFoundException
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.inventory.repository.StockMovementRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class KardexMovementService(
    private val movementRepository: StockMovementRepository,
    private val balanceService: StockBalanceService,
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository,
    private val lotRepository: ProductLotRepository,
    private val serialRepository: ProductSerialNumberRepository,
    private val pricingService: PricingService,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    private fun getCurrentUserId(): UUID? =
        SecurityContextHolder.getContext()?.userId?.let {
            try {
                UUID.fromString(it)
            } catch (e: Exception) {
                null
            }
        }

    fun recordMovement(request: CreateStockMovementRequest): StockMovementResponse {
        val tenantId = getCurrentTenantId()
        val userId = getCurrentUserId()

        val product =
            productRepository
                .findByIdAndTenantId(request.productId, tenantId)
                .orElseThrow { IllegalArgumentException("Produto '${request.productId}' não encontrado.") }

        val unitCost = request.unitCost ?: product.averageCost
        val qty = request.quantity

        var finalBalanceAfter = BigDecimal.ZERO

        when (request.movementType) {
            StockMovementType.INBOUND_PURCHASE, StockMovementType.RETURN_CUSTOMER -> {
                val destId =
                    request.destinationWarehouseId
                        ?: throw IllegalArgumentException("Movimentação de entrada exige destinationWarehouseId.")

                // 1. Recalculate CMP (Weighted Average Cost)
                if (request.unitCost != null && request.unitCost > BigDecimal.ZERO) {
                    val currentBalances = balanceService.getProductBalances(product.id)
                    val totalCurrentQty = currentBalances.sumOf { it.quantityOnHand }

                    val newCmp =
                        pricingService.calculateWeightedAverageCost(
                            currentQty = totalCurrentQty,
                            currentCmp = product.averageCost,
                            inboundQty = qty,
                            inboundCost = request.unitCost,
                        )
                    product.averageCost = newCmp
                    product.lastAcquisitionCost = request.unitCost
                    productRepository.save(product)
                }

                // 2. Adjust physical balance
                val updatedBalance = balanceService.adjustStock(destId, product.id, request.locationId, qty)
                finalBalanceAfter = updatedBalance.quantityOnHand

                // 3. Traceability updates
                if (request.lotId != null) {
                    val lot =
                        lotRepository
                            .findByIdAndTenantId(request.lotId, tenantId)
                            .orElseThrow { LotNotFoundException("Lote '${request.lotId}' não encontrado.") }
                    lot.quantityOnHand = lot.quantityOnHand.add(qty)
                    lotRepository.save(lot)
                }
                if (request.serialNumberId != null) {
                    val serial = serialRepository.findByIdAndTenantId(request.serialNumberId, tenantId).orElse(null)
                    if (serial != null) {
                        serial.status = SerialNumberStatus.IN_STOCK
                        serial.warehouseId = destId
                        serialRepository.save(serial)
                    }
                }
            }

            StockMovementType.OUTBOUND_SALE, StockMovementType.RETURN_SUPPLIER -> {
                val sourceId =
                    request.sourceWarehouseId
                        ?: throw IllegalArgumentException("Movimentação de saída exige sourceWarehouseId.")

                // 1. Traceability verification
                if (request.lotId != null) {
                    val lot =
                        lotRepository
                            .findByIdAndTenantId(request.lotId, tenantId)
                            .orElseThrow { LotNotFoundException("Lote '${request.lotId}' não encontrado.") }
                    if (lot.expirationDate.isBefore(LocalDate.now()) || lot.status == LotStatus.EXPIRED) {
                        throw IllegalArgumentException("Não é permitido expedir lote vencido (${lot.lotNumber}).")
                    }
                    if (lot.quantityOnHand < qty) {
                        throw IllegalArgumentException("Saldo insuficiente no lote ${lot.lotNumber}. Disponível: ${lot.quantityOnHand}")
                    }
                    lot.quantityOnHand = lot.quantityOnHand.subtract(qty)
                    lotRepository.save(lot)
                }
                if (request.serialNumberId != null) {
                    val serial = serialRepository.findByIdAndTenantId(request.serialNumberId, tenantId).orElse(null)
                    if (serial != null) {
                        serial.status = SerialNumberStatus.DISPATCHED_SOLD
                        serialRepository.save(serial)
                    }
                }

                // 2. Adjust physical balance
                val updatedBalance = balanceService.adjustStock(sourceId, product.id, request.locationId, qty.negate())
                finalBalanceAfter = updatedBalance.quantityOnHand
            }

            StockMovementType.TRANSFER_OUT, StockMovementType.TRANSFER_IN -> {
                val sourceId =
                    request.sourceWarehouseId
                        ?: throw IllegalArgumentException("Transferência exige sourceWarehouseId.")
                val destId =
                    request.destinationWarehouseId
                        ?: throw IllegalArgumentException("Transferência exige destinationWarehouseId.")

                balanceService.adjustStock(sourceId, product.id, request.locationId, qty.negate())
                val destBalance = balanceService.adjustStock(destId, product.id, null, qty)
                finalBalanceAfter = destBalance.quantityOnHand
            }

            StockMovementType.ADJUSTMENT_INVENTORY -> {
                val warehouseId =
                    request.destinationWarehouseId ?: request.sourceWarehouseId
                        ?: throw IllegalArgumentException("Ajuste manual exige armazém de origem ou destino.")

                if (request.reasonCode.isNullOrBlank()) {
                    throw IllegalArgumentException("Ajuste manual de estoque exige código de justificativa (reasonCode).")
                }

                val delta = if (request.destinationWarehouseId != null) qty else qty.negate()
                val updated = balanceService.adjustStock(warehouseId, product.id, request.locationId, delta)
                finalBalanceAfter = updated.quantityOnHand
            }
        }

        val movement =
            StockMovement(
                tenantId = tenantId,
                branchId = request.branchId,
                movementType = request.movementType,
                productId = product.id,
                sourceWarehouseId = request.sourceWarehouseId,
                destinationWarehouseId = request.destinationWarehouseId,
                locationId = request.locationId,
                lotId = request.lotId,
                serialNumberId = request.serialNumberId,
                quantity = qty,
                unitCost = unitCost,
                balanceAfter = finalBalanceAfter,
                referenceDocument = request.referenceDocument,
                reasonCode = request.reasonCode,
                notes = request.notes,
                createdByUserId = userId,
            )

        val saved = movementRepository.save(movement)

        auditService.recordAudit(
            entityType = "StockMovement",
            entityId = saved.id.toString(),
            action = "KARDEX_RECORD",
            details = "Recorded ${saved.movementType} of $qty ${product.unitOfMeasure} for product ${product.sku}",
            stateAfter = saved,
        )

        val srcW = saved.sourceWarehouseId?.let { warehouseRepository.findById(it).orElse(null) }
        val dstW = saved.destinationWarehouseId?.let { warehouseRepository.findById(it).orElse(null) }

        return StockMovementResponse.fromEntity(
            m = saved,
            productSku = product.sku,
            productName = product.name,
            sourceWarehouseName = srcW?.name,
            destinationWarehouseName = dstW?.name,
        )
    }

    @Transactional(readOnly = true)
    fun listMovements(
        productId: UUID? = null,
        warehouseId: UUID? = null,
        movementType: StockMovementType? = null,
        pageable: Pageable,
    ): StockMovementPageResponse {
        val tenantId = getCurrentTenantId()

        val spec =
            Specification<StockMovement> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<UUID>("tenantId"), tenantId))

                if (productId != null) {
                    predicates.add(cb.equal(root.get<UUID>("productId"), productId))
                }
                if (warehouseId != null) {
                    val srcMatch = cb.equal(root.get<UUID>("sourceWarehouseId"), warehouseId)
                    val dstMatch = cb.equal(root.get<UUID>("destinationWarehouseId"), warehouseId)
                    predicates.add(cb.or(srcMatch, dstMatch))
                }
                if (movementType != null) {
                    predicates.add(cb.equal(root.get<StockMovementType>("movementType"), movementType))
                }

                cb.and(*predicates.toTypedArray())
            }

        val page = movementRepository.findAll(spec, pageable)
        val items =
            page.content.map { m ->
                val prod = productRepository.findById(m.productId).orElse(null)
                val src = m.sourceWarehouseId?.let { warehouseRepository.findById(it).orElse(null) }
                val dst = m.destinationWarehouseId?.let { warehouseRepository.findById(it).orElse(null) }
                StockMovementResponse.fromEntity(m, prod?.sku, prod?.name, src?.name, dst?.name)
            }

        return StockMovementPageResponse(
            items = items,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            size = page.size,
        )
    }
}
