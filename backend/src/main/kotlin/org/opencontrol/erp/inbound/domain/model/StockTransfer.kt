package org.opencontrol.erp.inbound.domain.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

enum class StockTransferType {
    INTERNAL_WAREHOUSE,
    INTER_BRANCH,
}

enum class StockTransferStatus {
    DRAFT,
    DISPATCHED_IN_TRANSIT,
    RECEIVED_COMPLETED,
    CANCELLED,
}

@Entity
@Table(name = "stock_transfers")
class StockTransfer(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "transfer_number", length = 50, nullable = false)
    var transferNumber: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", length = 30, nullable = false)
    var transferType: StockTransferType = StockTransferType.INTERNAL_WAREHOUSE,
    @Column(name = "origin_branch_id", nullable = false)
    var originBranchId: UUID,
    @Column(name = "origin_warehouse_id", nullable = false)
    var originWarehouseId: UUID,
    @Column(name = "destination_branch_id", nullable = false)
    var destinationBranchId: UUID,
    @Column(name = "destination_warehouse_id", nullable = false)
    var destinationWarehouseId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: StockTransferStatus = StockTransferStatus.DRAFT,
    @Column(name = "dispatched_at")
    var dispatchedAt: Instant? = null,
    @Column(name = "received_at")
    var receivedAt: Instant? = null,
    @Column(name = "tracking_notes", columnDefinition = "TEXT")
    var trackingNotes: String? = null,
    @OneToMany(mappedBy = "stockTransfer", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<StockTransferItem> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "stock_transfer_items")
class StockTransferItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_transfer_id", nullable = false)
    var stockTransfer: StockTransfer,
    @Column(name = "product_id", nullable = false)
    var productId: UUID,
    @Column(name = "requested_quantity", precision = 15, scale = 4, nullable = false)
    var requestedQuantity: BigDecimal,
    @Column(name = "shipped_quantity", precision = 15, scale = 4, nullable = false)
    var shippedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "received_quantity", precision = 15, scale = 4, nullable = false)
    var receivedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "unit_cost", precision = 15, scale = 4, nullable = false)
    var unitCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "lot_id")
    var lotId: UUID? = null,
    @Column(name = "serial_numbers_json", columnDefinition = "JSONB", nullable = false)
    var serialNumbersJson: String = "[]",
)
