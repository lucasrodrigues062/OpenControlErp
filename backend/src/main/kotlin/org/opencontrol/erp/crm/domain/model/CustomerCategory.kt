package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "customer_categories")
class CustomerCategory(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "code", length = 30, nullable = false)
    var code: String,
    @Column(name = "name", length = 100, nullable = false)
    var name: String,
    @Column(name = "description", length = 255)
    var description: String? = null,
    @Column(name = "color_hex", length = 7, nullable = false)
    var colorHex: String = "#3B82F6",
    @Column(name = "default_discount_pct", precision = 5, scale = 2, nullable = false)
    var defaultDiscountPct: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
