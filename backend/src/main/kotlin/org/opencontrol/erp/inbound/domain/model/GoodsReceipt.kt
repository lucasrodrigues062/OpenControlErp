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
import java.time.LocalDate
import java.util.UUID

enum class GoodsReceiptStatus {
    IN_PROGRESS,
    CONFERRED_OK,
    CONFERRED_WITH_DIVERGENCE,
    CANCELLED,
}

@Entity
@Table(name = "goods_receipts")
class GoodsReceipt(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false)
    var branchId: UUID,
    @Column(name = "warehouse_id", nullable = false)
    var warehouseId: UUID,
    @Column(name = "purchase_order_id")
    var purchaseOrderId: UUID? = null,
    @Column(name = "inbound_invoice_id")
    var inboundInvoiceId: UUID? = null,
    @Column(name = "receipt_number", length = 50, nullable = false)
    var receiptNumber: String,
    @Column(name = "receipt_date", nullable = false)
    var receiptDate: LocalDate = LocalDate.now(),
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: GoodsReceiptStatus = GoodsReceiptStatus.IN_PROGRESS,
    @Column(name = "conference_notes", columnDefinition = "TEXT")
    var conferenceNotes: String? = null,
    @OneToMany(mappedBy = "goodsReceipt", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<GoodsReceiptItem> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "goods_receipt_items")
class GoodsReceiptItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    var goodsReceipt: GoodsReceipt,
    @Column(name = "product_id", nullable = false)
    var productId: UUID,
    @Column(name = "expected_quantity", precision = 15, scale = 4, nullable = false)
    var expectedQuantity: BigDecimal,
    @Column(name = "received_quantity", precision = 15, scale = 4, nullable = false)
    var receivedQuantity: BigDecimal,
    @Column(name = "rejected_quantity", precision = 15, scale = 4, nullable = false)
    var rejectedQuantity: BigDecimal = BigDecimal.ZERO,
    @Column(name = "lot_number", length = 100)
    var lotNumber: String? = null,
    @Column(name = "fabrication_date")
    var fabricationDate: LocalDate? = null,
    @Column(name = "expiration_date")
    var expirationDate: LocalDate? = null,
    @Column(name = "serial_numbers_json", columnDefinition = "JSONB", nullable = false)
    var serialNumbersJson: String = "[]",
)
