package org.opencontrol.erp.inbound.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.service.PricingService
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.inbound.api.dto.CreateTransferItemRequest
import org.opencontrol.erp.inbound.api.dto.CreateTransferRequest
import org.opencontrol.erp.inbound.domain.model.StockTransfer
import org.opencontrol.erp.inbound.domain.model.StockTransferItem
import org.opencontrol.erp.inbound.domain.model.StockTransferStatus
import org.opencontrol.erp.inbound.domain.model.StockTransferType
import org.opencontrol.erp.inbound.repository.StockTransferRepository
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.repository.ProductLotRepository
import org.opencontrol.erp.inventory.repository.ProductSerialNumberRepository
import org.opencontrol.erp.inventory.repository.StockBalanceRepository
import org.opencontrol.erp.inventory.repository.StockMovementRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class StockTransferServiceTest {
    private val transferRepository: StockTransferRepository = mockk()
    private val productRepository: ProductRepository = mockk()
    private val warehouseRepository: WarehouseRepository = mockk()
    private val branchRepository: CompanyBranchRepository = mockk()
    private val stockBalanceRepository: StockBalanceRepository = mockk()
    private val stockMovementRepository: StockMovementRepository = mockk(relaxed = true)
    private val productLotRepository: ProductLotRepository = mockk(relaxed = true)
    private val productSerialNumberRepository: ProductSerialNumberRepository = mockk(relaxed = true)
    private val pricingService: PricingService = mockk()
    private val auditService: AuditService = mockk(relaxed = true)
    private val objectMapper = jacksonObjectMapper()

    private lateinit var service: StockTransferService

    private val tenantId = UUID.randomUUID()
    private val branchId1 = UUID.randomUUID()
    private val branchId2 = UUID.randomUUID()
    private val warehouseId1 = UUID.randomUUID()
    private val warehouseId2 = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "logistics_user",
                roles = setOf("INBOUND_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = StockTransferService(
            transferRepository = transferRepository,
            productRepository = productRepository,
            warehouseRepository = warehouseRepository,
            branchRepository = branchRepository,
            stockBalanceRepository = stockBalanceRepository,
            stockMovementRepository = stockMovementRepository,
            productLotRepository = productLotRepository,
            productSerialNumberRepository = productSerialNumberRepository,
            pricingService = pricingService,
            auditService = auditService,
            objectMapper = objectMapper,
        )

        every { branchRepository.findById(any()) } returns Optional.empty()
        every { warehouseRepository.findByIdAndTenantId(any(), any()) } returns Optional.empty()
        every { stockMovementRepository.save(any()) } answers { firstArg() }
        every { productLotRepository.save(any()) } answers { firstArg() }
        every { productSerialNumberRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `should execute internal warehouse transfer immediately and update both balances`() {
        val product = Product(
            id = productId,
            tenantId = tenantId,
            name = "Cabo de Rede Cat6",
            sku = "CAB-CAT6",
            categoryId = UUID.randomUUID(),
            ncm = "85444200",
            averageCost = BigDecimal("5.00"),
            lastAcquisitionCost = BigDecimal("5.00"),
        )
        val originBalance = StockBalance(
            tenantId = tenantId,
            warehouseId = warehouseId1,
            productId = productId,
            quantityOnHand = BigDecimal("100.0000"),
            quantityReserved = BigDecimal("10.0000"),
            quantityAvailable = BigDecimal("90.0000"),
        )
        val destBalance = StockBalance(
            tenantId = tenantId,
            warehouseId = warehouseId2,
            productId = productId,
            quantityOnHand = BigDecimal("20.0000"),
            quantityReserved = BigDecimal.ZERO,
            quantityAvailable = BigDecimal("20.0000"),
        )

        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(product)
        every { productRepository.findAllById(any()) } returns listOf(product)
        every { transferRepository.existsByTenantIdAndTransferNumber(tenantId, any()) } returns false
        every { stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId1, productId) } returns Optional.of(originBalance)
        every { stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId2, productId) } returns Optional.of(destBalance)
        every { stockBalanceRepository.save(any()) } answers { firstArg() }
        every { transferRepository.save(any()) } answers { firstArg() }

        val request = CreateTransferRequest(
            transferNumber = "TRF-INT-001",
            transferType = StockTransferType.INTERNAL_WAREHOUSE,
            originBranchId = branchId1,
            originWarehouseId = warehouseId1,
            destinationBranchId = branchId1,
            destinationWarehouseId = warehouseId2,
            items = listOf(
                CreateTransferItemRequest(
                    productId = productId,
                    quantity = BigDecimal("30.0000"),
                )
            ),
        )

        val result = service.createTransfer(request)

        assertNotNull(result)
        assertEquals(StockTransferStatus.RECEIVED_COMPLETED, result.status)
        assertEquals(BigDecimal("70.0000"), originBalance.quantityOnHand)
        assertEquals(BigDecimal("50.0000"), destBalance.quantityOnHand)
        verify(exactly = 2) { stockMovementRepository.save(any()) }
    }

    @Test
    fun `should reject transfer if insufficient available balance`() {
        val product = Product(
            id = productId,
            tenantId = tenantId,
            name = "Switch 24p",
            sku = "SW-24P",
            categoryId = UUID.randomUUID(),
            ncm = "85176239",
            averageCost = BigDecimal("500.00"),
        )
        val originBalance = StockBalance(
            tenantId = tenantId,
            warehouseId = warehouseId1,
            productId = productId,
            quantityOnHand = BigDecimal("5.0000"),
            quantityReserved = BigDecimal("4.0000"),
            quantityAvailable = BigDecimal("1.0000"), // Only 1 available
        )

        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(product)
        every { transferRepository.existsByTenantIdAndTransferNumber(tenantId, any()) } returns false
        every { stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId1, productId) } returns Optional.of(originBalance)

        val request = CreateTransferRequest(
            transferType = StockTransferType.INTERNAL_WAREHOUSE,
            originBranchId = branchId1,
            originWarehouseId = warehouseId1,
            destinationBranchId = branchId1,
            destinationWarehouseId = warehouseId2,
            items = listOf(
                CreateTransferItemRequest(
                    productId = productId,
                    quantity = BigDecimal("5.0000"), // requesting 5
                )
            ),
        )

        assertThrows(IllegalStateException::class.java) {
            service.createTransfer(request)
        }
    }

    @Test
    fun `should dispatch inter-branch transfer into IN_TRANSIT and allow receive`() {
        val product = Product(
            id = productId,
            tenantId = tenantId,
            name = "Roteador Wi-Fi 6",
            sku = "RT-WIFI6",
            categoryId = UUID.randomUUID(),
            ncm = "85176241",
            averageCost = BigDecimal("200.00"),
            lastAcquisitionCost = BigDecimal("200.00"),
        )
        val originBalance = StockBalance(
            tenantId = tenantId,
            warehouseId = warehouseId1,
            productId = productId,
            quantityOnHand = BigDecimal("50.0000"),
            quantityReserved = BigDecimal.ZERO,
            quantityAvailable = BigDecimal("50.0000"),
        )
        val destBalance = StockBalance(
            tenantId = tenantId,
            warehouseId = warehouseId2,
            productId = productId,
            quantityOnHand = BigDecimal("10.0000"),
            quantityReserved = BigDecimal.ZERO,
            quantityAvailable = BigDecimal("10.0000"),
        )

        every { productRepository.findByIdAndTenantId(productId, tenantId) } returns Optional.of(product)
        every { productRepository.findAllById(any()) } returns listOf(product)
        every { productRepository.save(any()) } answers { firstArg() }
        every { transferRepository.existsByTenantIdAndTransferNumber(tenantId, any()) } returns false
        every { stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId1, productId) } returns Optional.of(originBalance)
        every { stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId2, productId) } returns Optional.of(destBalance)
        every { stockBalanceRepository.save(any()) } answers { firstArg() }
        every { transferRepository.save(any()) } answers { firstArg() }
        every { pricingService.calculateWeightedAverageCost(any(), any(), any(), any()) } returns BigDecimal("200.00")

        val request = CreateTransferRequest(
            transferType = StockTransferType.INTER_BRANCH,
            originBranchId = branchId1,
            originWarehouseId = warehouseId1,
            destinationBranchId = branchId2,
            destinationWarehouseId = warehouseId2,
            items = listOf(
                CreateTransferItemRequest(
                    productId = productId,
                    quantity = BigDecimal("15.0000"),
                )
            ),
        )

        val dispatched = service.createTransfer(request)

        assertEquals(StockTransferStatus.DISPATCHED_IN_TRANSIT, dispatched.status)
        assertEquals(BigDecimal("35.0000"), originBalance.quantityOnHand)
        assertEquals(BigDecimal("10.0000"), destBalance.quantityOnHand) // Not credited yet!

        // Now receive at destination
        val transferEntity = StockTransfer(
            id = dispatched.id,
            tenantId = tenantId,
            transferNumber = dispatched.transferNumber,
            transferType = StockTransferType.INTER_BRANCH,
            originBranchId = branchId1,
            originWarehouseId = warehouseId1,
            destinationBranchId = branchId2,
            destinationWarehouseId = warehouseId2,
            status = StockTransferStatus.DISPATCHED_IN_TRANSIT,
        )
        transferEntity.items.add(
            StockTransferItem(
                tenantId = tenantId,
                stockTransfer = transferEntity,
                productId = productId,
                requestedQuantity = BigDecimal("15.0000"),
                shippedQuantity = BigDecimal("15.0000"),
                receivedQuantity = BigDecimal.ZERO,
                unitCost = BigDecimal("200.00"),
            )
        )

        every { transferRepository.findByIdAndTenantId(dispatched.id, tenantId) } returns Optional.of(transferEntity)

        val received = service.receiveTransfer(dispatched.id)

        assertEquals(StockTransferStatus.RECEIVED_COMPLETED, received.status)
        assertEquals(BigDecimal("25.0000"), destBalance.quantityOnHand) // Now credited!
    }
}
