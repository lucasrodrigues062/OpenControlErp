package org.opencontrol.erp.inventory.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.inventory.api.dto.CreateStorageLocationRequest
import org.opencontrol.erp.inventory.api.dto.CreateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.StorageLocationResponse
import org.opencontrol.erp.inventory.api.dto.UpdateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.WarehouseResponse
import org.opencontrol.erp.inventory.domain.model.WarehouseType
import org.opencontrol.erp.inventory.service.WarehouseService
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class WarehouseControllerTest {
    private val service: WarehouseService = mockk()
    private lateinit var controller: WarehouseController

    private val warehouseId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()

    private val sampleWarehouse =
        WarehouseResponse(
            id = warehouseId,
            branchId = branchId,
            code = "WH-01",
            name = "Depósito Principal",
            warehouseType = WarehouseType.COMMERCIAL,
            isDefault = true,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private val sampleLocation =
        StorageLocationResponse(
            id = UUID.randomUUID(),
            warehouseId = warehouseId,
            aisle = "A",
            rack = "01",
            shelf = "02",
            bin = "03",
            formattedAddress = "A-01-02-03",
            isActive = true,
            createdAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = WarehouseController(service)
    }

    @Test
    fun `should list warehouses`() {
        every { service.listWarehouses(branchId) } returns listOf(sampleWarehouse)

        val res = controller.list(branchId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertNotNull(res.body)
        assertEquals(1, res.body!!.size)
        assertEquals("WH-01", res.body!![0].code)
    }

    @Test
    fun `should get warehouse by id`() {
        every { service.getById(warehouseId) } returns sampleWarehouse

        val res = controller.getById(warehouseId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(warehouseId, res.body!!.id)
    }

    @Test
    fun `should create warehouse`() {
        val req =
            CreateWarehouseRequest(
                branchId = branchId,
                code = "WH-01",
                name = "Depósito Principal",
                warehouseType = WarehouseType.COMMERCIAL,
                isDefault = true,
            )
        every { service.createWarehouse(req) } returns sampleWarehouse

        val res = controller.create(req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("WH-01", res.body!!.code)
    }

    @Test
    fun `should update warehouse`() {
        val req =
            UpdateWarehouseRequest(
                branchId = branchId,
                name = "Depósito Central Atualizado",
                warehouseType = WarehouseType.COMMERCIAL,
                isDefault = true,
                isActive = true,
            )
        every { service.updateWarehouse(warehouseId, req) } returns sampleWarehouse

        val res = controller.update(warehouseId, req)
        assertEquals(HttpStatus.OK, res.statusCode)
    }

    @Test
    fun `should delete warehouse`() {
        every { service.deleteWarehouse(warehouseId) } returns Unit

        controller.delete(warehouseId)
    }

    @Test
    fun `should create storage location`() {
        val req =
            CreateStorageLocationRequest(
                aisle = "A",
                rack = "01",
                shelf = "02",
                bin = "03",
            )
        every { service.createLocation(warehouseId, req) } returns sampleLocation

        val res = controller.createLocation(warehouseId, req)
        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertEquals("A-01-02-03", res.body!!.formattedAddress)
    }

    @Test
    fun `should list storage locations`() {
        every { service.listLocations(warehouseId) } returns listOf(sampleLocation)

        val res = controller.listLocations(warehouseId)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body!!.size)
    }
}
