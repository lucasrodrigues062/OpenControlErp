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
@Table(name = "product_prices")
class ProductPrice(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "price_list_id", nullable = false)
    val priceListId: UUID,
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "sales_price", precision = 15, scale = 4, nullable = false)
    var salesPrice: BigDecimal,
    @Column(name = "promotional_price", precision = 15, scale = 4)
    var promotionalPrice: BigDecimal? = null,
    @Column(name = "promo_start")
    var promoStart: LocalDate? = null,
    @Column(name = "promo_end")
    var promoEnd: LocalDate? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
