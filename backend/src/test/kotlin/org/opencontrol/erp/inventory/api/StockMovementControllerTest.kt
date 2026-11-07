package org.opencontrol.erp.inventory.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.inventory.api.dto.CreateStockMovementRequest
import org.opencontrol.erp.inventory.api.dto.StockMovementPageResponse
import org.opencontrol.erp.inventory.api.dto.StockMovementResponse
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.service.KardexMovementService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class StockMovementControllerTest {
    private val service: KardexMovementService = mockk()
    private lateinit var controller: StockMovementController

    private val productId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()

    private val sampleMovement =
        StockMovementResponse(
            id = UUID.randomUUID(),
            branchId = branchId,
            movementType = StockMovementType.INBOUND_PURCHASE,
            productId = productId,
            productSku = "PROD-001",
            productName = "Dell XPS Laptop",
            sourceWarehouseId = null,
            sourceWarehouseName = null,
            destinationWarehouseId = warehouseId,
            destinationWarehouseName = "Depósito Principal",
            locationId = null,
            lotId = null,
            serialNumberId = null,
            quantity = BigDecimal("10.0000"),
            unitCost = BigDecimal("5000.0000"),
            balanceAfter = BigDecimal("10.0000"),
            referenceDocument = "NF-9988",
            reasonCode = null,
            notes = "Compra Fornecedor",
            createdByUserId = UUID.randomUUID(),
            createdAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = StockMovementController(service)
    }

    @Test
    fun `should list movements with pagination`() {
        val pageResponse =
            StockMovementPageResponse(
                items = listOf(sampleMovement),
                totalElements = 1,
                totalPages = 1,
                page = 0,
                size = 50,
            )
        every { service.listMovements(productId, warehouseId, StockMovementType.INBOUND_PURCHASE, any()) } returns pageResponse

        val res = controller.listMovements(productId, warehouseId, StockMovementType.INBOUND_PURCHASE, 0, 50)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertEquals(1, res.body!!.totalElements)
        assertEquals(BigDecimal("10.0000"), res.body!!.items[0].quantity)
    }

    @Test
    fun `should record stock movement`() {
        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.INBOUND_PURCHASE,
                productId = productId,
                destinationWarehouseId = warehouseId,
                quantity = BigDecimal("10.0000"),
                unitCost = BigDecimal("5000.0000"),
                referenceDocument = "NF-9988",
            )
        every { service.recordMovement(req) } returns sampleMovement

        val res = controller.recordMovement(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals(sampleMovement.id, res.body!!.id)
    }
}
