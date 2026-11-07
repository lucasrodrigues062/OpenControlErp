package org.opencontrol.erp.catalog.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_unit_conversions")
class ProductUnitConversion(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "unit_from", length = 10, nullable = false)
    var unitFrom: String,
    @Column(name = "unit_to", length = 10, nullable = false)
    var unitTo: String,
    @Column(name = "conversion_factor", precision = 15, scale = 6, nullable = false)
    var conversionFactor: BigDecimal,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
