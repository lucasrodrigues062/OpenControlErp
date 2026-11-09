package org.opencontrol.erp.inbound.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.opencontrol.erp.catalog.repository.ProductCategoryRepository
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.service.PricingService
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.inbound.api.dto.FinalizeInvoiceStockRequest
import org.opencontrol.erp.inbound.api.dto.MatchProductRequest
import org.opencontrol.erp.inbound.domain.CostFormulaEngine
import org.opencontrol.erp.inbound.domain.model.InboundInvoice
import org.opencontrol.erp.inbound.domain.model.InboundInvoiceItem
import org.opencontrol.erp.inbound.domain.model.InboundInvoiceStatus
import org.opencontrol.erp.inbound.repository.CostFormulaProfileRepository
import org.opencontrol.erp.inbound.repository.InboundInvoiceRepository
import org.opencontrol.erp.inbound.repository.SupplierProductMappingRepository
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.repository.StockBalanceRepository
import org.opencontrol.erp.inventory.repository.StockMovementRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class InboundInvoiceServiceTest {
    private val invoiceRepository: InboundInvoiceRepository = mockk()
    private val customerRepository: CustomerRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val categoryRepository: ProductCategoryRepository = mockk(relaxed = true)
    private val supplierProductMappingRepository: SupplierProductMappingRepository = mockk()
    private val costFormulaProfileRepository: CostFormulaProfileRepository = mockk()
    private val stockBalanceRepository: StockBalanceRepository = mockk()
    private val stockMovementRepository: StockMovementRepository = mockk()
    private val xmlParserService: NFeXmlParserService = mockk()
    private val formulaEngine: CostFormulaEngine = CostFormulaEngine()
    private val pricingService: PricingService = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: InboundInvoiceService

    private val tenantId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()
    private val supplierId = UUID.randomUUID()
    private val productId = UUID.randomUUID()
    private val invoiceId = UUID.randomUUID()
    private val itemId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "fiscal",
                roles = setOf("INBOUND_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = InboundInvoiceService(
            invoiceRepository,
            customerRepository,
            productRepository,
            categoryRepository,
            supplierProductMappingRepository,
            costFormulaProfileRepository,
            stockBalanceRepository,
            stockMovementRepository,
            xmlParserService,
            formulaEngine,
            pricingService,
            auditService,
        )
    }

    @Test
    fun `should match supplier product with conversion factor and recalculate landed cost`() {
        val invoice = InboundInvoice(
            id = invoiceId,
            tenantId = tenantId,
            branchId = branchId,
            supplierId = supplierId,
            invoiceNumber = "12345",
            issuanceDate = LocalDate.now(),
        )
        val item = InboundInvoiceItem(
            id = itemId,
            tenantId = tenantId,
            inboundInvoice = invoice,
            itemIndex = 1,
            supplierItemCode = "COD-FORN-1",
            supplierItemDescription = "Caixa Parafuso 100un",
            invoicedQuantity = BigDecimal("2.0000"), // 2 caixas
            internalQuantity = BigDecimal("2.0000"),
            unitPrice = BigDecimal("50.0000"),
            totalPrice = BigDecimal("100.0000"),
            allocatedFreight = BigDecimal("10.0000"),
        )
        invoice.items.add(item)

        val product = Product(
            id = productId,
            tenantId = tenantId,
            sku = "PRD-PARAFUSO",
            name = "Parafuso Unitário",
            unitOfMeasure = "UN",
            ncm = "73181500",
            categoryId = UUID.randomUUID(),
        )

        every { invoiceRepository.findByIdAndTenantId(invoiceId, tenantId) } returns Optional.of(invoice)
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(product)
        every { supplierProductMappingRepository.findByTenantIdAndSupplierIdAndSupplierItemCode(tenantId, supplierId, "COD-FORN-1") } returns Optional.empty()
        every { supplierProductMappingRepository.save(any()) } answers { firstArg() }
        every { invoiceRepository.save(any()) } answers { firstArg() }
        every { customerRepository.findByIdAndTenantId(supplierId, tenantId) } returns Optional.empty()
        every { productRepository.findAllById(any()) } returns listOf(product)

        // 1 caixa = 100 unidades -> total 200 unidades
        val req = MatchProductRequest(
            productId = productId,
            conversionFactor = BigDecimal("100.0000"),
        )

        val updated = service.matchProduct(invoiceId, itemId, req)
        assertEquals(1, updated.items.size)
        val updatedItem = updated.items[0]
        assertEquals(productId, updatedItem.matchedProductId)
        assertEquals(BigDecimal("200.0000"), updatedItem.internalQuantity)
        assertEquals("UN", updatedItem.internalUom)
    }

    @Test
    fun `should finalize invoice stock, update product CMP and ledger movement`() {
        val invoice = InboundInvoice(
            id = invoiceId,
            tenantId = tenantId,
            branchId = branchId,
            supplierId = supplierId,
            invoiceNumber = "12345",
            issuanceDate = LocalDate.now(),
            status = InboundInvoiceStatus.DRAFT,
        )
        val item = InboundInvoiceItem(
            id = itemId,
            tenantId = tenantId,
            inboundInvoice = invoice,
            itemIndex = 1,
            supplierItemCode = "COD-1",
            supplierItemDescription = "Item 1",
            matchedProductId = productId,
            invoicedQuantity = BigDecimal("10.0000"),
            internalQuantity = BigDecimal("10.0000"),
            unitPrice = BigDecimal("100.0000"),
            totalPrice = BigDecimal("1000.0000"),
            finalLandedUnitCost = BigDecimal("105.0000"),
        )
        invoice.items.add(item)

        val product = Product(
            id = productId,
            tenantId = tenantId,
            sku = "PRD-01",
            name = "Item Catálogo",
            averageCost = BigDecimal("90.0000"),
            ncm = "12345678",
            categoryId = UUID.randomUUID(),
        )

        every { invoiceRepository.findByIdAndTenantId(invoiceId, tenantId) } returns Optional.of(invoice)
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(product)
        every { stockBalanceRepository.findAllByTenantIdAndProductId(tenantId, productId) } returns emptyList()
        every { pricingService.calculateWeightedAverageCost(any(), any(), any(), any()) } returns BigDecimal("105.0000")
        every { productRepository.save(any()) } answers { firstArg() }
        every { stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.empty()
        every { stockBalanceRepository.save(any()) } answers { firstArg() }
        every { stockMovementRepository.save(any()) } answers { firstArg() }
        every { invoiceRepository.save(any()) } answers { firstArg() }
        every { customerRepository.findByIdAndTenantId(supplierId, tenantId) } returns Optional.empty()
        every { productRepository.findAllById(any()) } returns listOf(product)

        val req = FinalizeInvoiceStockRequest(warehouseId = warehouseId)
        val res = service.finalizeStock(invoiceId, req)

        assertEquals(InboundInvoiceStatus.STOCKED, res.status)
        assertEquals(BigDecimal("105.0000"), product.averageCost)
        assertEquals(BigDecimal("105.0000"), product.lastAcquisitionCost)
    }

    @Test
    fun `should auto-register missing products when autoRegisterMissingProducts is called`() {
        val invoice = InboundInvoice(
            id = invoiceId,
            tenantId = tenantId,
            branchId = branchId,
            supplierId = supplierId,
            invoiceNumber = "999",
            issuanceDate = LocalDate.now(),
            status = InboundInvoiceStatus.DRAFT,
        )
        val item = InboundInvoiceItem(
            id = itemId,
            tenantId = tenantId,
            inboundInvoice = invoice,
            itemIndex = 1,
            supplierItemCode = "ITEM-UNKNOWN-99",
            supplierItemDescription = "Produto Novo Importado",
            matchedProductId = null, // unmapped
            invoicedQuantity = BigDecimal("5.0000"),
            internalQuantity = BigDecimal("5.0000"),
            supplierUom = "UN",
            unitPrice = BigDecimal("25.0000"),
            totalPrice = BigDecimal("125.0000"),
        )
        invoice.items.add(item)

        val defaultCat = ProductCategory(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            code = "GERAL",
            name = "Geral",
        )

        every { invoiceRepository.findByIdAndTenantId(invoiceId, tenantId) } returns Optional.of(invoice)
        every { productRepository.findByTenantIdAndSku(tenantId, "ITEM-UNKNOWN-99") } returns Optional.empty()
        every { productRepository.findByTenantIdAndNameIgnoreCase(tenantId, "Produto Novo Importado") } returns Optional.empty()
        every { categoryRepository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId) } returns listOf(defaultCat)
        every { productRepository.existsByTenantIdAndSku(tenantId, "ITEM-UNKNOWN-99") } returns false
        every { productRepository.save(any()) } answers {
            val p = firstArg<Product>()
            Product(
                id = UUID.randomUUID(),
                tenantId = p.tenantId,
                sku = p.sku,
                barcode = p.barcode,
                name = p.name,
                unitOfMeasure = p.unitOfMeasure,
                categoryId = p.categoryId,
                ncm = p.ncm,
                lastAcquisitionCost = p.lastAcquisitionCost,
                averageCost = p.averageCost,
                replacementCost = p.replacementCost,
            )
        }
        every { supplierProductMappingRepository.save(any()) } answers { firstArg() }
        every { invoiceRepository.save(any()) } answers { firstArg() }
        every { customerRepository.findByIdAndTenantId(supplierId, tenantId) } returns Optional.empty()
        every { productRepository.findAllById(any()) } answers {
            val ids = firstArg<Iterable<UUID>>()
            ids.map { id ->
                Product(
                    id = id,
                    tenantId = tenantId,
                    sku = "ITEM-UNKNOWN-99",
                    name = "Produto Novo Importado",
                    ncm = "00000000",
                    categoryId = defaultCat.id,
                )
            }
        }

        val res = service.autoRegisterMissingProducts(invoiceId)

        assertEquals(1, res.items.size)
        assertNotNull(res.items[0].matchedProductId)
        assertEquals("ITEM-UNKNOWN-99", res.items[0].matchedProductSku)
        assertEquals("Produto Novo Importado", res.items[0].matchedProductName)
    }
}
