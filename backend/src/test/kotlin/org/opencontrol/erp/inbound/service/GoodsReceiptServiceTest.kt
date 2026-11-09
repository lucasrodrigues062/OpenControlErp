package org.opencontrol.erp.inbound.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.inbound.api.dto.GoodsReceiptItemDto
import org.opencontrol.erp.inbound.api.dto.SubmitConferenceRequest
import org.opencontrol.erp.inbound.domain.model.GoodsReceiptStatus
import org.opencontrol.erp.inbound.repository.GoodsReceiptRepository
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class GoodsReceiptServiceTest {
    private val goodsReceiptRepository: GoodsReceiptRepository = mockk()
    private val warehouseRepository: WarehouseRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val productLotRepository: ProductLotRepository = mockk(relaxed = true)
    private val productSerialNumberRepository: ProductSerialNumberRepository = mockk(relaxed = true)
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: GoodsReceiptService

    private val tenantId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "checker",
                roles = setOf("INBOUND_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        every { productLotRepository.save(any()) } answers { firstArg() }
        every { productSerialNumberRepository.save(any()) } answers { firstArg() }
        service = GoodsReceiptService(
            goodsReceiptRepository,
            warehouseRepository,
            productRepository,
            productLotRepository,
            productSerialNumberRepository,
            auditService,
        )
    }

    @Test
    fun `should submit conference and mark OK when expected equals received`() {
        every { goodsReceiptRepository.save(any()) } answers { firstArg() }
        every { warehouseRepository.findByIdAndTenantId(warehouseId, tenantId) } returns Optional.empty()
        every { productRepository.findAllById(any()) } returns emptyList()
        every { productLotRepository.findByTenantIdAndProductIdAndLotNumber(any(), any(), any()) } returns Optional.empty()

        val req = SubmitConferenceRequest(
            branchId = branchId,
            warehouseId = warehouseId,
            items = listOf(
                GoodsReceiptItemDto(
                    productId = productId,
                    expectedQuantity = BigDecimal("10.0000"),
                    receivedQuantity = BigDecimal("10.0000"),
                    lotNumber = "LOT-2026-X1",
                    fabricationDate = LocalDate.now(),
                    expirationDate = LocalDate.now().plusMonths(12),
                )
            ),
        )

        val res = service.submitConference(req)
        assertEquals(GoodsReceiptStatus.CONFERRED_OK, res.status)
        assertEquals(1, res.items.size)
        assertEquals(false, res.items[0].hasDivergence)
    }

    @Test
    fun `should detect divergence when received quantity is less than expected`() {
        every { goodsReceiptRepository.save(any()) } answers { firstArg() }
        every { warehouseRepository.findByIdAndTenantId(warehouseId, tenantId) } returns Optional.empty()
        every { productRepository.findAllById(any()) } returns emptyList()

        val req = SubmitConferenceRequest(
            branchId = branchId,
            warehouseId = warehouseId,
            items = listOf(
                GoodsReceiptItemDto(
                    productId = productId,
                    expectedQuantity = BigDecimal("10.0000"),
                    receivedQuantity = BigDecimal("8.0000"), // 2 missing!
                )
            ),
        )

        val res = service.submitConference(req)
        assertEquals(GoodsReceiptStatus.CONFERRED_WITH_DIVERGENCE, res.status)
        assertEquals(true, res.items[0].hasDivergence)
    }
}
