package org.opencontrol.erp.catalog.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "price_lists")
class PriceList(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "code", length = 50, nullable = false)
    var code: String,
    @Column(name = "name", length = 150, nullable = false)
    var name: String,
    @Column(name = "currency", length = 3, nullable = false)
    var currency: String = "BRL",
    @Column(name = "discount_or_markup_pct", precision = 5, scale = 2, nullable = false)
    var discountOrMarkupPct: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "valid_from")
    var validFrom: LocalDate? = null,
    @Column(name = "valid_until")
    var validUntil: LocalDate? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
