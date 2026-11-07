package org.opencontrol.erp.catalog.domain.model

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
@Table(name = "products")
class Product(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id")
    var branchId: UUID? = null,
    @Column(name = "sku", length = 50, nullable = false)
    var sku: String,
    @Column(name = "barcode", length = 50)
    var barcode: String? = null,
    @Column(name = "name", length = 200, nullable = false)
    var name: String,
    @Column(name = "description", columnDefinition = "TEXT")
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20, nullable = false)
    var itemType: SpedItemType = SpedItemType.REV_00,
    @Column(name = "unit_of_measure", length = 10, nullable = false)
    var unitOfMeasure: String = "UN",
    @Column(name = "category_id", nullable = false)
    var categoryId: UUID,
    @Column(name = "brand", length = 100)
    var brand: String? = null,
    @Column(name = "model", length = 100)
    var model: String? = null,
    @Column(name = "ncm", length = 8, nullable = false)
    var ncm: String,
    @Column(name = "ncm_exception", length = 3)
    var ncmException: String? = null,
    @Column(name = "cest", length = 7)
    var cest: String? = null,
    @Column(name = "origin", nullable = false)
    var origin: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(name = "tracking_mode", length = 20, nullable = false)
    var trackingMode: ProductTrackingMode = ProductTrackingMode.NONE,
    @Column(name = "gross_weight", precision = 12, scale = 4)
    var grossWeight: BigDecimal? = null,
    @Column(name = "net_weight", precision = 12, scale = 4)
    var netWeight: BigDecimal? = null,
    @Column(name = "width", precision = 10, scale = 2)
    var width: BigDecimal? = null,
    @Column(name = "height", precision = 10, scale = 2)
    var height: BigDecimal? = null,
    @Column(name = "depth", precision = 10, scale = 2)
    var depth: BigDecimal? = null,
    @Column(name = "volume", precision = 12, scale = 4)
    var volume: BigDecimal? = null,
    @Column(name = "min_stock", precision = 15, scale = 4, nullable = false)
    var minStock: BigDecimal = BigDecimal.ZERO,
    @Column(name = "max_stock", precision = 15, scale = 4)
    var maxStock: BigDecimal? = null,
    @Column(name = "reorder_point", precision = 15, scale = 4)
    var reorderPoint: BigDecimal? = null,
    @Column(name = "last_acquisition_cost", precision = 15, scale = 4, nullable = false)
    var lastAcquisitionCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "average_cost", precision = 15, scale = 4, nullable = false)
    var averageCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "replacement_cost", precision = 15, scale = 4, nullable = false)
    var replacementCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
