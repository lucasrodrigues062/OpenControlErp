package org.opencontrol.erp.inventory.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.SpedItemType
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.Warehouse
import org.opencontrol.erp.inventory.domain.model.WarehouseType
import org.opencontrol.erp.inventory.exception.InsufficientStockException
import org.opencontrol.erp.inventory.repository.StockBalanceRepository
import org.opencontrol.erp.inventory.repository.StorageLocationRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class StockBalanceServiceTest {
    private val balanceRepository: StockBalanceRepository = mockk()
    private val warehouseRepository: WarehouseRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val locationRepository: StorageLocationRepository = mockk()

    private lateinit var service: StockBalanceService
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
        service = StockBalanceService(balanceRepository, warehouseRepository, productRepository, locationRepository)
    }

    private fun sampleBalance(
        onHand: BigDecimal = BigDecimal("50.0000"),
        reserved: BigDecimal = BigDecimal("10.0000"),
    ) = StockBalance(
        id = UUID.randomUUID(),
        tenantId = tenantId,
        warehouseId = warehouseId,
        productId = productId,
        locationId = null,
        quantityOnHand = onHand,
        quantityReserved = reserved,
        quantityAvailable = onHand.subtract(reserved),
    )

    private fun sampleWarehouse() =
        Warehouse(
            id = warehouseId,
            tenantId = tenantId,
            branchId = UUID.randomUUID(),
            code = "WH-01",
            name = "Depósito Principal",
            warehouseType = WarehouseType.COMMERCIAL,
        )

    private fun sampleProduct() =
        Product(
            id = productId,
            tenantId = tenantId,
            sku = "PROD-001",
            name = "Test Product",
            itemType = SpedItemType.REV_00,
            unitOfMeasure = "UN",
            categoryId = UUID.randomUUID(),
            ncm = "84713012",
        )

    @Test
    fun `should increase stock on positive adjustment`() {
        val balance = sampleBalance(BigDecimal("50.0000"), BigDecimal("10.0000"))
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.of(balance)
        every { balanceRepository.save(any()) } answers { firstArg() }

        val updated = service.adjustStock(warehouseId, productId, null, BigDecimal("20.0000"))
        assertEquals(BigDecimal("70.0000"), updated.quantityOnHand)
        assertEquals(BigDecimal("60.0000"), updated.quantityAvailable)
    }

    @Test
    fun `should decrease stock on negative adjustment`() {
        val balance = sampleBalance(BigDecimal("50.0000"), BigDecimal("10.0000"))
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.of(balance)
        every { balanceRepository.save(any()) } answers { firstArg() }

        val updated = service.adjustStock(warehouseId, productId, null, BigDecimal("-20.0000"))
        assertEquals(BigDecimal("30.0000"), updated.quantityOnHand)
        assertEquals(BigDecimal("20.0000"), updated.quantityAvailable)
    }

    @Test
    fun `should throw when stock adjustment results in negative quantity`() {
        val balance = sampleBalance(BigDecimal("50.0000"), BigDecimal("0.0000"))
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.of(balance)

        assertThrows<InsufficientStockException> {
            service.adjustStock(warehouseId, productId, null, BigDecimal("-60.0000"))
        }
    }

    @Test
    fun `should reserve stock when available`() {
        val balance = sampleBalance(BigDecimal("50.0000"), BigDecimal("10.0000")) // available = 40
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.of(balance)
        every { balanceRepository.save(any()) } answers { firstArg() }

        val updated = service.reserveStock(warehouseId, productId, null, BigDecimal("25.0000"))
        assertEquals(BigDecimal("35.0000"), updated.quantityReserved)
        assertEquals(BigDecimal("15.0000"), updated.quantityAvailable)
    }

    @Test
    fun `should throw when reserving more than available stock`() {
        val balance = sampleBalance(BigDecimal("50.0000"), BigDecimal("40.0000")) // available = 10
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.of(balance)

        assertThrows<InsufficientStockException> {
            service.reserveStock(warehouseId, productId, null, BigDecimal("20.0000"))
        }
    }

    @Test
    fun `should release reserved stock`() {
        val balance = sampleBalance(BigDecimal("50.0000"), BigDecimal("20.0000")) // available = 30
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.of(balance)
        every { balanceRepository.save(any()) } answers { firstArg() }

        val updated = service.releaseReservedStock(warehouseId, productId, null, BigDecimal("15.0000"))
        assertEquals(BigDecimal("5.0000"), updated.quantityReserved)
        assertEquals(BigDecimal("45.0000"), updated.quantityAvailable)
    }

    @Test
    fun `should create new balance when none exists`() {
        every { balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId) } returns Optional.empty()
        every { balanceRepository.save(any()) } answers { firstArg() }

        val created = service.getOrCreateBalance(warehouseId, productId, null)
        assertNotNull(created)
        assertEquals(BigDecimal.ZERO, created.quantityOnHand)
    }

    @Test
    fun `should list balances with all filter variations`() {
        val balance = sampleBalance()
        every { warehouseRepository.findById(warehouseId) } returns Optional.of(sampleWarehouse())
        every { productRepository.findById(productId) } returns Optional.of(sampleProduct())
        every { locationRepository.findById(any()) } returns Optional.empty()

        // 1. Both warehouseId and productId
        every { balanceRepository.findAllByTenantIdAndWarehouseId(tenantId, warehouseId) } returns listOf(balance)
        val res1 = service.listBalances(warehouseId, productId)
        assertEquals(1, res1.size)

        // 2. Only warehouseId
        val res2 = service.listBalances(warehouseId, null)
        assertEquals(1, res2.size)

        // 3. Only productId
        every { balanceRepository.findAllByTenantIdAndProductId(tenantId, productId) } returns listOf(balance)
        val res3 = service.listBalances(null, productId)
        assertEquals(1, res3.size)

        // 4. All
        every { balanceRepository.findAllByTenantId(tenantId) } returns listOf(balance)
        val res4 = service.listBalances(null, null)
        assertEquals(1, res4.size)
    }

    @Test
    fun `should get product balances across warehouses`() {
        val balance = sampleBalance()
        every { warehouseRepository.findById(warehouseId) } returns Optional.of(sampleWarehouse())
        every { productRepository.findById(productId) } returns Optional.of(sampleProduct())
        every { locationRepository.findById(any()) } returns Optional.empty()
        every { balanceRepository.findAllByTenantIdAndProductId(tenantId, productId) } returns listOf(balance)

        val res = service.getProductBalances(productId)
        assertEquals(1, res.size)
        assertEquals("PROD-001", res[0].productSku)
    }
}
