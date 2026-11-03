package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.CreateCrmActivityRequest
import org.opencontrol.erp.crm.api.dto.CrmActivityResponse
import org.opencontrol.erp.crm.domain.model.ActivityEntityType
import org.opencontrol.erp.crm.service.CrmActivityService
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
@RequestMapping("/api/v1/crm/activities")
@Tag(name = "CRM Activities", description = "Chronological customer, lead, and deal interaction timeline")
class CrmActivityController(
    private val activityService: CrmActivityService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List activities for target entity (Customer, Lead, or Deal)")
    fun list(
        @RequestParam entityType: ActivityEntityType,
        @RequestParam entityId: UUID,
    ): ResponseEntity<List<CrmActivityResponse>> = ResponseEntity.ok(activityService.listActivities(entityType, entityId))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Log an interaction activity or task")
    fun create(
        @Valid @RequestBody request: CreateCrmActivityRequest,
    ): ResponseEntity<CrmActivityResponse> = ResponseEntity.status(HttpStatus.CREATED).body(activityService.createActivity(request))
}
