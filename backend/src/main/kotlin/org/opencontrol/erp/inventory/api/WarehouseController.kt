package org.opencontrol.erp.inventory.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inventory.api.dto.CreateStorageLocationRequest
import org.opencontrol.erp.inventory.api.dto.CreateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.StorageLocationResponse
import org.opencontrol.erp.inventory.api.dto.UpdateWarehouseRequest
import org.opencontrol.erp.inventory.api.dto.WarehouseResponse
import org.opencontrol.erp.inventory.service.WarehouseService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/warehouses")
@Tag(name = "Inventory - Warehouses", description = "Multi-warehouse management and logistical bin addressing")
class WarehouseController(
    private val service: WarehouseService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "List warehouses partitioned by company branch")
    fun list(
        @RequestParam(required = false) branchId: UUID?,
    ): ResponseEntity<List<WarehouseResponse>> = ResponseEntity.ok(service.listWarehouses(branchId))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Get warehouse by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<WarehouseResponse> = ResponseEntity.ok(service.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasAuthority('INVENTORY_ADMIN')")
    @Operation(summary = "Create a new warehouse")
    fun create(
        @Valid @RequestBody request: CreateWarehouseRequest,
    ): ResponseEntity<WarehouseResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createWarehouse(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasAuthority('INVENTORY_ADMIN')")
    @Operation(summary = "Update warehouse details")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateWarehouseRequest,
    ): ResponseEntity<WarehouseResponse> = ResponseEntity.ok(service.updateWarehouse(id, request))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasAuthority('INVENTORY_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a warehouse")
    fun delete(
        @PathVariable id: UUID,
    ) {
        service.deleteWarehouse(id)
    }

    @PostMapping("/{id}/locations")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasAuthority('INVENTORY_ADMIN')")
    @Operation(summary = "Create a physical bin address in a warehouse")
    fun createLocation(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateStorageLocationRequest,
    ): ResponseEntity<StorageLocationResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createLocation(id, request))

    @GetMapping("/{id}/locations")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "List all bin addresses in a warehouse")
    fun listLocations(
        @PathVariable id: UUID,
    ): ResponseEntity<List<StorageLocationResponse>> = ResponseEntity.ok(service.listLocations(id))
}
