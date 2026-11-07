package org.opencontrol.erp.inventory.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.inventory.api.dto.StockBalanceResponse
import org.opencontrol.erp.inventory.service.StockBalanceService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class StockBalanceControllerTest {
    private val service: StockBalanceService = mockk()
    private lateinit var controller: StockBalanceController

    private val productId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()

    private val sampleBalance =
        StockBalanceResponse(
            id = UUID.randomUUID(),
            warehouseId = warehouseId,
            warehouseName = "Depósito Principal",
            productId = productId,
            productSku = "PROD-001",
            productName = "Dell XPS Laptop",
            locationId = null,
            locationAddress = null,
            quantityOnHand = BigDecimal("50.0000"),
            quantityReserved = BigDecimal("10.0000"),
            quantityAvailable = BigDecimal("40.0000"),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = StockBalanceController(service)
    }

    @Test
    fun `should list balances`() {
        every { service.listBalances(warehouseId, productId) } returns listOf(sampleBalance)

        val res = controller.listBalances(warehouseId, productId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertEquals(1, res.body!!.size)
        assertEquals(BigDecimal("50.0000"), res.body!![0].quantityOnHand)
    }

    @Test
    fun `should get product balances across warehouses`() {
        every { service.getProductBalances(productId) } returns listOf(sampleBalance)

        val res = controller.getProductBalances(productId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
    }
}
