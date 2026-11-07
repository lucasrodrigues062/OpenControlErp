package org.opencontrol.erp.inventory.repository

import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.ProductSerialNumber
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ProductLotRepository : JpaRepository<ProductLot, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<ProductLot>

    fun findByTenantIdAndProductIdAndLotNumber(
        tenantId: UUID,
        productId: UUID,
        lotNumber: String,
    ): Optional<ProductLot>

    fun existsByTenantIdAndProductIdAndLotNumber(
        tenantId: UUID,
        productId: UUID,
        lotNumber: String,
    ): Boolean

    fun findAllByTenantIdAndProductIdOrderByExpirationDateAsc(
        tenantId: UUID,
        productId: UUID,
    ): List<ProductLot>

    fun findAllByTenantIdAndProductIdAndWarehouseIdOrderByExpirationDateAsc(
        tenantId: UUID,
        productId: UUID,
        warehouseId: UUID,
    ): List<ProductLot>

    fun findAllByTenantIdAndProductIdAndWarehouseIdAndStatusOrderByExpirationDateAsc(
        tenantId: UUID,
        productId: UUID,
        warehouseId: UUID,
        status: LotStatus,
    ): List<ProductLot>
}

@Repository
interface ProductSerialNumberRepository : JpaRepository<ProductSerialNumber, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<ProductSerialNumber>

    fun findByTenantIdAndProductIdAndSerialNumber(
        tenantId: UUID,
        productId: UUID,
        serialNumber: String,
    ): Optional<ProductSerialNumber>

    fun findByTenantIdAndSerialNumber(
        tenantId: UUID,
        serialNumber: String,
    ): Optional<ProductSerialNumber>

    fun existsByTenantIdAndProductIdAndSerialNumber(
        tenantId: UUID,
        productId: UUID,
        serialNumber: String,
    ): Boolean

    fun findAllByTenantIdAndProductIdOrderBySerialNumberAsc(
        tenantId: UUID,
        productId: UUID,
    ): List<ProductSerialNumber>

    fun findAllByTenantIdAndProductIdAndStatusOrderBySerialNumberAsc(
        tenantId: UUID,
        productId: UUID,
        status: SerialNumberStatus,
    ): List<ProductSerialNumber>
}
