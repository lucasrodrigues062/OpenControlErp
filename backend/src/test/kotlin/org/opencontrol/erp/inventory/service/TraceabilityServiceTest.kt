package org.opencontrol.erp.inventory.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.inventory.api.dto.CreateLotRequest
import org.opencontrol.erp.inventory.api.dto.CreateSerialNumberRequest
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.ProductSerialNumber
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.opencontrol.erp.inventory.exception.DuplicateSerialNumberException
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class TraceabilityServiceTest {
    private val lotRepository: ProductLotRepository = mockk()
    private val serialRepository: ProductSerialNumberRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: TraceabilityService
    private val tenantId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("INVENTORY_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = TraceabilityService(lotRepository, serialRepository, auditService)
    }

    @Test
    fun `should list lots in FEFO expiration order and mark expired`() {
        val today = LocalDate.now()
        val lot1 =
            ProductLot(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                lotNumber = "LOT-EXPIRED",
                manufacturingDate = today.minusMonths(6),
                expirationDate = today.minusDays(5),
                quantityOnHand = BigDecimal("10.0000"),
                quantityReserved = BigDecimal.ZERO,
                status = LotStatus.RELEASED,
            )
        val lot2 =
            ProductLot(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                lotNumber = "LOT-VALID",
                manufacturingDate = today.minusMonths(1),
                expirationDate = today.plusMonths(6),
                quantityOnHand = BigDecimal("50.0000"),
                quantityReserved = BigDecimal.ZERO,
                status = LotStatus.RELEASED,
            )

        every { lotRepository.findAllByTenantIdAndProductIdOrderByExpirationDateAsc(tenantId, productId) } returns listOf(lot1, lot2)
        every { lotRepository.save(any()) } answers { firstArg() }

        val list = service.listLots(productId, warehouseId = null, availableOnly = false)
        assertEquals(2, list.size)
        assertEquals(LotStatus.EXPIRED, list[0].status)
        assertEquals(LotStatus.RELEASED, list[1].status)
    }

    @Test
    fun `should create lot successfully`() {
        val today = LocalDate.now()
        every { lotRepository.existsByTenantIdAndProductIdAndLotNumber(tenantId, productId, "LOT-2026-A") } returns false
        every { lotRepository.save(any()) } answers { firstArg() }

        val req =
            CreateLotRequest(
                warehouseId = warehouseId,
                productId = productId,
                lotNumber = "LOT-2026-A",
                manufacturingDate = today,
                expirationDate = today.plusYears(1),
                quantityOnHand = BigDecimal("100"),
            )

        val created = service.createLot(req)
        assertNotNull(created.id)
        assertEquals("LOT-2026-A", created.lotNumber)
        assertEquals(LotStatus.RELEASED, created.status)
        verify {
            auditService.recordAudit(
                entityType = "ProductLot",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should reject lot creation when expiration date before manufacturing date`() {
        val today = LocalDate.now()
        val req =
            CreateLotRequest(
                warehouseId = warehouseId,
                productId = productId,
                lotNumber = "LOT-INVALID-DATES",
                manufacturingDate = today,
                expirationDate = today.minusDays(1),
            )

        assertThrows<IllegalArgumentException> {
            service.createLot(req)
        }
    }

    @Test
    fun `should register and find serial number`() {
        every { serialRepository.existsByTenantIdAndProductIdAndSerialNumber(tenantId, productId, "SN-998877") } returns false
        every { serialRepository.save(any()) } answers { firstArg() }

        val req =
            CreateSerialNumberRequest(
                warehouseId = warehouseId,
                productId = productId,
                serialNumber = "SN-998877",
                warrantyUntil = LocalDate.now().plusYears(2),
            )

        val created = service.createSerialNumber(req)
        assertEquals("SN-998877", created.serialNumber)
        assertEquals(SerialNumberStatus.IN_STOCK, created.status)
        verify {
            auditService.recordAudit(
                entityType = "ProductSerialNumber",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should reject duplicate serial number for product`() {
        every { serialRepository.existsByTenantIdAndProductIdAndSerialNumber(tenantId, productId, "SN-DUP") } returns true

        val req =
            CreateSerialNumberRequest(
                warehouseId = warehouseId,
                productId = productId,
                serialNumber = "SN-DUP",
            )

        assertThrows<DuplicateSerialNumberException> {
            service.createSerialNumber(req)
        }
    }

    @Test
    fun `should update serial number status`() {
        val serial =
            ProductSerialNumber(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                serialNumber = "SN-100",
                status = SerialNumberStatus.IN_STOCK,
            )
        every { serialRepository.findByIdAndTenantId(serial.id, tenantId) } returns Optional.of(serial)
        every { serialRepository.save(any()) } answers { firstArg() }

        val updated = service.updateSerialNumberStatus(serial.id, SerialNumberStatus.DISPATCHED_SOLD)
        assertEquals(SerialNumberStatus.DISPATCHED_SOLD, updated.status)
        verify {
            auditService.recordAudit(
                entityType = "ProductSerialNumber",
                entityId = any(),
                action = "UPDATE_STATUS",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should list serial numbers by multiple filters`() {
        val serial =
            ProductSerialNumber(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                serialNumber = "SN-100",
                status = SerialNumberStatus.IN_STOCK,
            )

        // 1. By serial number
        every { serialRepository.findByTenantIdAndSerialNumber(tenantId, "SN-100") } returns Optional.of(serial)
        val res1 = service.listSerialNumbers(serialNumber = "SN-100")
        assertEquals(1, res1.size)

        // 2. By product and status
        every {
            serialRepository.findAllByTenantIdAndProductIdAndStatusOrderBySerialNumberAsc(tenantId, productId, SerialNumberStatus.IN_STOCK)
        } returns listOf(serial)
        val res2 = service.listSerialNumbers(productId = productId, status = SerialNumberStatus.IN_STOCK)
        assertEquals(1, res2.size)

        // 3. By product only
        every { serialRepository.findAllByTenantIdAndProductIdOrderBySerialNumberAsc(tenantId, productId) } returns listOf(serial)
        val res3 = service.listSerialNumbers(productId = productId)
        assertEquals(1, res3.size)

        // 4. All
        every { serialRepository.findAll() } returns listOf(serial)
        val res4 = service.listSerialNumbers()
        assertEquals(1, res4.size)
    }
}
