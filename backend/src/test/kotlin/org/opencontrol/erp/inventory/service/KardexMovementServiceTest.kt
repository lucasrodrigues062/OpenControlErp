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
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.SpedItemType
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.service.PricingService
import org.opencontrol.erp.inventory.api.dto.CreateStockMovementRequest
import org.opencontrol.erp.inventory.api.dto.StockBalanceResponse
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.StockMovement
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.inventory.repository.StockMovementRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class KardexMovementServiceTest {
    private val movementRepository: StockMovementRepository = mockk()
    private val balanceService: StockBalanceService = mockk()
    private val productRepository: ProductRepository = mockk()
    private val warehouseRepository: WarehouseRepository = mockk()
    private val lotRepository: ProductLotRepository = mockk()
    private val serialRepository: ProductSerialNumberRepository = mockk()
    private val pricingService: PricingService = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: KardexMovementService
    private val tenantId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()
    private val productId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()

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
        service =
            KardexMovementService(
                movementRepository,
                balanceService,
                productRepository,
                warehouseRepository,
                lotRepository,
                serialRepository,
                pricingService,
                auditService,
            )
    }

    private fun sampleProduct() =
        Product(
            id = productId,
            tenantId = tenantId,
            sku = "PROD-CMP",
            name = "Product CMP Test",
            itemType = SpedItemType.REV_00,
            unitOfMeasure = "UN",
            categoryId = UUID.randomUUID(),
            ncm = "84713012",
            averageCost = BigDecimal("10.0000"),
            lastAcquisitionCost = BigDecimal("10.0000"),
        )

    @Test
    fun `should record inbound purchase and recalculate CMP`() {
        val prod = sampleProduct()
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(prod)
        every { balanceService.getProductBalances(productId) } returns
            listOf(
                StockBalanceResponse(
                    id = UUID.randomUUID(),
                    warehouseId = warehouseId,
                    warehouseName = "Depósito",
                    productId = productId,
                    productSku = "PROD-CMP",
                    productName = "Product CMP Test",
                    locationId = null,
                    locationAddress = null,
                    quantityOnHand = BigDecimal("100"),
                    quantityReserved = BigDecimal.ZERO,
                    quantityAvailable = BigDecimal("100"),
                    updatedAt = java.time.Instant.now(),
                ),
            )
        // CMP calculation
        every {
            pricingService.calculateWeightedAverageCost(
                currentQty = BigDecimal("100"),
                currentCmp = BigDecimal("10.0000"),
                inboundQty = BigDecimal("50"),
                inboundCost = BigDecimal("16.0000"),
            )
        } returns BigDecimal("12.0000")

        every { productRepository.save(any()) } answers { firstArg() }

        val updatedBalance =
            StockBalance(
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                quantityOnHand = BigDecimal("150.0000"),
            )
        every { balanceService.adjustStock(warehouseId, productId, null, BigDecimal("50")) } returns updatedBalance
        every { movementRepository.save(any()) } answers { firstArg() }
        every { warehouseRepository.findById(warehouseId) } returns Optional.empty()

        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.INBOUND_PURCHASE,
                productId = productId,
                destinationWarehouseId = warehouseId,
                quantity = BigDecimal("50"),
                unitCost = BigDecimal("16.0000"),
                referenceDocument = "NF-e 12345",
            )

        val res = service.recordMovement(req)
        assertNotNull(res.id)
        assertEquals(BigDecimal("150.0000"), res.balanceAfter)
        assertEquals(BigDecimal("12.0000"), prod.averageCost)
        verify {
            auditService.recordAudit(
                entityType = "StockMovement",
                entityId = any(),
                action = "KARDEX_RECORD",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should record outbound sale and deduct stock`() {
        val prod = sampleProduct()
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(prod)

        val updatedBalance =
            StockBalance(
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                quantityOnHand = BigDecimal("80.0000"),
            )
        every { balanceService.adjustStock(warehouseId, productId, null, BigDecimal("-20")) } returns updatedBalance
        every { movementRepository.save(any()) } answers { firstArg() }
        every { warehouseRepository.findById(warehouseId) } returns Optional.empty()

        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.OUTBOUND_SALE,
                productId = productId,
                sourceWarehouseId = warehouseId,
                quantity = BigDecimal("20"),
                referenceDocument = "PED-987",
            )

        val res = service.recordMovement(req)
        assertEquals(BigDecimal("80.0000"), res.balanceAfter)
    }

    @Test
    fun `should block outbound sale from expired lot`() {
        val prod = sampleProduct()
        val lotId = UUID.randomUUID()
        val expiredLot =
            ProductLot(
                id = lotId,
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                lotNumber = "LOT-OLD",
                manufacturingDate = LocalDate.now().minusYears(1),
                expirationDate = LocalDate.now().minusDays(1),
                quantityOnHand = BigDecimal("50"),
                quantityReserved = BigDecimal.ZERO,
                status = LotStatus.EXPIRED,
            )

        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(prod)
        every { lotRepository.findByIdAndTenantId(lotId, tenantId) } returns Optional.of(expiredLot)

        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.OUTBOUND_SALE,
                productId = productId,
                sourceWarehouseId = warehouseId,
                lotId = lotId,
                quantity = BigDecimal("5"),
            )

        assertThrows<IllegalArgumentException> {
            service.recordMovement(req)
        }
    }

    @Test
    fun `should require reason code on manual inventory adjustment`() {
        val prod = sampleProduct()
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(prod)

        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.ADJUSTMENT_INVENTORY,
                productId = productId,
                destinationWarehouseId = warehouseId,
                quantity = BigDecimal("5"),
                reasonCode = null, // Missing!
            )

        assertThrows<IllegalArgumentException> {
            service.recordMovement(req)
        }
    }

    @Test
    fun `should execute transfer out from source warehouse`() {
        val prod = sampleProduct()
        val destWarehouseId = UUID.randomUUID()
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(prod)

        val sourceBalance =
            StockBalance(
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                quantityOnHand = BigDecimal("40.0000"),
            )

        every { balanceService.adjustStock(any(), any(), any(), any()) } returns sourceBalance
        every { movementRepository.save(any()) } answers { firstArg() }
        every { warehouseRepository.findById(any()) } returns Optional.empty()

        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.TRANSFER_OUT,
                productId = productId,
                sourceWarehouseId = warehouseId,
                destinationWarehouseId = destWarehouseId,
                quantity = BigDecimal("10"),
                notes = "Transferencia entre filiais",
            )

        val res = service.recordMovement(req)
        assertNotNull(res.id)
        assertEquals(StockMovementType.TRANSFER_OUT, res.movementType)
    }

    @Test
    fun `should execute manual adjustment with reason code`() {
        val prod = sampleProduct()
        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(prod)

        val updatedBalance =
            StockBalance(
                tenantId = tenantId,
                warehouseId = warehouseId,
                productId = productId,
                quantityOnHand = BigDecimal("55.0000"),
            )
        every { balanceService.adjustStock(warehouseId, productId, null, BigDecimal("5")) } returns updatedBalance
        every { movementRepository.save(any()) } answers { firstArg() }
        every { warehouseRepository.findById(warehouseId) } returns Optional.empty()

        val req =
            CreateStockMovementRequest(
                branchId = branchId,
                movementType = StockMovementType.ADJUSTMENT_INVENTORY,
                productId = productId,
                destinationWarehouseId = warehouseId,
                quantity = BigDecimal("5"),
                reasonCode = "COUNT_VARIANCE",
                notes = "Contagem fisica anual",
            )

        val res = service.recordMovement(req)
        assertNotNull(res.id)
        assertEquals("COUNT_VARIANCE", res.reasonCode)
    }

    @Test
    fun `should list movements with pagination`() {
        val pageable = PageRequest.of(0, 10)
        val movement =
            StockMovement(
                tenantId = tenantId,
                branchId = branchId,
                productId = productId,
                destinationWarehouseId = warehouseId,
                movementType = StockMovementType.INBOUND_PURCHASE,
                quantity = BigDecimal("10"),
                unitCost = BigDecimal("10.00"),
                balanceAfter = BigDecimal("10"),
            )
        val page = PageImpl(listOf(movement), pageable, 1)

        every { movementRepository.findAll(any<Specification<StockMovement>>(), pageable) } returns page
        every { productRepository.findById(any()) } returns Optional.of(sampleProduct())
        every { warehouseRepository.findById(any()) } returns Optional.empty()
        every { lotRepository.findById(any()) } returns Optional.empty()

        val res = service.listMovements(productId, warehouseId, StockMovementType.INBOUND_PURCHASE, pageable)
        assertNotNull(res)
        assertEquals(1, res.totalElements)
    }
}
