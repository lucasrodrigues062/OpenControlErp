package org.opencontrol.erp.catalog.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.api.dto.CreateProductRequest
import org.opencontrol.erp.catalog.api.dto.UnitConversionRequest
import org.opencontrol.erp.catalog.api.dto.UpdateProductRequest
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.opencontrol.erp.catalog.domain.model.ProductTrackingMode
import org.opencontrol.erp.catalog.domain.model.SpedItemType
import org.opencontrol.erp.catalog.exception.DuplicateSkuException
import org.opencontrol.erp.catalog.exception.InvalidFiscalDataException
import org.opencontrol.erp.catalog.exception.ProductBarcodeNotFoundException
import org.opencontrol.erp.catalog.exception.ProductCategoryNotFoundException
import org.opencontrol.erp.catalog.exception.ProductNotFoundException
import org.opencontrol.erp.catalog.repository.ProductCategoryRepository
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.repository.ProductUnitConversionRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class ProductServiceTest {
    private val productRepository: ProductRepository = mockk()
    private val categoryRepository: ProductCategoryRepository = mockk()
    private val unitConversionRepository: ProductUnitConversionRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: ProductService
    private val tenantId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

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
        service =
            ProductService(
                productRepository,
                categoryRepository,
                unitConversionRepository,
                auditService,
            )
    }

    private fun createSampleProduct(
        id: UUID = UUID.randomUUID(),
        sku: String = "PROD-001",
        barcode: String? = "7891000315507",
    ) = Product(
        id = id,
        tenantId = tenantId,
        sku = sku,
        barcode = barcode,
        name = "Dell XPS Laptop",
        description = "High-end developer laptop",
        itemType = SpedItemType.REV_00,
        unitOfMeasure = "UN",
        categoryId = categoryId,
        brand = "Dell",
        model = "XPS 15",
        ncm = "84713012",
        cest = "0100100",
        origin = 0,
        trackingMode = ProductTrackingMode.SERIAL_NUMBER,
        lastAcquisitionCost = BigDecimal("7500.00"),
        averageCost = BigDecimal("7500.00"),
        replacementCost = BigDecimal("7500.00"),
        isActive = true,
    )

    @Test
    fun `should search products with pagination`() {
        val prod = createSampleProduct()
        val pageable = PageRequest.of(0, 20)
        every { productRepository.findAll(any<Specification<Product>>(), pageable) } returns PageImpl(listOf(prod), pageable, 1)
        every { categoryRepository.findById(categoryId) } returns
            Optional.of(ProductCategory(id = categoryId, tenantId = tenantId, code = "ELEC", name = "Electronics"))
        every { unitConversionRepository.findAllByTenantIdAndProductId(tenantId, prod.id) } returns emptyList()

        val response = service.searchProducts(query = "Dell", categoryId = null, ncm = null, trackingMode = null, pageable = pageable)
        assertEquals(1, response.totalElements)
        assertEquals("PROD-001", response.items[0].sku)
        assertEquals("Electronics", response.items[0].categoryName)
    }

    @Test
    fun `should get product by id`() {
        val prod = createSampleProduct()
        every { productRepository.findByIdAndTenantId(prod.id, tenantId) } returns Optional.of(prod)
        every { categoryRepository.findById(categoryId) } returns Optional.empty()
        every { unitConversionRepository.findAllByTenantIdAndProductId(tenantId, prod.id) } returns emptyList()

        val response = service.getById(prod.id)
        assertEquals(prod.id, response.id)
        assertEquals("PROD-001", response.sku)
    }

    @Test
    fun `should throw when product by id not found`() {
        val id = UUID.randomUUID()
        every { productRepository.findByIdAndTenantId(id, tenantId) } returns Optional.empty()

        assertThrows<ProductNotFoundException> {
            service.getById(id)
        }
    }

    @Test
    fun `should get product by barcode`() {
        val prod = createSampleProduct()
        every { productRepository.findByTenantIdAndBarcode(tenantId, "7891000315507") } returns Optional.of(prod)
        every { categoryRepository.findById(categoryId) } returns Optional.empty()
        every { unitConversionRepository.findAllByTenantIdAndProductId(tenantId, prod.id) } returns emptyList()

        val response = service.getByBarcode("7891000315507")
        assertEquals(prod.id, response.id)
    }

    @Test
    fun `should throw when barcode not found`() {
        every { productRepository.findByTenantIdAndBarcode(tenantId, "9999999999999") } returns Optional.empty()

        assertThrows<ProductBarcodeNotFoundException> {
            service.getByBarcode("9999999999999")
        }
    }

    @Test
    fun `should create product successfully`() {
        val req =
            CreateProductRequest(
                sku = "NEW-SKU",
                barcode = "7891000315507",
                name = "Test Product",
                unitOfMeasure = "UN",
                categoryId = categoryId,
                ncm = "8471.30.12",
                cest = "01.001.00",
                origin = 0,
                trackingMode = ProductTrackingMode.BATCH_LOT,
                lastAcquisitionCost = BigDecimal("150.00"),
            )
        every { categoryRepository.existsById(categoryId) } returns true
        every { productRepository.findByTenantIdAndSku(tenantId, "NEW-SKU") } returns Optional.empty()
        every { productRepository.findByTenantIdAndBarcode(tenantId, "7891000315507") } returns Optional.empty()
        every { productRepository.save(any()) } answers { firstArg() }
        every { categoryRepository.findById(categoryId) } returns
            Optional.of(ProductCategory(id = categoryId, tenantId = tenantId, code = "ELEC", name = "Electronics"))
        every { unitConversionRepository.findAllByTenantIdAndProductId(tenantId, any()) } returns emptyList()

        val created = service.createProduct(req)
        assertNotNull(created.id)
        assertEquals("NEW-SKU", created.sku)
        assertEquals("84713012", created.ncm) // dots sanitized
        assertEquals("0100100", created.cest) // dots sanitized
        verify {
            auditService.recordAudit(
                entityType = "Product",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should reject product creation when category does not exist`() {
        val req =
            CreateProductRequest(
                sku = "SKU-1",
                name = "Test",
                unitOfMeasure = "UN",
                categoryId = categoryId,
                ncm = "84713012",
            )
        every { categoryRepository.existsById(categoryId) } returns false

        assertThrows<ProductCategoryNotFoundException> {
            service.createProduct(req)
        }
    }

    @Test
    fun `should reject product creation with duplicate SKU`() {
        val req =
            CreateProductRequest(
                sku = "DUPLICATE",
                name = "Test",
                unitOfMeasure = "UN",
                categoryId = categoryId,
                ncm = "84713012",
            )
        val existing = createSampleProduct(sku = "DUPLICATE")
        every { categoryRepository.existsById(categoryId) } returns true
        every { productRepository.findByTenantIdAndSku(tenantId, "DUPLICATE") } returns Optional.of(existing)

        assertThrows<DuplicateSkuException> {
            service.createProduct(req)
        }
    }

    @Test
    fun `should reject product creation with invalid NCM format`() {
        val req =
            CreateProductRequest(
                sku = "SKU-INV-NCM",
                name = "Test",
                unitOfMeasure = "UN",
                categoryId = categoryId,
                ncm = "12345", // Only 5 digits
            )
        every { categoryRepository.existsById(categoryId) } returns true
        every { productRepository.findByTenantIdAndSku(tenantId, "SKU-INV-NCM") } returns Optional.empty()

        assertThrows<InvalidFiscalDataException> {
            service.createProduct(req)
        }
    }

    @Test
    fun `should reject product creation with invalid barcode check digit`() {
        val req =
            CreateProductRequest(
                sku = "SKU-BAD-BARCODE",
                barcode = "7891000315508", // Invalid check digit
                name = "Test",
                unitOfMeasure = "UN",
                categoryId = categoryId,
                ncm = "84713012",
            )
        every { categoryRepository.existsById(categoryId) } returns true
        every { productRepository.findByTenantIdAndSku(tenantId, "SKU-BAD-BARCODE") } returns Optional.empty()

        assertThrows<InvalidFiscalDataException> {
            service.createProduct(req)
        }
    }

    @Test
    fun `should update product successfully`() {
        val prod = createSampleProduct()
        every { productRepository.findByIdAndTenantId(prod.id, tenantId) } returns Optional.of(prod)
        every { categoryRepository.existsById(categoryId) } returns true
        every { productRepository.findByTenantIdAndSku(tenantId, prod.sku) } returns Optional.of(prod)
        every { productRepository.findByTenantIdAndBarcode(tenantId, prod.barcode!!) } returns Optional.of(prod)
        every { productRepository.save(any()) } answers { firstArg() }
        every { categoryRepository.findById(categoryId) } returns Optional.empty()
        every { unitConversionRepository.findAllByTenantIdAndProductId(tenantId, prod.id) } returns emptyList()

        val req =
            UpdateProductRequest(
                sku = prod.sku,
                barcode = prod.barcode,
                name = "Updated Product Name",
                unitOfMeasure = "UN",
                categoryId = categoryId,
                ncm = prod.ncm,
                isActive = true,
            )

        val updated = service.updateProduct(prod.id, req)
        assertEquals("Updated Product Name", updated.name)
        verify {
            auditService.recordAudit(
                entityType = "Product",
                entityId = any(),
                action = "UPDATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should deactivate product on delete`() {
        val prod = createSampleProduct()
        every { productRepository.findByIdAndTenantId(prod.id, tenantId) } returns Optional.of(prod)
        every { productRepository.save(any()) } answers { firstArg() }

        service.deleteProduct(prod.id)
        assertFalse(prod.isActive)
        verify {
            auditService.recordAudit(
                entityType = "Product",
                entityId = any(),
                action = "DEACTIVATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should add unit conversion factor`() {
        val prod = createSampleProduct()
        every { productRepository.findByIdAndTenantId(prod.id, tenantId) } returns Optional.of(prod)
        every { unitConversionRepository.findByTenantIdAndProductIdAndUnitFromAndUnitTo(tenantId, prod.id, "CX", "UN") } returns
            Optional.empty()
        every { unitConversionRepository.save(any()) } answers { firstArg() }

        val req = UnitConversionRequest(unitFrom = "CX", unitTo = "UN", conversionFactor = BigDecimal("12"))
        val conv = service.addUnitConversion(prod.id, req)

        assertEquals("CX", conv.unitFrom)
        assertEquals("UN", conv.unitTo)
        assertEquals(BigDecimal("12"), conv.conversionFactor)
        verify {
            auditService.recordAudit(
                entityType = "ProductUnitConversion",
                entityId = any(),
                action = "CREATE_OR_UPDATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should reject conversion between identical units`() {
        val prod = createSampleProduct()
        every { productRepository.findByIdAndTenantId(prod.id, tenantId) } returns Optional.of(prod)

        val req = UnitConversionRequest(unitFrom = "UN", unitTo = "UN", conversionFactor = BigDecimal("1"))
        assertThrows<IllegalArgumentException> {
            service.addUnitConversion(prod.id, req)
        }
    }
}
