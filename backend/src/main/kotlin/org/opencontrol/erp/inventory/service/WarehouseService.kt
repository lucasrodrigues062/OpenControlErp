package org.opencontrol.erp.inventory.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.inventory.api.dto.CreateStorageLocationRequest
import org.opencontrol.erp.inventory.api.dto.CreateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.StorageLocationResponse
import org.opencontrol.erp.inventory.api.dto.UpdateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.WarehouseResponse
import org.opencontrol.erp.inventory.domain.model.StorageLocation
import org.opencontrol.erp.inventory.domain.model.Warehouse
import org.opencontrol.erp.inventory.exception.DuplicateStorageLocationException
import org.opencontrol.erp.inventory.exception.DuplicateWarehouseCodeException
import org.opencontrol.erp.inventory.exception.WarehouseNotFoundException
import org.opencontrol.erp.inventory.repository.StorageLocationRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class WarehouseService(
    private val warehouseRepository: WarehouseRepository,
    private val locationRepository: StorageLocationRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listWarehouses(branchId: UUID? = null): List<WarehouseResponse> {
        val tenantId = getCurrentTenantId()
        val list =
            if (branchId != null) {
                warehouseRepository.findAllByTenantIdAndBranchIdOrderByCodeAsc(tenantId, branchId)
            } else {
                warehouseRepository.findAllByTenantIdOrderByCodeAsc(tenantId)
            }
        return list.map { WarehouseResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): WarehouseResponse {
        val tenantId = getCurrentTenantId()
        val warehouse =
            warehouseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { WarehouseNotFoundException(id) }
        return WarehouseResponse.fromEntity(warehouse)
    }

    fun createWarehouse(request: CreateWarehouseRequest): WarehouseResponse {
        val tenantId = getCurrentTenantId()
        val code = request.code.trim().uppercase()

        if (warehouseRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw DuplicateWarehouseCodeException(code)
        }

        if (request.isDefault) {
            warehouseRepository
                .findByTenantIdAndBranchIdAndIsDefaultTrue(tenantId, request.branchId)
                .ifPresent {
                    it.isDefault = false
                    warehouseRepository.save(it)
                }
        }

        val warehouse =
            Warehouse(
                tenantId = tenantId,
                branchId = request.branchId,
                code = code,
                name = request.name.trim(),
                warehouseType = request.warehouseType,
                isDefault = request.isDefault,
                isActive = true,
            )

        val saved = warehouseRepository.save(warehouse)

        auditService.recordAudit(
            entityType = "Warehouse",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created warehouse ${saved.code} - ${saved.name}",
            stateAfter = saved,
        )

        return WarehouseResponse.fromEntity(saved)
    }

    fun updateWarehouse(
        id: UUID,
        request: UpdateWarehouseRequest,
    ): WarehouseResponse {
        val tenantId = getCurrentTenantId()
        val warehouse =
            warehouseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { WarehouseNotFoundException(id) }

        if (request.isDefault && !warehouse.isDefault) {
            warehouseRepository
                .findByTenantIdAndBranchIdAndIsDefaultTrue(tenantId, request.branchId)
                .ifPresent {
                    it.isDefault = false
                    warehouseRepository.save(it)
                }
        }

        val beforeState = WarehouseResponse.fromEntity(warehouse)
        warehouse.branchId = request.branchId
        warehouse.name = request.name.trim()
        warehouse.warehouseType = request.warehouseType
        warehouse.isDefault = request.isDefault
        warehouse.isActive = request.isActive
        warehouse.updatedAt = Instant.now()

        val saved = warehouseRepository.save(warehouse)

        auditService.recordAudit(
            entityType = "Warehouse",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated warehouse ${saved.code}",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        return WarehouseResponse.fromEntity(saved)
    }

    fun deleteWarehouse(id: UUID) {
        val tenantId = getCurrentTenantId()
        val warehouse =
            warehouseRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { WarehouseNotFoundException(id) }

        warehouse.isActive = false
        warehouse.updatedAt = Instant.now()
        warehouseRepository.save(warehouse)

        auditService.recordAudit(
            entityType = "Warehouse",
            entityId = id.toString(),
            action = "DEACTIVATE",
            details = "Deactivated warehouse ${warehouse.code} - ${warehouse.name}",
        )
    }

    fun createLocation(
        warehouseId: UUID,
        request: CreateStorageLocationRequest,
    ): StorageLocationResponse {
        val tenantId = getCurrentTenantId()
        val warehouse =
            warehouseRepository
                .findByIdAndTenantId(warehouseId, tenantId)
                .orElseThrow { WarehouseNotFoundException(warehouseId) }

        val aisle = request.aisle.trim().uppercase()
        val rack = request.rack.trim().uppercase()
        val shelf = request.shelf.trim().uppercase()
        val bin = request.bin.trim().uppercase()
        val formattedAddress = "$aisle-$rack-$shelf-$bin"

        if (locationRepository.existsByWarehouseIdAndFormattedAddress(warehouse.id, formattedAddress)) {
            throw DuplicateStorageLocationException(formattedAddress)
        }

        val location =
            StorageLocation(
                tenantId = tenantId,
                warehouseId = warehouse.id,
                aisle = aisle,
                rack = rack,
                shelf = shelf,
                bin = bin,
                formattedAddress = formattedAddress,
                isActive = true,
            )

        val saved = locationRepository.save(location)

        auditService.recordAudit(
            entityType = "StorageLocation",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created storage location $formattedAddress in warehouse ${warehouse.code}",
            stateAfter = saved,
        )

        return StorageLocationResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun listLocations(warehouseId: UUID): List<StorageLocationResponse> {
        val tenantId = getCurrentTenantId()
        return locationRepository
            .findAllByTenantIdAndWarehouseIdOrderByFormattedAddressAsc(tenantId, warehouseId)
            .map { StorageLocationResponse.fromEntity(it) }
    }
}
