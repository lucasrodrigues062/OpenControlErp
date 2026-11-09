package org.opencontrol.erp.inbound.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inbound.api.dto.GoodsReceiptResponse
import org.opencontrol.erp.inbound.api.dto.SubmitConferenceRequest
import org.opencontrol.erp.inbound.service.GoodsReceiptService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inbound/goods-receipts")
@Tag(name = "Inbound - Goods Receipts", description = "Physical receiving, blind conference, divergence detection, and lot/serial capture")
class GoodsReceiptController(
    private val service: GoodsReceiptService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "List goods receipt conferences")
    fun list(): ResponseEntity<List<GoodsReceiptResponse>> = ResponseEntity.ok(service.listGoodsReceipts())

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Get goods receipt by ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<GoodsReceiptResponse> =
        ResponseEntity.ok(service.getById(id))

    @PostMapping("/conference")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Submit warehouse receiving conference with lot and serial number capture")
    fun submitConference(
        @Valid @RequestBody request: SubmitConferenceRequest,
    ): ResponseEntity<GoodsReceiptResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.submitConference(request))
}
