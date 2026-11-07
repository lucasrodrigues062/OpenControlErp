package org.opencontrol.erp.inventory.service

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
import org.opencontrol.erp.inventory.api.dto.CreateStorageLocationRequest
import org.opencontrol.erp.inventory.api.dto.CreateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.UpdateWarehouseRequest
import org.opencontrol.erp.inventory.domain.model.StorageLocation
import org.opencontrol.erp.inventory.domain.model.Warehouse
import org.opencontrol.erp.inventory.domain.model.WarehouseType
import org.opencontrol.erp.inventory.exception.DuplicateStorageLocationException
import org.opencontrol.erp.inventory.exception.DuplicateWarehouseCodeException
import org.opencontrol.erp.inventory.exception.WarehouseNotFoundException
import org.opencontrol.erp.inventory.repository.StorageLocationRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID

class WarehouseServiceTest {
    private val warehouseRepository: WarehouseRepository = mockk()
    private val locationRepository: StorageLocationRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: WarehouseService
    private val tenantId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("INVENTORY_ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = WarehouseService(warehouseRepository, locationRepository, auditService)
    }

    private fun sampleWarehouse(
        id: UUID = UUID.randomUUID(),
        code: String = "CENTRAL",
        isDefault: Boolean = true,
    ) = Warehouse(
        id = id,
        tenantId = tenantId,
        branchId = branchId,
        code = code,
        name = "Centro de Distribuição Principal",
        warehouseType = WarehouseType.COMMERCIAL,
        isDefault = isDefault,
        isActive = true,
    )

    @Test
    fun `should list warehouses for branch`() {
        val w = sampleWarehouse()
        every { warehouseRepository.findAllByTenantIdAndBranchIdOrderByCodeAsc(tenantId, branchId) } returns listOf(w)

        val result = service.listWarehouses(branchId)
        assertEquals(1, result.size)
        assertEquals("CENTRAL", result[0].code)
    }

    @Test
    fun `should get warehouse by id`() {
        val w = sampleWarehouse()
        every { warehouseRepository.findByIdAndTenantId(w.id, tenantId) } returns Optional.of(w)

        val result = service.getById(w.id)
        assertEquals(w.id, result.id)
        assertEquals("CENTRAL", result.code)
    }

    @Test
    fun `should throw when warehouse not found`() {
        val id = UUID.randomUUID()
        every { warehouseRepository.findByIdAndTenantId(id, tenantId) } returns Optional.empty()

        assertThrows<WarehouseNotFoundException> {
            service.getById(id)
        }
    }

    @Test
    fun `should create warehouse and reset previous default`() {
        val existingDefault = sampleWarehouse(code = "OLD_DEF", isDefault = true)
        every { warehouseRepository.existsByTenantIdAndCode(tenantId, "NEW_DEF") } returns false
        every { warehouseRepository.findByTenantIdAndBranchIdAndIsDefaultTrue(tenantId, branchId) } returns Optional.of(existingDefault)
        every { warehouseRepository.save(any()) } answers { firstArg() }

        val req =
            CreateWarehouseRequest(
                branchId = branchId,
                code = "NEW_DEF",
                name = "Novo Depósito Padrão",
                isDefault = true,
            )

        val created = service.createWarehouse(req)
        assertNotNull(created.id)
        assertEquals("NEW_DEF", created.code)
        assertFalse(existingDefault.isDefault)
        verify {
            auditService.recordAudit(
                entityType = "Warehouse",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should reject duplicate warehouse code`() {
        every { warehouseRepository.existsByTenantIdAndCode(tenantId, "DUPLICATE") } returns true

        val req =
            CreateWarehouseRequest(
                branchId = branchId,
                code = "DUPLICATE",
                name = "Duplicate",
            )

        assertThrows<DuplicateWarehouseCodeException> {
            service.createWarehouse(req)
        }
    }

    @Test
    fun `should update warehouse`() {
        val w = sampleWarehouse(isDefault = false)
        every { warehouseRepository.findByIdAndTenantId(w.id, tenantId) } returns Optional.of(w)
        every { warehouseRepository.save(any()) } answers { firstArg() }

        val req =
            UpdateWarehouseRequest(
                branchId = branchId,
                name = "Nome Atualizado",
                warehouseType = WarehouseType.STORAGE,
                isDefault = false,
                isActive = true,
            )

        val updated = service.updateWarehouse(w.id, req)
        assertEquals("Nome Atualizado", updated.name)
        assertEquals(WarehouseType.STORAGE, updated.warehouseType)
        verify {
            auditService.recordAudit(
                entityType = "Warehouse",
                entityId = any(),
                action = "UPDATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should deactivate warehouse on delete`() {
        val w = sampleWarehouse()
        every { warehouseRepository.findByIdAndTenantId(w.id, tenantId) } returns Optional.of(w)
        every { warehouseRepository.save(any()) } answers { firstArg() }

        service.deleteWarehouse(w.id)
        assertFalse(w.isActive)
        verify {
            auditService.recordAudit(
                entityType = "Warehouse",
                entityId = any(),
                action = "DEACTIVATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should create physical storage location with formatted address`() {
        val w = sampleWarehouse()
        every { warehouseRepository.findByIdAndTenantId(w.id, tenantId) } returns Optional.of(w)
        every { locationRepository.existsByWarehouseIdAndFormattedAddress(w.id, "A-01-02-03") } returns false
        every { locationRepository.save(any()) } answers { firstArg() }

        val req =
            CreateStorageLocationRequest(
                aisle = "A",
                rack = "01",
                shelf = "02",
                bin = "03",
            )

        val loc = service.createLocation(w.id, req)
        assertEquals("A-01-02-03", loc.formattedAddress)
        verify {
            auditService.recordAudit(
                entityType = "StorageLocation",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should reject duplicate storage location in warehouse`() {
        val w = sampleWarehouse()
        every { warehouseRepository.findByIdAndTenantId(w.id, tenantId) } returns Optional.of(w)
        every { locationRepository.existsByWarehouseIdAndFormattedAddress(w.id, "A-01-02-03") } returns true

        val req =
            CreateStorageLocationRequest(
                aisle = "A",
                rack = "01",
                shelf = "02",
                bin = "03",
            )

        assertThrows<DuplicateStorageLocationException> {
            service.createLocation(w.id, req)
        }
    }

    @Test
    fun `should list storage locations for warehouse`() {
        val w = sampleWarehouse()
        val loc =
            StorageLocation(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                warehouseId = w.id,
                aisle = "A",
                rack = "01",
                shelf = "01",
                bin = "01",
                formattedAddress = "A-01-01-01",
                isActive = true,
            )
        every { locationRepository.findAllByTenantIdAndWarehouseIdOrderByFormattedAddressAsc(tenantId, w.id) } returns listOf(loc)

        val list = service.listLocations(w.id)
        assertEquals(1, list.size)
        assertEquals("A-01-01-01", list[0].formattedAddress)
    }
}
