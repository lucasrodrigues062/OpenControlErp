package org.opencontrol.erp.inventory.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.inventory.api.dto.CreateLotRequest
import org.opencontrol.erp.inventory.api.dto.CreateSerialNumberRequest
import org.opencontrol.erp.inventory.api.dto.ProductLotResponse
import org.opencontrol.erp.inventory.api.dto.ProductSerialNumberResponse
import org.opencontrol.erp.inventory.api.dto.UpdateSerialStatusRequest
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.opencontrol.erp.inventory.service.TraceabilityService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class TraceabilityControllerTest {
    private val service: TraceabilityService = mockk()
    private lateinit var controller: TraceabilityController

    private val productId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()
    private val lotId = UUID.randomUUID()
    private val serialId = UUID.randomUUID()

    private val sampleLot =
        ProductLotResponse(
            id = lotId,
            warehouseId = warehouseId,
            productId = productId,
            lotNumber = "LOT-2026-001",
            manufacturingDate = LocalDate.now().minusMonths(1),
            expirationDate = LocalDate.now().plusMonths(11),
            quantityOnHand = BigDecimal("85.0000"),
            quantityReserved = BigDecimal.ZERO,
            status = LotStatus.RELEASED,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private val sampleSerial =
        ProductSerialNumberResponse(
            id = serialId,
            warehouseId = warehouseId,
            productId = productId,
            lotId = lotId,
            serialNumber = "SN-99887766",
            status = SerialNumberStatus.IN_STOCK,
            warrantyUntil = LocalDate.now().plusYears(1),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = TraceabilityController(service)
    }

    @Test
    fun `should list lots`() {
        every { service.listLots(productId, warehouseId, false) } returns listOf(sampleLot)

        val res = controller.listLots(productId, warehouseId, false)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertEquals(1, res.body!!.size)
        assertEquals("LOT-2026-001", res.body!![0].lotNumber)
    }

    @Test
    fun `should create lot`() {
        val req =
            CreateLotRequest(
                warehouseId = warehouseId,
                productId = productId,
                lotNumber = "LOT-2026-001",
                manufacturingDate = LocalDate.now().minusMonths(1),
                expirationDate = LocalDate.now().plusMonths(11),
                quantityOnHand = BigDecimal("100.0000"),
            )
        every { service.createLot(req) } returns sampleLot

        val res = controller.createLot(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("LOT-2026-001", res.body!!.lotNumber)
    }

    @Test
    fun `should list serial numbers`() {
        every { service.listSerialNumbers(productId, "SN-99887766", SerialNumberStatus.IN_STOCK) } returns listOf(sampleSerial)

        val res = controller.listSerialNumbers(productId, "SN-99887766", SerialNumberStatus.IN_STOCK)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
        assertEquals("SN-99887766", res.body!![0].serialNumber)
    }

    @Test
    fun `should create serial number`() {
        val req =
            CreateSerialNumberRequest(
                warehouseId = warehouseId,
                productId = productId,
                lotId = lotId,
                serialNumber = "SN-99887766",
            )
        every { service.createSerialNumber(req) } returns sampleSerial

        val res = controller.createSerialNumber(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("SN-99887766", res.body!!.serialNumber)
    }

    @Test
    fun `should update serial status`() {
        val req = UpdateSerialStatusRequest(status = SerialNumberStatus.DISPATCHED_SOLD)
        val updated = sampleSerial.copy(status = SerialNumberStatus.DISPATCHED_SOLD)
        every { service.updateSerialNumberStatus(serialId, SerialNumberStatus.DISPATCHED_SOLD) } returns updated

        val res = controller.updateSerialStatus(serialId, req)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(SerialNumberStatus.DISPATCHED_SOLD, res.body!!.status)
    }
}
