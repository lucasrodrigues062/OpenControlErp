package org.opencontrol.erp.inventory.repository

import org.opencontrol.erp.inventory.domain.model.StorageLocation
import org.opencontrol.erp.inventory.domain.model.Warehouse
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface WarehouseRepository : JpaRepository<Warehouse, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<Warehouse>

    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<Warehouse>

    fun findAllByTenantIdAndBranchIdOrderByCodeAsc(
        tenantId: UUID,
        branchId: UUID,
    ): List<Warehouse>

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean

    fun findByTenantIdAndBranchIdAndIsDefaultTrue(
        tenantId: UUID,
        branchId: UUID,
    ): Optional<Warehouse>
}

@Repository
interface StorageLocationRepository : JpaRepository<StorageLocation, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<StorageLocation>

    fun findAllByTenantIdAndWarehouseIdOrderByFormattedAddressAsc(
        tenantId: UUID,
        warehouseId: UUID,
    ): List<StorageLocation>

    fun existsByWarehouseIdAndFormattedAddress(
        warehouseId: UUID,
        formattedAddress: String,
    ): Boolean
}
