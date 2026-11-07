package org.opencontrol.erp.inventory.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "stock_balances")
class StockBalance(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "warehouse_id", nullable = false)
    val warehouseId: UUID,
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "location_id")
    var locationId: UUID? = null,
    @Column(name = "quantity_on_hand", precision = 15, scale = 4, nullable = false)
    var quantityOnHand: BigDecimal = BigDecimal.ZERO,
    @Column(name = "quantity_reserved", precision = 15, scale = 4, nullable = false)
    var quantityReserved: BigDecimal = BigDecimal.ZERO,
    @Column(name = "quantity_available", precision = 15, scale = 4, nullable = false)
    var quantityAvailable: BigDecimal = BigDecimal.ZERO,
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
