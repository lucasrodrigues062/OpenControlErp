package org.opencontrol.erp.inbound.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.inbound.api.dto.CreatePurchaseOrderItemRequest
import org.opencontrol.erp.inbound.api.dto.CreatePurchaseOrderRequest
import org.opencontrol.erp.inbound.api.dto.PurchaseOrderResponse
import org.opencontrol.erp.inbound.domain.model.PurchaseOrderStatus
import org.opencontrol.erp.inbound.service.PurchaseOrderService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PurchaseOrderControllerTest {
    private val service: PurchaseOrderService = mockk()
    private lateinit var controller: PurchaseOrderController

    private val poId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()

    private val sampleResponse = PurchaseOrderResponse(
        id = poId,
        branchId = branchId,
        orderNumber = "PC-12345",
        supplierId = supplierId,
        supplierName = "Fornecedor Beta",
        orderDate = LocalDate.now(),
        expectedDeliveryDate = LocalDate.now().plusDays(7),
        status = PurchaseOrderStatus.DRAFT,
        totalAmount = BigDecimal("1500.00"),
        currency = "BRL",
        paymentTerms = "30 dias",
        notes = "Urgente",
        items = emptyList(),
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
    )

    @BeforeEach
    fun setUp() {
        controller = PurchaseOrderController(service)
    }

    @Test
    fun `should list purchase orders`() {
        every { service.listPurchaseOrders(null, null) } returns listOf(sampleResponse)
        val res = controller.list(null, null)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
        assertEquals("PC-12345", res.body!![0].orderNumber)
    }

    @Test
    fun `should create purchase order`() {
        val req = CreatePurchaseOrderRequest(
            branchId = branchId,
            supplierId = supplierId,
            items = listOf(
                CreatePurchaseOrderItemRequest(
                    productId = UUID.randomUUID(),
                    quantityOrdered = BigDecimal.TEN,
                    unitPrice = BigDecimal("150.00"),
                )
            ),
        )
        every { service.createPurchaseOrder(req) } returns sampleResponse
        val res = controller.create(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals(poId, res.body!!.id)
    }

    @Test
    fun `should approve purchase order`() {
        val approved = sampleResponse.copy(status = PurchaseOrderStatus.APPROVED)
        every { service.approvePurchaseOrder(poId) } returns approved
        val res = controller.approve(poId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(PurchaseOrderStatus.APPROVED, res.body!!.status)
    }
}
