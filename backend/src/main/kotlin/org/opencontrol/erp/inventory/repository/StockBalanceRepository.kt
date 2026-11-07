package org.opencontrol.erp.inventory.repository

import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.StockMovement
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface StockBalanceRepository : JpaRepository<StockBalance, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<StockBalance>

    fun findByWarehouseIdAndProductIdAndLocationId(
        warehouseId: UUID,
        productId: UUID,
        locationId: UUID?,
    ): Optional<StockBalance>

    fun findByWarehouseIdAndProductIdAndLocationIdIsNull(
        warehouseId: UUID,
        productId: UUID,
    ): Optional<StockBalance>

    fun findAllByTenantIdAndProductId(
        tenantId: UUID,
        productId: UUID,
    ): List<StockBalance>

    fun findAllByTenantIdAndWarehouseId(
        tenantId: UUID,
        warehouseId: UUID,
    ): List<StockBalance>

    fun findAllByTenantId(tenantId: UUID): List<StockBalance>
}

@Repository
interface StockMovementRepository :
    JpaRepository<StockMovement, UUID>,
    JpaSpecificationExecutor<StockMovement> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<StockMovement>

    fun findAllByTenantId(
        tenantId: UUID,
        pageable: Pageable,
    ): Page<StockMovement>
}
