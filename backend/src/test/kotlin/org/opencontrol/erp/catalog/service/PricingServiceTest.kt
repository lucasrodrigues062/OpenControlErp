package org.opencontrol.erp.catalog.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.api.dto.CreatePriceListRequest
import org.opencontrol.erp.catalog.domain.model.PriceList
import org.opencontrol.erp.catalog.domain.model.ProductPrice
import org.opencontrol.erp.catalog.repository.PriceListRepository
import org.opencontrol.erp.catalog.repository.ProductPriceRepository
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class PricingServiceTest {
    private val priceListRepository: PriceListRepository = mockk()
    private val productPriceRepository: ProductPriceRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: PricingService
    private val tenantId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("CATALOG_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = PricingService(priceListRepository, productPriceRepository, productRepository, auditService)
    }

    @Test
    fun `should calculate markup price correctly`() {
        val cost = BigDecimal("100.00")
        val markup = BigDecimal("50.00") // 50%
        val salesPrice = service.calculateMarkupPrice(cost, markup)
        assertEquals(BigDecimal("150.00"), salesPrice)
    }

    @Test
    fun `should calculate Weighted Average Cost CMP correctly`() {
        // Current: 100 units at R$ 10.00 = R$ 1000
        // Inbound: 50 units at R$ 16.00 = R$ 800
        // Total: 150 units with R$ 1800 => CMP = R$ 12.0000
        val cmp =
            service.calculateWeightedAverageCost(
                currentQty = BigDecimal("100"),
                currentCmp = BigDecimal("10.0000"),
                inboundQty = BigDecimal("50"),
                inboundCost = BigDecimal("16.0000"),
            )
        assertEquals(BigDecimal("12.0000"), cmp)
    }

    @Test
    fun `should create price list and handle default list`() {
        every { priceListRepository.existsByTenantIdAndCode(tenantId, "VAREJO") } returns false
        every { priceListRepository.findByTenantIdAndIsDefaultTrue(tenantId) } returns Optional.empty()
        every { priceListRepository.save(any()) } answers { firstArg() }

        val req =
            CreatePriceListRequest(
                code = "VAREJO",
                name = "Tabela de Venda Varejo",
                isDefault = true,
            )

        val created = service.createPriceList(req)
        assertEquals("VAREJO", created.code)
        assertTrue(created.isDefault)
        verify {
            auditService.recordAudit(
                entityType = "PriceList",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should return prices by price list`() {
        val priceListId = UUID.randomUUID()
        val productPrice =
            ProductPrice(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                priceListId = priceListId,
                productId = productId,
                salesPrice = BigDecimal("199.90"),
            )
        every { productPriceRepository.findAllByTenantIdAndPriceListId(tenantId, priceListId) } returns listOf(productPrice)

        val list = service.getPricesByPriceList(priceListId)
        assertEquals(1, list.size)
        assertEquals(BigDecimal("199.90"), list[0].salesPrice)
    }

    @Test
    fun `should resolve promotional price when promo is active`() {
        val priceListId = UUID.randomUUID()
        val priceList =
            PriceList(
                id = priceListId,
                tenantId = tenantId,
                code = "PROMO",
                name = "Campanha Promocional",
                isDefault = true,
            )
        val today = LocalDate.now()
        val productPrice =
            ProductPrice(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                priceListId = priceListId,
                productId = productId,
                salesPrice = BigDecimal("100.00"),
                promotionalPrice = BigDecimal("79.90"),
                promoStart = today.minusDays(2),
                promoEnd = today.plusDays(5),
            )

        every { priceListRepository.findByIdAndTenantId(priceListId, tenantId) } returns Optional.of(priceList)
        every { productPriceRepository.findByPriceListIdAndProductId(priceListId, productId) } returns Optional.of(productPrice)

        val resolved = service.resolveEffectivePrice(productId, priceListId)
        assertEquals(BigDecimal("79.90"), resolved.effectivePrice)
        assertTrue(resolved.isPromotional)
    }

    @Test
    fun `should resolve regular sales price when promotional date has expired`() {
        val priceListId = UUID.randomUUID()
        val priceList =
            PriceList(
                id = priceListId,
                tenantId = tenantId,
                code = "PROMO",
                name = "Campanha Promocional",
                isDefault = true,
            )
        val today = LocalDate.now()
        val productPrice =
            ProductPrice(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                priceListId = priceListId,
                productId = productId,
                salesPrice = BigDecimal("100.00"),
                promotionalPrice = BigDecimal("79.90"),
                promoStart = today.minusDays(10),
                promoEnd = today.minusDays(1), // Already ended
            )

        every { priceListRepository.findByIdAndTenantId(priceListId, tenantId) } returns Optional.of(priceList)
        every { productPriceRepository.findByPriceListIdAndProductId(priceListId, productId) } returns Optional.of(productPrice)

        val resolved = service.resolveEffectivePrice(productId, priceListId)
        assertEquals(BigDecimal("100.00"), resolved.effectivePrice)
        assertFalse(resolved.isPromotional)
    }
}
