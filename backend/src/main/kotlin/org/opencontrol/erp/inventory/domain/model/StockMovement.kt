package org.opencontrol.erp.inventory.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "stock_movements")
class StockMovement(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false)
    val branchId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", length = 30, nullable = false)
    val movementType: StockMovementType,
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "source_warehouse_id")
    val sourceWarehouseId: UUID? = null,
    @Column(name = "destination_warehouse_id")
    val destinationWarehouseId: UUID? = null,
    @Column(name = "location_id")
    val locationId: UUID? = null,
    @Column(name = "lot_id")
    val lotId: UUID? = null,
    @Column(name = "serial_number_id")
    val serialNumberId: UUID? = null,
    @Column(name = "quantity", precision = 15, scale = 4, nullable = false)
    val quantity: BigDecimal,
    @Column(name = "unit_cost", precision = 15, scale = 4, nullable = false)
    val unitCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "balance_after", precision = 15, scale = 4, nullable = false)
    val balanceAfter: BigDecimal = BigDecimal.ZERO,
    @Column(name = "reference_document", length = 100)
    val referenceDocument: String? = null,
    @Column(name = "reason_code", length = 50)
    val reasonCode: String? = null,
    @Column(name = "notes", columnDefinition = "TEXT")
    val notes: String? = null,
    @Column(name = "created_by_user_id")
    val createdByUserId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
