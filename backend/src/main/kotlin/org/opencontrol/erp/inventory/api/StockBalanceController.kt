package org.opencontrol.erp.inventory.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencontrol.erp.inventory.api.dto.StockBalanceResponse
import org.opencontrol.erp.inventory.service.StockBalanceService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/balances")
@Tag(name = "Inventory - Balances", description = "Current physical, reserved, and available stock positions")
class StockBalanceController(
    private val service: StockBalanceService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "List current stock balances")
    fun listBalances(
        @RequestParam(required = false) warehouseId: UUID?,
        @RequestParam(required = false) productId: UUID?,
    ): ResponseEntity<List<StockBalanceResponse>> = ResponseEntity.ok(service.listBalances(warehouseId, productId))

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_READ') or hasAuthority('INVENTORY_WRITE')")
    @Operation(summary = "Get stock balances for a product across all warehouses")
    fun getProductBalances(
        @PathVariable productId: UUID,
    ): ResponseEntity<List<StockBalanceResponse>> = ResponseEntity.ok(service.getProductBalances(productId))
}
