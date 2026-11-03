package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.CreateDealRequest
import org.opencontrol.erp.crm.api.dto.DealResponse
import org.opencontrol.erp.crm.api.dto.PipelineMetricsResponse
import org.opencontrol.erp.crm.api.dto.UpdateDealStageRequest
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.opencontrol.erp.crm.service.DealService
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
@RequestMapping("/api/v1/crm/deals")
@Tag(name = "Deals", description = "Sales opportunities and visual pipeline Kanban tracking")
class DealController(
    private val dealService: DealService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List deals with filters (for Kanban and list views)")
    fun list(
        @RequestParam(required = false) pipelineId: UUID?,
        @RequestParam(required = false) stageId: UUID?,
        @RequestParam(required = false) status: DealStatus?,
        @RequestParam(required = false) assignedUserId: UUID?,
        @RequestParam(required = false) branchId: UUID?,
    ): ResponseEntity<List<DealResponse>> =
        ResponseEntity.ok(
            dealService.listDeals(
                pipelineId = pipelineId,
                stageId = stageId,
                status = status,
                assignedUserId = assignedUserId,
                branchId = branchId,
            ),
        )

    @GetMapping("/metrics")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get aggregate pipeline metrics")
    fun getMetrics(
        @RequestParam(required = false) pipelineId: UUID?,
    ): ResponseEntity<PipelineMetricsResponse> = ResponseEntity.ok(dealService.getPipelineMetrics(pipelineId))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get deal details by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<DealResponse> = ResponseEntity.ok(dealService.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Create sales deal / opportunity")
    fun create(
        @Valid @RequestBody request: CreateDealRequest,
    ): ResponseEntity<DealResponse> = ResponseEntity.status(HttpStatus.CREATED).body(dealService.createDeal(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update deal details")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateDealRequest,
    ): ResponseEntity<DealResponse> = ResponseEntity.ok(dealService.updateDeal(id, request))

    @PatchMapping("/{id}/stage")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Move deal to another stage (Kanban drag-and-drop)")
    fun updateStage(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateDealStageRequest,
    ): ResponseEntity<DealResponse> = ResponseEntity.ok(dealService.updateDealStage(id, request.stageId, request.lossReason))
}
