package org.opencontrol.erp.catalog.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.opencontrol.erp.catalog.domain.model.PriceList
import org.opencontrol.erp.catalog.domain.model.ProductPrice
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PriceListResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val currency: String,
    val discountOrMarkupPct: BigDecimal,
    val isDefault: Boolean,
    val validFrom: LocalDate?,
    val validUntil: LocalDate?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(p: PriceList) =
            PriceListResponse(
                id = p.id,
                code = p.code,
                name = p.name,
                currency = p.currency,
                discountOrMarkupPct = p.discountOrMarkupPct,
                isDefault = p.isDefault,
                validFrom = p.validFrom,
                validUntil = p.validUntil,
                isActive = p.isActive,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt,
            )
    }
}

data class CreatePriceListRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val code: String,
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,
    @field:Size(max = 3)
    val currency: String = "BRL",
    val discountOrMarkupPct: BigDecimal = BigDecimal.ZERO,
    val isDefault: Boolean = false,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
)

data class ProductPriceResponse(
    val id: UUID,
    val priceListId: UUID,
    val productId: UUID,
    val salesPrice: BigDecimal,
    val promotionalPrice: BigDecimal?,
    val promoStart: LocalDate?,
    val promoEnd: LocalDate?,
    val effectivePrice: BigDecimal,
    val isPromotionalActive: Boolean,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(
            p: ProductPrice,
            today: LocalDate = LocalDate.now(),
        ): ProductPriceResponse {
            val isPromoActive =
                p.promotionalPrice != null &&
                    (p.promoStart == null || !today.isBefore(p.promoStart)) &&
                    (p.promoEnd == null || !today.isAfter(p.promoEnd))

            val effective = if (isPromoActive) p.promotionalPrice!! else p.salesPrice

            return ProductPriceResponse(
                id = p.id,
                priceListId = p.priceListId,
                productId = p.productId,
                salesPrice = p.salesPrice,
                promotionalPrice = p.promotionalPrice,
                promoStart = p.promoStart,
                promoEnd = p.promoEnd,
                effectivePrice = effective,
                isPromotionalActive = isPromoActive,
                updatedAt = p.updatedAt,
            )
        }
    }
}

data class SetProductPriceRequest(
    @field:NotNull
    @field:PositiveOrZero
    val salesPrice: BigDecimal,
    val promotionalPrice: BigDecimal? = null,
    val promoStart: LocalDate? = null,
    val promoEnd: LocalDate? = null,
)

data class ResolvePriceResponse(
    val productId: UUID,
    val priceListId: UUID,
    val effectivePrice: BigDecimal,
    val currency: String,
    val isPromotional: Boolean,
)
