package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.CreateEconomicGroupRequest
import org.opencontrol.erp.crm.api.dto.EconomicGroupResponse
import org.opencontrol.erp.crm.service.EconomicGroupService
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
@RequestMapping("/api/v1/crm/economic-groups")
@Tag(name = "Economic Groups", description = "Corporate holdings and customer conglomerate management")
class EconomicGroupController(
    private val service: EconomicGroupService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List all economic groups")
    fun list(
        @RequestParam(defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<List<EconomicGroupResponse>> = ResponseEntity.ok(service.listGroups(activeOnly))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get economic group by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<EconomicGroupResponse> = ResponseEntity.ok(service.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Create economic group")
    fun create(
        @Valid @RequestBody request: CreateEconomicGroupRequest,
    ): ResponseEntity<EconomicGroupResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createGroup(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update economic group")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateEconomicGroupRequest,
    ): ResponseEntity<EconomicGroupResponse> = ResponseEntity.ok(service.updateGroup(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Deactivate economic group")
    fun delete(
        @PathVariable id: UUID,
    ) {
        service.deleteGroup(id)
    }
}
