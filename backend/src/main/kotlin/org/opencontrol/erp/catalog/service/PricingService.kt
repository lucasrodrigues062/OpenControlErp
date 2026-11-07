package org.opencontrol.erp.catalog.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.api.dto.CreatePriceListRequest
import org.opencontrol.erp.catalog.api.dto.PriceListResponse
import org.opencontrol.erp.catalog.api.dto.ProductPriceResponse
import org.opencontrol.erp.catalog.api.dto.ResolvePriceResponse
import org.opencontrol.erp.catalog.api.dto.SetProductPriceRequest
import org.opencontrol.erp.catalog.domain.model.PriceList
import org.opencontrol.erp.catalog.domain.model.ProductPrice
import org.opencontrol.erp.catalog.repository.PriceListRepository
import org.opencontrol.erp.catalog.repository.ProductPriceRepository
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class PricingService(
    private val priceListRepository: PriceListRepository,
    private val productPriceRepository: ProductPriceRepository,
    private val productRepository: ProductRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listPriceLists(): List<PriceListResponse> {
        val tenantId = getCurrentTenantId()
        return priceListRepository
            .findAllByTenantIdOrderByCodeAsc(tenantId)
            .map { PriceListResponse.fromEntity(it) }
    }

    fun createPriceList(request: CreatePriceListRequest): PriceListResponse {
        val tenantId = getCurrentTenantId()
        val code = request.code.trim().uppercase()

        if (priceListRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw IllegalArgumentException("Tabela de preços com código '$code' já existe.")
        }

        if (request.isDefault) {
            priceListRepository.findByTenantIdAndIsDefaultTrue(tenantId).ifPresent {
                it.isDefault = false
                priceListRepository.save(it)
            }
        }

        val priceList =
            PriceList(
                tenantId = tenantId,
                code = code,
                name = request.name.trim(),
                currency = request.currency.trim().uppercase(),
                discountOrMarkupPct = request.discountOrMarkupPct,
                isDefault = request.isDefault,
                validFrom = request.validFrom,
                validUntil = request.validUntil,
                isActive = true,
            )

        val saved = priceListRepository.save(priceList)

        auditService.recordAudit(
            entityType = "PriceList",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created price list ${saved.code} - ${saved.name}",
            stateAfter = saved,
        )

        return PriceListResponse.fromEntity(saved)
    }

    fun setProductPrice(
        priceListId: UUID,
        productId: UUID,
        request: SetProductPriceRequest,
    ): ProductPriceResponse {
        val tenantId = getCurrentTenantId()

        val existing = productPriceRepository.findByPriceListIdAndProductId(priceListId, productId)
        val price =
            existing.orElseGet {
                ProductPrice(
                    tenantId = tenantId,
                    priceListId = priceListId,
                    productId = productId,
                    salesPrice = request.salesPrice,
                )
            }

        price.salesPrice = request.salesPrice
        price.promotionalPrice = request.promotionalPrice
        price.promoStart = request.promoStart
        price.promoEnd = request.promoEnd
        price.updatedAt = Instant.now()

        val saved = productPriceRepository.save(price)

        auditService.recordAudit(
            entityType = "ProductPrice",
            entityId = saved.id.toString(),
            action = "SET_PRICE",
            details = "Configured price for product $productId in list $priceListId: R$ ${request.salesPrice}",
            stateAfter = saved,
        )

        return ProductPriceResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun getProductPrices(productId: UUID): List<ProductPriceResponse> {
        val tenantId = getCurrentTenantId()
        return productPriceRepository
            .findAllByTenantIdAndProductId(tenantId, productId)
            .map { ProductPriceResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun resolveEffectivePrice(
        productId: UUID,
        targetPriceListId: UUID? = null,
    ): ResolvePriceResponse {
        val tenantId = getCurrentTenantId()
        val priceList =
            if (targetPriceListId != null) {
                priceListRepository
                    .findByIdAndTenantId(targetPriceListId, tenantId)
                    .orElseThrow { IllegalArgumentException("Tabela de preços '$targetPriceListId' não encontrada.") }
            } else {
                priceListRepository
                    .findByTenantIdAndIsDefaultTrue(tenantId)
                    .orElse(null)
            }

        val today = LocalDate.now()

        if (priceList != null) {
            val productPrice = productPriceRepository.findByPriceListIdAndProductId(priceList.id, productId)
            if (productPrice.isPresent) {
                val res = ProductPriceResponse.fromEntity(productPrice.get(), today)
                return ResolvePriceResponse(
                    productId = productId,
                    priceListId = priceList.id,
                    effectivePrice = res.effectivePrice,
                    currency = priceList.currency,
                    isPromotional = res.isPromotionalActive,
                )
            }
        }

        // Fallback: product cost + default 30% markup or 0
        val product =
            productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow { IllegalArgumentException("Produto '$productId' não encontrado.") }

        val fallbackPrice =
            if (product.averageCost > BigDecimal.ZERO) {
                calculateMarkupPrice(product.averageCost, BigDecimal("30.00"))
            } else {
                BigDecimal.ZERO
            }

        return ResolvePriceResponse(
            productId = productId,
            priceListId = priceList?.id ?: UUID.fromString("00000000-0000-0000-0000-000000000000"),
            effectivePrice = fallbackPrice,
            currency = priceList?.currency ?: "BRL",
            isPromotional = false,
        )
    }

    /**
     * Calculates suggested sales price from cost base using markup percentage.
     * Formula: Sales Price = Cost * (1 + Markup / 100)
     */
    fun calculateMarkupPrice(
        cost: BigDecimal,
        markupPct: BigDecimal,
    ): BigDecimal {
        if (cost <= BigDecimal.ZERO) return BigDecimal.ZERO
        val multiplier = BigDecimal.ONE.add(markupPct.divide(BigDecimal("100"), 6, RoundingMode.HALF_UP))
        return cost.multiply(multiplier).setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * Calculates statutory Weighted Average Cost (Custo Médio Ponderado - CMP) following RIR/Decreto 9.580/2018.
     * Formula: CMP = (Current Qty * Current CMP + Inbound Qty * Inbound Unit Cost) / (Current Qty + Inbound Qty)
     */
    fun calculateWeightedAverageCost(
        currentQty: BigDecimal,
        currentCmp: BigDecimal,
        inboundQty: BigDecimal,
        inboundCost: BigDecimal,
    ): BigDecimal {
        val totalQty = currentQty.add(inboundQty)
        if (totalQty <= BigDecimal.ZERO) return inboundCost

        val currentTotalValue = currentQty.multiply(currentCmp)
        val inboundTotalValue = inboundQty.multiply(inboundCost)
        val totalValue = currentTotalValue.add(inboundTotalValue)

        return totalValue.divide(totalQty, 4, RoundingMode.HALF_UP)
    }
}
