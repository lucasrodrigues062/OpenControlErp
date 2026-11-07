package org.opencontrol.erp.inventory.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inventory.api.dto.CreateStockMovementRequest
import org.opencontrol.erp.inventory.api.dto.StockMovementPageResponse
import org.opencontrol.erp.inventory.api.dto.StockMovementResponse
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.service.KardexMovementService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/movements")
@Tag(name = "Inventory - Kardex Ledger", description = "Immutable append-only stock movement ledger and cycle count adjustments")
class StockMovementController(
    private val service: KardexMovementService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Query immutable Kardex stock movement transactions")
    fun listMovements(
        @RequestParam(required = false) productId: UUID?,
        @RequestParam(required = false) warehouseId: UUID?,
        @RequestParam(required = false) movementType: StockMovementType?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<StockMovementPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return ResponseEntity.ok(service.listMovements(productId, warehouseId, movementType, pageable))
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_WRITE') or hasAuthority('INVENTORY_ADMIN')")
    @Operation(summary = "Post a stock movement (inbound, outbound, transfer, manual adjustment)")
    fun recordMovement(
        @Valid @RequestBody request: CreateStockMovementRequest,
    ): ResponseEntity<StockMovementResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.recordMovement(request))
}
