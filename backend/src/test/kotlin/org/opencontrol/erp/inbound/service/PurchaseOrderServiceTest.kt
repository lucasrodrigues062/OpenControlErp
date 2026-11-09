package org.opencontrol.erp.inbound.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.inbound.api.dto.CreatePurchaseOrderItemRequest
import org.opencontrol.erp.inbound.api.dto.CreatePurchaseOrderRequest
import org.opencontrol.erp.inbound.domain.model.PurchaseOrder
import org.opencontrol.erp.inbound.domain.model.PurchaseOrderStatus
import org.opencontrol.erp.inbound.repository.PurchaseOrderRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class PurchaseOrderServiceTest {
    private val purchaseOrderRepository: PurchaseOrderRepository = mockk()
    private val customerRepository: CustomerRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: PurchaseOrderService

    private val tenantId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "buyer",
                roles = setOf("INBOUND_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = PurchaseOrderService(purchaseOrderRepository, customerRepository, productRepository, auditService)
    }

    @Test
    fun `should create purchase order in draft status with correct totals`() {
        every { purchaseOrderRepository.existsByTenantIdAndOrderNumber(tenantId, any()) } returns false
        every { purchaseOrderRepository.save(any()) } answers { firstArg() }
        every { customerRepository.findByIdAndTenantId(supplierId, tenantId) } returns Optional.of(
            Customer(
                id = supplierId,
                tenantId = tenantId,
                document = "12345678000199", legalName = "Fornecedor Alfa LTDA",
            )
        )
        every { productRepository.findAllById(any()) } returns listOf(
            Product(
                id = productId,
                tenantId = tenantId,
                sku = "PRD-101",
                name = "Cadeira Ergonômica",
                ncm = "94013000",
                categoryId = UUID.randomUUID(),
            )
        )

        val req = CreatePurchaseOrderRequest(
            branchId = branchId,
            supplierId = supplierId,
            orderDate = LocalDate.now(),
            items = listOf(
                CreatePurchaseOrderItemRequest(
                    productId = productId,
                    quantityOrdered = BigDecimal("5.0000"),
                    unitPrice = BigDecimal("200.0000"),
                    discountAmount = BigDecimal("50.0000"),
                )
            ),
        )

        val created = service.createPurchaseOrder(req)
        assertEquals(PurchaseOrderStatus.DRAFT, created.status)
        assertEquals(BigDecimal("950.0000"), created.totalAmount) // 5 * 200 - 50 = 950
        assertEquals("Fornecedor Alfa LTDA", created.supplierName)
        assertEquals(1, created.items.size)
        assertEquals("Cadeira Ergonômica", created.items[0].productName)
        verify { auditService.recordAudit(entityType = "PurchaseOrder", entityId = any(), action = "CREATE", any(), any(), any()) }
    }

    @Test
    fun `should approve purchase order`() {
        val poId = UUID.randomUUID()
        val po = PurchaseOrder(
            id = poId,
            tenantId = tenantId,
            branchId = branchId,
            orderNumber = "PC-001",
            supplierId = supplierId,
            status = PurchaseOrderStatus.DRAFT,
        )
        every { purchaseOrderRepository.findByIdAndTenantId(poId, tenantId) } returns Optional.of(po)
        every { purchaseOrderRepository.save(any()) } answers { firstArg() }
        every { customerRepository.findByIdAndTenantId(supplierId, tenantId) } returns Optional.empty()
        every { productRepository.findAllById(any()) } returns emptyList()

        val approved = service.approvePurchaseOrder(poId)
        assertEquals(PurchaseOrderStatus.APPROVED, approved.status)
    }
}
