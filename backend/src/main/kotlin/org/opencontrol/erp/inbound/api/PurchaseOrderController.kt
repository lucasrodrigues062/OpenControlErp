package org.opencontrol.erp.inbound.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inbound.api.dto.CreatePurchaseOrderRequest
import org.opencontrol.erp.inbound.api.dto.PurchaseOrderResponse
import org.opencontrol.erp.inbound.service.PurchaseOrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inbound/purchase-orders")
@Tag(name = "Inbound - Purchase Orders", description = "Vendor procurement orders and commitment tracking")
class PurchaseOrderController(
    private val service: PurchaseOrderService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "List purchase orders")
    fun list(
        @RequestParam(required = false) branchId: UUID?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<List<PurchaseOrderResponse>> = ResponseEntity.ok(service.listPurchaseOrders(branchId, status))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Get purchase order by ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(service.getPurchaseOrderById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Create a purchase order")
    fun create(
        @Valid @RequestBody request: CreatePurchaseOrderRequest,
    ): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createPurchaseOrder(request))

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Approve purchase order")
    fun approve(@PathVariable id: UUID): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(service.approvePurchaseOrder(id))

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Cancel purchase order")
    fun cancel(@PathVariable id: UUID): ResponseEntity<PurchaseOrderResponse> =
        ResponseEntity.ok(service.cancelPurchaseOrder(id))
}
