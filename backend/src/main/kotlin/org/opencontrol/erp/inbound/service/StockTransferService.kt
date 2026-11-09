package org.opencontrol.erp.inbound.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.service.PricingService
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.inbound.api.dto.CreateTransferRequest
import org.opencontrol.erp.inbound.api.dto.StockTransferItemResponse
import org.opencontrol.erp.inbound.api.dto.StockTransferResponse
import org.opencontrol.erp.inbound.domain.model.StockTransfer
import org.opencontrol.erp.inbound.domain.model.StockTransferItem
import org.opencontrol.erp.inbound.domain.model.StockTransferStatus
import org.opencontrol.erp.inbound.domain.model.StockTransferType
import org.opencontrol.erp.inbound.repository.StockTransferRepository
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.StockMovement
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.inventory.repository.StockBalanceRepository
import org.opencontrol.erp.inventory.repository.StockMovementRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class StockTransferService(
    private val transferRepository: StockTransferRepository,
    private val productRepository: ProductRepository,
    private val warehouseRepository: WarehouseRepository,
    private val branchRepository: CompanyBranchRepository,
    private val stockBalanceRepository: StockBalanceRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val productLotRepository: ProductLotRepository,
    private val productSerialNumberRepository: ProductSerialNumberRepository,
    private val pricingService: PricingService,
    private val auditService: AuditService,
    private val objectMapper: ObjectMapper,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()
    private val random = SecureRandom()

    @Transactional(readOnly = true)
    fun listTransfers(
        transferType: StockTransferType? = null,
        status: StockTransferStatus? = null,
    ): List<StockTransferResponse> {
        val tenantId = getCurrentTenantId()
        val all = transferRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
        val filtered = all.filter { t ->
            (transferType == null || t.transferType == transferType) &&
            (status == null || t.status == status)
        }
        return filtered.map { mapToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): StockTransferResponse {
        val tenantId = getCurrentTenantId()
        val transfer = transferRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { NoSuchElementException("Transferência com ID $id não encontrada") }
        return mapToResponse(transfer)
    }

    fun createTransfer(request: CreateTransferRequest): StockTransferResponse {
        val tenantId = getCurrentTenantId()

        // Validation
        if (request.originWarehouseId == request.destinationWarehouseId) {
            throw IllegalArgumentException("Armazém de origem e destino não podem ser o mesmo")
        }

        if (request.transferType == StockTransferType.INTERNAL_WAREHOUSE &&
            request.originBranchId != request.destinationBranchId
        ) {
            throw IllegalArgumentException("Transferência interna deve pertencer à mesma filial")
        }

        val transferNumber = if (!request.transferNumber.isNullOrBlank()) {
            if (transferRepository.existsByTenantIdAndTransferNumber(tenantId, request.transferNumber.trim())) {
                throw IllegalArgumentException("Número de transferência '${request.transferNumber}' já existe")
            }
            request.transferNumber.trim()
        } else {
            generateTransferNumber(tenantId)
        }

        val isInternal = request.transferType == StockTransferType.INTERNAL_WAREHOUSE
        val now = Instant.now()

        val transfer = StockTransfer(
            tenantId = tenantId,
            transferNumber = transferNumber,
            transferType = request.transferType,
            originBranchId = request.originBranchId,
            originWarehouseId = request.originWarehouseId,
            destinationBranchId = request.destinationBranchId,
            destinationWarehouseId = request.destinationWarehouseId,
            status = if (isInternal) StockTransferStatus.RECEIVED_COMPLETED else StockTransferStatus.DISPATCHED_IN_TRANSIT,
            dispatchedAt = now,
            receivedAt = if (isInternal) now else null,
            trackingNotes = request.trackingNotes,
            createdAt = now,
            updatedAt = now,
        )

        for (itemReq in request.items) {
            val product = productRepository.findByIdAndTenantId(itemReq.productId, tenantId)
                .orElseThrow { NoSuchElementException("Produto com ID ${itemReq.productId} não encontrado") }

            val originBalance = stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(
                request.originWarehouseId, itemReq.productId
            ).orElseThrow {
                IllegalStateException("Saldo não encontrado no armazém de origem para o produto '${product.name}'")
            }

            if (originBalance.quantityAvailable < itemReq.quantity) {
                throw IllegalStateException(
                    "Saldo disponível insuficiente para o produto '${product.name}'. Disponível: ${originBalance.quantityAvailable}, Solicitado: ${itemReq.quantity}"
                )
            }

            val unitCost = if (product.averageCost > BigDecimal.ZERO) {
                product.averageCost
            } else if (product.lastAcquisitionCost > BigDecimal.ZERO) {
                product.lastAcquisitionCost
            } else {
                product.replacementCost
            }

            // Debit origin warehouse
            originBalance.quantityOnHand = originBalance.quantityOnHand.subtract(itemReq.quantity)
            originBalance.quantityAvailable = originBalance.quantityOnHand.subtract(originBalance.quantityReserved)
            originBalance.updatedAt = now
            stockBalanceRepository.save(originBalance)

            // Record origin movement (TRANSFER_OUT)
            val outMovement = StockMovement(
                tenantId = tenantId,
                branchId = request.originBranchId,
                movementType = StockMovementType.TRANSFER_OUT,
                productId = product.id,
                sourceWarehouseId = request.originWarehouseId,
                destinationWarehouseId = request.destinationWarehouseId,
                quantity = itemReq.quantity,
                unitCost = unitCost,
                balanceAfter = originBalance.quantityOnHand,
                referenceDocument = transferNumber,
                notes = "Transferência ${request.transferType} - Saída do armazém de origem",
                createdAt = now,
            )
            stockMovementRepository.save(outMovement)

            val shippedQty = itemReq.quantity
            val receivedQty = if (isInternal) itemReq.quantity else BigDecimal.ZERO

            // If internal warehouse transfer, credit destination immediately
            if (isInternal) {
                val destBalance = stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(
                    request.destinationWarehouseId, itemReq.productId
                ).orElseGet {
                    StockBalance(
                        tenantId = tenantId,
                        warehouseId = request.destinationWarehouseId,
                        productId = itemReq.productId,
                        quantityOnHand = BigDecimal.ZERO,
                        quantityReserved = BigDecimal.ZERO,
                        quantityAvailable = BigDecimal.ZERO,
                        updatedAt = now,
                    )
                }

                destBalance.quantityOnHand = destBalance.quantityOnHand.add(itemReq.quantity)
                destBalance.quantityAvailable = destBalance.quantityOnHand.subtract(destBalance.quantityReserved)
                destBalance.updatedAt = now
                stockBalanceRepository.save(destBalance)

                // Record destination movement (TRANSFER_IN)
                val inMovement = StockMovement(
                    tenantId = tenantId,
                    branchId = request.destinationBranchId,
                    movementType = StockMovementType.TRANSFER_IN,
                    productId = product.id,
                    sourceWarehouseId = request.originWarehouseId,
                    destinationWarehouseId = request.destinationWarehouseId,
                    quantity = itemReq.quantity,
                    unitCost = unitCost,
                    balanceAfter = destBalance.quantityOnHand,
                    referenceDocument = transferNumber,
                    notes = "Transferência interna - Entrada no armazém de destino",
                    createdAt = now,
                )
                stockMovementRepository.save(inMovement)

                // Update serial numbers warehouse
                itemReq.serialNumbers.forEach { sn ->
                    productSerialNumberRepository.findByTenantIdAndProductIdAndSerialNumber(tenantId, product.id, sn)
                        .ifPresent { s ->
                            s.warehouseId = request.destinationWarehouseId
                            s.updatedAt = now
                            productSerialNumberRepository.save(s)
                        }
                }
            }

            val transferItem = StockTransferItem(
                tenantId = tenantId,
                stockTransfer = transfer,
                productId = product.id,
                requestedQuantity = itemReq.quantity,
                shippedQuantity = shippedQty,
                receivedQuantity = receivedQty,
                unitCost = unitCost,
                lotId = itemReq.lotId,
                serialNumbersJson = objectMapper.writeValueAsString(itemReq.serialNumbers),
            )
            transfer.items.add(transferItem)
        }

        val saved = transferRepository.save(transfer)
        auditService.recordAudit(
            action = if (isInternal) "CREATE_AND_RECEIVE_INTERNAL" else "DISPATCH_INTER_BRANCH",
            entityType = "StockTransfer",
            entityId = saved.id.toString(),
            details = "Transferência criada ${saved.transferNumber} - Tipo: ${saved.transferType}, Status: ${saved.status}"
        )

        return mapToResponse(saved)
    }

    fun receiveTransfer(id: UUID): StockTransferResponse {
        val tenantId = getCurrentTenantId()
        val transfer = transferRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { NoSuchElementException("Transferência com ID $id não encontrada") }

        if (transfer.status != StockTransferStatus.DISPATCHED_IN_TRANSIT) {
            throw IllegalStateException("Apenas transferências EM TRÂNSITO podem ser recebidas (status atual: ${transfer.status})")
        }

        val now = Instant.now()

        for (item in transfer.items) {
            val product = productRepository.findByIdAndTenantId(item.productId, tenantId)
                .orElseThrow { NoSuchElementException("Produto com ID ${item.productId} não encontrado") }

            val destBalance = stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(
                transfer.destinationWarehouseId, item.productId
            ).orElseGet {
                StockBalance(
                    tenantId = tenantId,
                    warehouseId = transfer.destinationWarehouseId,
                    productId = item.productId,
                    quantityOnHand = BigDecimal.ZERO,
                    quantityReserved = BigDecimal.ZERO,
                    quantityAvailable = BigDecimal.ZERO,
                    updatedAt = now,
                )
            }

            // Recalculate CMP
            val newCmp = pricingService.calculateWeightedAverageCost(
                currentQty = destBalance.quantityOnHand,
                currentCmp = product.averageCost,
                inboundQty = item.shippedQuantity,
                inboundCost = item.unitCost,
            )
            product.averageCost = newCmp
            product.updatedAt = now
            productRepository.save(product)

            destBalance.quantityOnHand = destBalance.quantityOnHand.add(item.shippedQuantity)
            destBalance.quantityAvailable = destBalance.quantityOnHand.subtract(destBalance.quantityReserved)
            destBalance.updatedAt = now
            stockBalanceRepository.save(destBalance)

            val inMovement = StockMovement(
                tenantId = tenantId,
                branchId = transfer.destinationBranchId,
                movementType = StockMovementType.TRANSFER_IN,
                productId = product.id,
                sourceWarehouseId = transfer.originWarehouseId,
                destinationWarehouseId = transfer.destinationWarehouseId,
                quantity = item.shippedQuantity,
                unitCost = item.unitCost,
                balanceAfter = destBalance.quantityOnHand,
                referenceDocument = transfer.transferNumber,
                notes = "Recebimento de transferência entre filiais",
                createdAt = now,
            )
            stockMovementRepository.save(inMovement)

            item.receivedQuantity = item.shippedQuantity

            // Update lot if present
            if (item.lotId != null) {
                productLotRepository.findById(item.lotId!!).ifPresent { originLot ->
                    val destLot = productLotRepository.findByTenantIdAndProductIdAndLotNumber(
                        tenantId, item.productId, originLot.lotNumber
                    ).orElseGet {
                        ProductLot(
                            tenantId = tenantId,
                            warehouseId = transfer.destinationWarehouseId,
                            productId = item.productId,
                            lotNumber = originLot.lotNumber,
                            manufacturingDate = originLot.manufacturingDate,
                            expirationDate = originLot.expirationDate,
                            quantityOnHand = BigDecimal.ZERO,
                        )
                    }
                    destLot.quantityOnHand = destLot.quantityOnHand.add(item.shippedQuantity)
                    destLot.updatedAt = now
                    productLotRepository.save(destLot)
                }
            }

            // Update serial numbers warehouse
            val serials: List<String> = try {
                objectMapper.readValue(item.serialNumbersJson, object : TypeReference<List<String>>() {})
            } catch (_: Exception) {
                emptyList()
            }

            serials.forEach { sn ->
                productSerialNumberRepository.findByTenantIdAndProductIdAndSerialNumber(tenantId, product.id, sn)
                    .ifPresent { s ->
                        s.warehouseId = transfer.destinationWarehouseId
                        s.updatedAt = now
                        productSerialNumberRepository.save(s)
                    }
            }
        }

        transfer.status = StockTransferStatus.RECEIVED_COMPLETED
        transfer.receivedAt = now
        transfer.updatedAt = now

        val saved = transferRepository.save(transfer)
        auditService.recordAudit(
            action = "RECEIVE_TRANSFER",
            entityType = "StockTransfer",
            entityId = saved.id.toString(),
            details = "Transferência ${saved.transferNumber} recebida com sucesso na filial de destino"
        )

        return mapToResponse(saved)
    }

    private fun generateTransferNumber(tenantId: UUID): String {
        for (i in 0..10) {
            val num = "TRF-${random.nextInt(90000) + 10000}"
            if (!transferRepository.existsByTenantIdAndTransferNumber(tenantId, num)) {
                return num
            }
        }
        return "TRF-${System.currentTimeMillis() % 100000}"
    }

    private fun mapToResponse(transfer: StockTransfer): StockTransferResponse {
        val tenantId = transfer.tenantId

        val originBranch = branchRepository.findById(transfer.originBranchId).orElse(null)
        val destBranch = branchRepository.findById(transfer.destinationBranchId).orElse(null)
        val originWarehouse = warehouseRepository.findByIdAndTenantId(transfer.originWarehouseId, tenantId).orElse(null)
        val destWarehouse = warehouseRepository.findByIdAndTenantId(transfer.destinationWarehouseId, tenantId).orElse(null)

        val productIds = transfer.items.map { it.productId }
        val products = productRepository.findAllById(productIds).associateBy { it.id }

        val itemResponses = transfer.items.map { item ->
            val product = products[item.productId]
            val serials: List<String> = try {
                objectMapper.readValue(item.serialNumbersJson, object : TypeReference<List<String>>() {})
            } catch (_: Exception) {
                emptyList()
            }

            StockTransferItemResponse.fromEntity(
                item = item,
                productName = product?.name,
                productSku = product?.sku,
                lotNumber = null,
                serialNumbers = serials,
            )
        }

        return StockTransferResponse(
            id = transfer.id,
            transferNumber = transfer.transferNumber,
            transferType = transfer.transferType,
            originBranchId = transfer.originBranchId,
            originBranchName = originBranch?.tradeName ?: originBranch?.legalName,
            originWarehouseId = transfer.originWarehouseId,
            originWarehouseName = originWarehouse?.name,
            destinationBranchId = transfer.destinationBranchId,
            destinationBranchName = destBranch?.tradeName ?: destBranch?.legalName,
            destinationWarehouseId = transfer.destinationWarehouseId,
            destinationWarehouseName = destWarehouse?.name,
            status = transfer.status,
            dispatchedAt = transfer.dispatchedAt,
            receivedAt = transfer.receivedAt,
            trackingNotes = transfer.trackingNotes,
            items = itemResponses,
            createdAt = transfer.createdAt,
            updatedAt = transfer.updatedAt,
        )
    }
}
