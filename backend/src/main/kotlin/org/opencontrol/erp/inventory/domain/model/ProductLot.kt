package org.opencontrol.erp.inventory.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "product_lots")
class ProductLot(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "warehouse_id", nullable = false)
    val warehouseId: UUID,
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "lot_number", length = 100, nullable = false)
    var lotNumber: String,
    @Column(name = "manufacturing_date", nullable = false)
    var manufacturingDate: LocalDate,
    @Column(name = "expiration_date", nullable = false)
    var expirationDate: LocalDate,
    @Column(name = "quantity_on_hand", precision = 15, scale = 4, nullable = false)
    var quantityOnHand: BigDecimal = BigDecimal.ZERO,
    @Column(name = "quantity_reserved", precision = 15, scale = 4, nullable = false)
    var quantityReserved: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: LotStatus = LotStatus.RELEASED,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
