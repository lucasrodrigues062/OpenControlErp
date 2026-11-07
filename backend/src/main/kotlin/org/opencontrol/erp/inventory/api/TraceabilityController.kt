package org.opencontrol.erp.inventory.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inventory.api.dto.CreateLotRequest
import org.opencontrol.erp.inventory.api.dto.CreateSerialNumberRequest
import org.opencontrol.erp.inventory.api.dto.ProductLotResponse
import org.opencontrol.erp.inventory.api.dto.ProductSerialNumberResponse
import org.opencontrol.erp.inventory.api.dto.UpdateSerialStatusRequest
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import org.opencontrol.erp.inventory.service.TraceabilityService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory - Traceability", description = "Batch/Lot FEFO tracking and serialized unit management")
class TraceabilityController(
    private val service: TraceabilityService,
) {
    @GetMapping("/lots")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Query product lots ordered by expiration date (FEFO)")
    fun listLots(
        @RequestParam productId: UUID,
        @RequestParam(required = false) warehouseId: UUID?,
        @RequestParam(defaultValue = "false") availableOnly: Boolean,
    ): ResponseEntity<List<ProductLotResponse>> = ResponseEntity.ok(service.listLots(productId, warehouseId, availableOnly))

    @PostMapping("/lots")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Register a new product lot/batch")
    fun createLot(
        @Valid @RequestBody request: CreateLotRequest,
    ): ResponseEntity<ProductLotResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createLot(request))

    @GetMapping("/serial-numbers")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Query registered unit serial numbers")
    fun listSerialNumbers(
        @RequestParam(required = false) productId: UUID?,
        @RequestParam(required = false) serialNumber: String?,
        @RequestParam(required = false) status: SerialNumberStatus?,
    ): ResponseEntity<List<ProductSerialNumberResponse>> = ResponseEntity.ok(service.listSerialNumbers(productId, serialNumber, status))

    @PostMapping("/serial-numbers")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Register an individual serialized unit")
    fun createSerialNumber(
        @Valid @RequestBody request: CreateSerialNumberRequest,
    ): ResponseEntity<ProductSerialNumberResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createSerialNumber(request))

    @PatchMapping("/serial-numbers/{id}/status")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Update asset lifecycle status of a serial number")
    fun updateSerialStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateSerialStatusRequest,
    ): ResponseEntity<ProductSerialNumberResponse> = ResponseEntity.ok(service.updateSerialNumberStatus(id, request.status))
}
