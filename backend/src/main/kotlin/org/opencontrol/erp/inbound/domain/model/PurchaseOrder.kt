package org.opencontrol.erp.inbound.domain.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class PurchaseOrderStatus {
    DRAFT,
    APPROVED,
    PARTIALLY_RECEIVED,
    FULLY_RECEIVED,
    CANCELLED,
}

@Entity
@Table(name = "purchase_orders")
class PurchaseOrder(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false)
    var branchId: UUID,
    @Column(name = "order_number", length = 50, nullable = false)
    var orderNumber: String,
    @Column(name = "supplier_id", nullable = false)
    var supplierId: UUID,
    @Column(name = "order_date", nullable = false)
    var orderDate: LocalDate = LocalDate.now(),
    @Column(name = "expected_delivery_date")
    var expectedDeliveryDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: PurchaseOrderStatus = PurchaseOrderStatus.DRAFT,
    @Column(name = "total_amount", precision = 15, scale = 4, nullable = false)
    var totalAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "currency", length = 3, nullable = false)
    var currency: String = "BRL",
    @Column(name = "payment_terms", length = 100)
    var paymentTerms: String? = null,
    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null,
    @OneToMany(mappedBy = "purchaseOrder", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<PurchaseOrderItem> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "purchase_order_items")
class PurchaseOrderItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @jakarta.persistence.ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    var purchaseOrder: PurchaseOrder,
    @Column(name = "product_id", nullable = false)
    var productId: UUID,
    @Column(name = "target_warehouse_id")
    var targetWarehouseId: UUID? = null,
    @Column(name = "quantity_ordered", precision = 15, scale = 4, nullable = false)
    var quantityOrdered: BigDecimal,
    @Column(name = "quantity_received", precision = 15, scale = 4, nullable = false)
    var quantityReceived: BigDecimal = BigDecimal.ZERO,
    @Column(name = "unit_price", precision = 15, scale = 4, nullable = false)
    var unitPrice: BigDecimal,
    @Column(name = "discount_amount", precision = 15, scale = 4, nullable = false)
    var discountAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_price", precision = 15, scale = 4, nullable = false)
    var totalPrice: BigDecimal = BigDecimal.ZERO,
)
