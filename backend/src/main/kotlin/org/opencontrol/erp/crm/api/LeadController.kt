package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.ConvertLeadRequest
import org.opencontrol.erp.crm.api.dto.CreateLeadRequest
import org.opencontrol.erp.crm.api.dto.DisqualifyLeadRequest
import org.opencontrol.erp.crm.api.dto.LeadConversionResponse
import org.opencontrol.erp.crm.api.dto.LeadResponse
import org.opencontrol.erp.crm.domain.model.LeadSource
import org.opencontrol.erp.crm.domain.model.LeadStatus
import org.opencontrol.erp.crm.service.LeadService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/leads")
@Tag(name = "Leads", description = "Prospective leads capture, qualification, and 1-click conversion")
class LeadController(
    private val leadService: LeadService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List leads with status and source filters")
    fun list(
        @RequestParam(required = false) status: LeadStatus?,
        @RequestParam(required = false) source: LeadSource?,
        @RequestParam(required = false) assignedUserId: UUID?,
    ): ResponseEntity<List<LeadResponse>> = ResponseEntity.ok(leadService.listLeads(status, source, assignedUserId))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get lead details by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<LeadResponse> = ResponseEntity.ok(leadService.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Create prospect lead")
    fun create(
        @Valid @RequestBody request: CreateLeadRequest,
    ): ResponseEntity<LeadResponse> = ResponseEntity.status(HttpStatus.CREATED).body(leadService.createLead(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update lead details")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateLeadRequest,
    ): ResponseEntity<LeadResponse> = ResponseEntity.ok(leadService.updateLead(id, request))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update lead qualification status")
    fun updateStatus(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, String>,
    ): ResponseEntity<LeadResponse> {
        val status = LeadStatus.valueOf(body["status"] ?: "NEW")
        return ResponseEntity.ok(leadService.updateStatus(id, status))
    }

    @PostMapping("/{id}/disqualify")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Disqualify lead with reason")
    fun disqualify(
        @PathVariable id: UUID,
        @Valid @RequestBody request: DisqualifyLeadRequest,
    ): ResponseEntity<LeadResponse> = ResponseEntity.ok(leadService.disqualifyLead(id, request.reason))

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "One-click convert lead to Customer and Deal")
    fun convert(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ConvertLeadRequest,
    ): ResponseEntity<LeadConversionResponse> = ResponseEntity.ok(leadService.convertLead(id, request))
}
