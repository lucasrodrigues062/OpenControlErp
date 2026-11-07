package org.opencontrol.erp.inventory.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.inventory.api.dto.CreateLotRequest
import org.opencontrol.erp.inventory.api.dto.CreateSerialNumberRequest
import org.opencontrol.erp.inventory.api.dto.ProductLotResponse
import org.opencontrol.erp.inventory.api.dto.ProductSerialNumberResponse
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.ProductSerialNumber
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.opencontrol.erp.inventory.exception.DuplicateSerialNumberException
import org.opencontrol.erp.inventory.exception.SerialNumberNotFoundException
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class TraceabilityService(
    private val lotRepository: ProductLotRepository,
    private val serialRepository: ProductSerialNumberRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listLots(
        productId: UUID,
        warehouseId: UUID? = null,
        availableOnly: Boolean = false,
    ): List<ProductLotResponse> {
        val tenantId = getCurrentTenantId()
        val today = LocalDate.now()

        val lots =
            if (warehouseId != null) {
                lotRepository.findAllByTenantIdAndProductIdAndWarehouseIdOrderByExpirationDateAsc(tenantId, productId, warehouseId)
            } else {
                lotRepository.findAllByTenantIdAndProductIdOrderByExpirationDateAsc(tenantId, productId)
            }

        return lots
            .map { lot ->
                // Check expiry state
                if (lot.expirationDate.isBefore(today) && lot.status != LotStatus.EXPIRED) {
                    lot.status = LotStatus.EXPIRED
                    lot.updatedAt = Instant.now()
                    lotRepository.save(lot)
                }
                lot
            }.filter { lot ->
                if (availableOnly) {
                    lot.status == LotStatus.RELEASED &&
                        !lot.expirationDate.isBefore(today) &&
                        (lot.quantityOnHand - lot.quantityReserved) > java.math.BigDecimal.ZERO
                } else {
                    true
                }
            }.map { ProductLotResponse.fromEntity(it) }
    }

    fun createLot(request: CreateLotRequest): ProductLotResponse {
        val tenantId = getCurrentTenantId()
        val lotNumber = request.lotNumber.trim().uppercase()

        if (request.expirationDate.isBefore(request.manufacturingDate)) {
            throw IllegalArgumentException("Data de validade não pode ser anterior à data de fabricação.")
        }

        if (lotRepository.existsByTenantIdAndProductIdAndLotNumber(tenantId, request.productId, lotNumber)) {
            throw IllegalArgumentException("Lote '$lotNumber' já cadastrado para este produto.")
        }

        val initialStatus =
            if (request.expirationDate.isBefore(LocalDate.now())) {
                LotStatus.EXPIRED
            } else {
                LotStatus.RELEASED
            }

        val lot =
            ProductLot(
                tenantId = tenantId,
                warehouseId = request.warehouseId,
                productId = request.productId,
                lotNumber = lotNumber,
                manufacturingDate = request.manufacturingDate,
                expirationDate = request.expirationDate,
                quantityOnHand = request.quantityOnHand,
                quantityReserved = java.math.BigDecimal.ZERO,
                status = initialStatus,
            )

        val saved = lotRepository.save(lot)

        auditService.recordAudit(
            entityType = "ProductLot",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created lot ${saved.lotNumber} for product ${saved.productId}",
            stateAfter = saved,
        )

        return ProductLotResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun listSerialNumbers(
        productId: UUID? = null,
        serialNumber: String? = null,
        status: SerialNumberStatus? = null,
    ): List<ProductSerialNumberResponse> {
        val tenantId = getCurrentTenantId()

        if (!serialNumber.isNullOrBlank()) {
            return serialRepository
                .findByTenantIdAndSerialNumber(tenantId, serialNumber.trim())
                .map { listOf(ProductSerialNumberResponse.fromEntity(it)) }
                .orElse(emptyList())
        }

        val list =
            when {
                productId != null && status != null ->
                    serialRepository.findAllByTenantIdAndProductIdAndStatusOrderBySerialNumberAsc(tenantId, productId, status)
                productId != null ->
                    serialRepository.findAllByTenantIdAndProductIdOrderBySerialNumberAsc(tenantId, productId)
                else ->
                    serialRepository.findAll().filter { it.tenantId == tenantId }
            }

        return list.map { ProductSerialNumberResponse.fromEntity(it) }
    }

    fun createSerialNumber(request: CreateSerialNumberRequest): ProductSerialNumberResponse {
        val tenantId = getCurrentTenantId()
        val serialNumber = request.serialNumber.trim().uppercase()

        if (serialRepository.existsByTenantIdAndProductIdAndSerialNumber(tenantId, request.productId, serialNumber)) {
            throw DuplicateSerialNumberException(serialNumber)
        }

        val serial =
            ProductSerialNumber(
                tenantId = tenantId,
                warehouseId = request.warehouseId,
                productId = request.productId,
                lotId = request.lotId,
                serialNumber = serialNumber,
                status = SerialNumberStatus.IN_STOCK,
                warrantyUntil = request.warrantyUntil,
            )

        val saved = serialRepository.save(serial)

        auditService.recordAudit(
            entityType = "ProductSerialNumber",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Registered serial number $serialNumber for product ${saved.productId}",
            stateAfter = saved,
        )

        return ProductSerialNumberResponse.fromEntity(saved)
    }

    fun updateSerialNumberStatus(
        id: UUID,
        newStatus: SerialNumberStatus,
    ): ProductSerialNumberResponse {
        val tenantId = getCurrentTenantId()
        val serial =
            serialRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { SerialNumberNotFoundException("Serial number with ID '$id' was not found.") }

        val beforeState = ProductSerialNumberResponse.fromEntity(serial)
        serial.status = newStatus
        serial.updatedAt = Instant.now()
        val saved = serialRepository.save(serial)

        auditService.recordAudit(
            entityType = "ProductSerialNumber",
            entityId = saved.id.toString(),
            action = "UPDATE_STATUS",
            details = "Updated serial ${saved.serialNumber} status to $newStatus",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        return ProductSerialNumberResponse.fromEntity(saved)
    }
}
