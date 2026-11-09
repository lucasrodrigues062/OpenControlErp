package org.opencontrol.erp.inbound.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inbound.api.dto.FinalizeInvoiceStockRequest
import org.opencontrol.erp.inbound.api.dto.InboundInvoiceResponse
import org.opencontrol.erp.inbound.api.dto.MatchProductRequest
import org.opencontrol.erp.inbound.service.InboundInvoiceService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inbound/invoices")
@Tag(name = "Inbound - Fiscal Invoices", description = "SEFAZ NF-e XML ingestion, supplier product de-para, and landed cost stocking")
class InboundInvoiceController(
    private val service: InboundInvoiceService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "List inbound invoices")
    fun list(
        @RequestParam(required = false) branchId: UUID?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<List<InboundInvoiceResponse>> = ResponseEntity.ok(service.listInvoices(branchId, status))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Get inbound invoice by ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<InboundInvoiceResponse> =
        ResponseEntity.ok(service.getInvoiceById(id))

    @PostMapping("/upload-xml", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Upload and process SEFAZ NF-e XML file")
    fun uploadXml(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("targetBranchId") targetBranchId: UUID,
        @RequestParam(value = "costFormulaProfileId", required = false) costFormulaProfileId: UUID?,
        @RequestParam(value = "autoRegisterMissingProducts", required = false, defaultValue = "false") autoRegisterMissingProducts: Boolean = false,
    ): ResponseEntity<InboundInvoiceResponse> =
        ResponseEntity.ok(service.uploadXml(file, targetBranchId, costFormulaProfileId, autoRegisterMissingProducts))

    @PostMapping("/{id}/auto-register-missing-products")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Automatically register and map missing products in catalog from invoice items")
    fun autoRegisterMissingProducts(
        @PathVariable id: UUID,
    ): ResponseEntity<InboundInvoiceResponse> =
        ResponseEntity.ok(service.autoRegisterMissingProducts(id))

    @PutMapping("/{id}/items/{itemId}/match")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Match supplier line item with internal catalog product and conversion factor")
    fun matchProduct(
        @PathVariable id: UUID,
        @PathVariable itemId: UUID,
        @Valid @RequestBody request: MatchProductRequest,
    ): ResponseEntity<InboundInvoiceResponse> =
        ResponseEntity.ok(service.matchProduct(id, itemId, request))

    @PostMapping("/{id}/finalize-stock")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Finalize invoice, commit stock balances and recalculate product CMP")
    fun finalizeStock(
        @PathVariable id: UUID,
        @Valid @RequestBody request: FinalizeInvoiceStockRequest,
    ): ResponseEntity<InboundInvoiceResponse> =
        ResponseEntity.ok(service.finalizeStock(id, request))
}
