package org.opencontrol.erp.catalog.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.catalog.api.dto.CreateProductCategoryRequest
import org.opencontrol.erp.catalog.api.dto.CreateProductRequest
import org.opencontrol.erp.catalog.api.dto.ProductCategoryResponse
import org.opencontrol.erp.catalog.api.dto.ProductPageResponse
import org.opencontrol.erp.catalog.api.dto.ProductResponse
import org.opencontrol.erp.catalog.api.dto.UnitConversionRequest
import org.opencontrol.erp.catalog.api.dto.UnitConversionResponse
import org.opencontrol.erp.catalog.api.dto.UpdateProductCategoryRequest
import org.opencontrol.erp.catalog.api.dto.UpdateProductRequest
import org.opencontrol.erp.catalog.domain.model.ProductTrackingMode
import org.opencontrol.erp.catalog.domain.model.SpedItemType
import org.opencontrol.erp.catalog.service.ProductCategoryService
import org.opencontrol.erp.catalog.service.ProductService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ProductControllerTest {
    private val productService: ProductService = mockk()
    private val categoryService: ProductCategoryService = mockk()

    private lateinit var productController: ProductController
    private lateinit var categoryController: ProductCategoryController

    private val sampleCategory =
        ProductCategoryResponse(
            id = UUID.randomUUID(),
            parentId = null,
            code = "ELEC",
            name = "Electronics",
            colorHex = "#10B981",
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private val sampleProduct =
        ProductResponse(
            id = UUID.randomUUID(),
            sku = "PROD-001",
            barcode = "7891000315507",
            name = "Dell XPS Laptop",
            description = "Developer laptop",
            itemType = SpedItemType.REV_00,
            unitOfMeasure = "UN",
            categoryId = sampleCategory.id,
            categoryName = "Electronics",
            brand = "Dell",
            model = "XPS 15",
            ncm = "84713012",
            ncmException = null,
            cest = "0100100",
            origin = 0,
            trackingMode = ProductTrackingMode.SERIAL_NUMBER,
            grossWeight = BigDecimal("2.5000"),
            netWeight = BigDecimal("1.8000"),
            width = BigDecimal("35.00"),
            height = BigDecimal("2.00"),
            depth = BigDecimal("24.00"),
            volume = BigDecimal("0.0017"),
            minStock = BigDecimal("5.0000"),
            maxStock = BigDecimal("50.0000"),
            reorderPoint = BigDecimal("10.0000"),
            lastAcquisitionCost = BigDecimal("7500.00"),
            averageCost = BigDecimal("7500.00"),
            replacementCost = BigDecimal("7500.00"),
            isActive = true,
            unitConversions = emptyList(),
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        productController = ProductController(productService)
        categoryController = ProductCategoryController(categoryService)
    }

    @Test
    fun `should search products via controller`() {
        val pageResponse =
            ProductPageResponse(
                items = listOf(sampleProduct),
                totalElements = 1,
                totalPages = 1,
                page = 0,
                size = 20,
            )
        every { productService.searchProducts(any(), any(), any(), any(), any()) } returns pageResponse

        val res = productController.search(null, null, null, null, 0, 20)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertEquals(1, res.body!!.totalElements)
        assertEquals("PROD-001", res.body!!.items[0].sku)
    }

    @Test
    fun `should get product by id via controller`() {
        every { productService.getById(sampleProduct.id) } returns sampleProduct

        val res = productController.getById(sampleProduct.id)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals("PROD-001", res.body!!.sku)
    }

    @Test
    fun `should get product by barcode via controller`() {
        every { productService.getByBarcode("7891000315507") } returns sampleProduct

        val res = productController.getByBarcode("7891000315507")
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals("7891000315507", res.body!!.barcode)
    }

    @Test
    fun `should create product via controller`() {
        val req =
            CreateProductRequest(
                sku = "PROD-001",
                barcode = "7891000315507",
                name = "Dell XPS Laptop",
                unitOfMeasure = "UN",
                categoryId = sampleCategory.id,
                ncm = "84713012",
            )
        every { productService.createProduct(req) } returns sampleProduct

        val res = productController.create(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("PROD-001", res.body!!.sku)
    }

    @Test
    fun `should update product via controller`() {
        val req =
            UpdateProductRequest(
                sku = "PROD-001",
                name = "Dell XPS Laptop 2026",
                unitOfMeasure = "UN",
                categoryId = sampleCategory.id,
                ncm = "84713012",
            )
        every { productService.updateProduct(sampleProduct.id, req) } returns sampleProduct

        val res = productController.update(sampleProduct.id, req)
        assertEquals(HttpStatus.OK, res.statusCode)
    }

    @Test
    fun `should delete product via controller`() {
        every { productService.deleteProduct(sampleProduct.id) } returns Unit

        productController.delete(sampleProduct.id)
    }

    @Test
    fun `should add unit conversion via controller`() {
        val req = UnitConversionRequest(unitFrom = "CX", unitTo = "UN", conversionFactor = BigDecimal("12"))
        val convRes =
            UnitConversionResponse(
                id = UUID.randomUUID(),
                productId = sampleProduct.id,
                unitFrom = "CX",
                unitTo = "UN",
                conversionFactor = BigDecimal("12"),
            )
        every { productService.addUnitConversion(sampleProduct.id, req) } returns convRes

        val res = productController.addUnitConversion(sampleProduct.id, req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("CX", res.body!!.unitFrom)
    }

    @Test
    fun `should list categories via category controller`() {
        every { categoryService.listCategories(any(), any()) } returns listOf(sampleCategory)

        val res = categoryController.list(null, false)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
        assertEquals("ELEC", res.body!![0].code)
    }

    @Test
    fun `should create category via category controller`() {
        val req = CreateProductCategoryRequest(code = "ELEC", name = "Electronics")
        every { categoryService.createCategory(req) } returns sampleCategory

        val res = categoryController.create(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("ELEC", res.body!!.code)
    }

    @Test
    fun `should update category via category controller`() {
        val req = UpdateProductCategoryRequest(name = "Consumer Electronics")
        every { categoryService.updateCategory(sampleCategory.id, req) } returns sampleCategory

        val res = categoryController.update(sampleCategory.id, req)
        assertEquals(HttpStatus.OK, res.statusCode)
    }

    @Test
    fun `should delete category via category controller`() {
        every { categoryService.deleteCategory(sampleCategory.id) } returns Unit

        categoryController.delete(sampleCategory.id)
    }
}
