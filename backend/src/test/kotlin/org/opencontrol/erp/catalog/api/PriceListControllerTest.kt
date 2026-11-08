package org.opencontrol.erp.catalog.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.catalog.api.dto.CreatePriceListRequest
import org.opencontrol.erp.catalog.api.dto.PriceListResponse
import org.opencontrol.erp.catalog.api.dto.ProductPriceResponse
import org.opencontrol.erp.catalog.api.dto.ResolvePriceResponse
import org.opencontrol.erp.catalog.api.dto.SetProductPriceRequest
import org.opencontrol.erp.catalog.service.PricingService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PriceListControllerTest {
    private val pricingService: PricingService = mockk()
    private lateinit var controller: PriceListController

    private val priceListId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    private val samplePriceList =
        PriceListResponse(
            id = priceListId,
            code = "VAREJO",
            name = "Tabela Varejo Geral",
            currency = "BRL",
            discountOrMarkupPct = BigDecimal("10.00"),
            isDefault = true,
            validFrom = LocalDate.now().minusDays(1),
            validUntil = LocalDate.now().plusMonths(6),
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private val sampleProductPrice =
        ProductPriceResponse(
            id = UUID.randomUUID(),
            priceListId = priceListId,
            productId = productId,
            salesPrice = BigDecimal("150.0000"),
            promotionalPrice = BigDecimal("120.0000"),
            promoStart = LocalDate.now().minusDays(1),
            promoEnd = LocalDate.now().plusDays(5),
            effectivePrice = BigDecimal("120.0000"),
            isPromotionalActive = true,
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = PriceListController(pricingService)
    }

    @Test
    fun `should list price lists`() {
        every { pricingService.listPriceLists() } returns listOf(samplePriceList)

        val res = controller.listPriceLists()
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertEquals(1, res.body!!.size)
        assertEquals("VAREJO", res.body!![0].code)
    }

    @Test
    fun `should create price list`() {
        val req =
            CreatePriceListRequest(
                code = "ATACADO",
                name = "Tabela Atacado",
                currency = "BRL",
                discountOrMarkupPct = BigDecimal.ZERO,
                isDefault = false,
            )
        every { pricingService.createPriceList(req) } returns samplePriceList

        val res = controller.createPriceList(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals(priceListId, res.body!!.id)
    }

    @Test
    fun `should set product price`() {
        val req =
            SetProductPriceRequest(
                salesPrice = BigDecimal("150.00"),
                promotionalPrice = BigDecimal("120.00"),
                promoStart = LocalDate.now().minusDays(1),
                promoEnd = LocalDate.now().plusDays(5),
            )
        every { pricingService.setProductPrice(priceListId, productId, req) } returns sampleProductPrice

        val res = controller.setPrice(priceListId, productId, req)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(BigDecimal("150.0000"), res.body!!.salesPrice)
    }

    @Test
    fun `should get prices by price list`() {
        every { pricingService.getPricesByPriceList(priceListId) } returns listOf(sampleProductPrice)

        val res = controller.getPricesByPriceList(priceListId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
        assertEquals(sampleProductPrice.id, res.body!![0].id)
    }

    @Test
    fun `should get product prices across lists`() {
        every { pricingService.getProductPrices(productId) } returns listOf(sampleProductPrice)

        val res = controller.getProductPrices(productId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
    }

    @Test
    fun `should resolve effective price`() {
        val resolveRes =
            ResolvePriceResponse(
                productId = productId,
                priceListId = priceListId,
                effectivePrice = BigDecimal("120.0000"),
                currency = "BRL",
                isPromotional = true,
            )
        every { pricingService.resolveEffectivePrice(productId, priceListId) } returns resolveRes

        val res = controller.resolvePrice(productId, priceListId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(BigDecimal("120.0000"), res.body!!.effectivePrice)
        assertEquals(true, res.body!!.isPromotional)
    }
}
