package org.opencontrol.erp.inbound.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inbound.api.dto.CreateTransferRequest
import org.opencontrol.erp.inbound.api.dto.StockTransferResponse
import org.opencontrol.erp.inbound.domain.model.StockTransferStatus
import org.opencontrol.erp.inbound.domain.model.StockTransferType
import org.opencontrol.erp.inbound.service.StockTransferService
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
@RequestMapping("/api/v1/inbound/transfers")
@Tag(name = "Inbound - Stock Transfers", description = "Internal warehouse and inter-branch stock transfers with in-transit tracking and CMP recalculation")
class StockTransferController(
    private val service: StockTransferService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "List internal and inter-branch stock transfers")
    fun list(
        @RequestParam(required = false) transferType: StockTransferType?,
        @RequestParam(required = false) status: StockTransferStatus?,
    ): ResponseEntity<List<StockTransferResponse>> =
        ResponseEntity.ok(service.listTransfers(transferType, status))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Get stock transfer by ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<StockTransferResponse> =
        ResponseEntity.ok(service.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Create internal warehouse or inter-branch stock transfer")
    fun create(
        @Valid @RequestBody request: CreateTransferRequest,
    ): ResponseEntity<StockTransferResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createTransfer(request))

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Receive inter-branch transfer at destination branch")
    fun receive(
        @PathVariable id: UUID,
    ): ResponseEntity<StockTransferResponse> =
        ResponseEntity.ok(service.receiveTransfer(id))
}
