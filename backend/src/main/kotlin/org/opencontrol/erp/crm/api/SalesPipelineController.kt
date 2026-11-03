package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.CreatePipelineRequest
import org.opencontrol.erp.crm.api.dto.CreatePipelineStageRequest
import org.opencontrol.erp.crm.api.dto.PipelineStageResponse
import org.opencontrol.erp.crm.api.dto.ReorderStagesRequest
import org.opencontrol.erp.crm.api.dto.SalesPipelineResponse
import org.opencontrol.erp.crm.api.dto.UpdatePipelineRequest
import org.opencontrol.erp.crm.api.dto.UpdatePipelineStageRequest
import org.opencontrol.erp.crm.service.SalesPipelineService
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/pipelines")
@Tag(name = "Sales Pipelines", description = "Custom commercial sales pipelines and stages")
class SalesPipelineController(
    private val pipelineService: SalesPipelineService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List sales pipelines and stages")
    fun list(): ResponseEntity<List<SalesPipelineResponse>> = ResponseEntity.ok(pipelineService.listPipelines())

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get sales pipeline by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<SalesPipelineResponse> = ResponseEntity.ok(pipelineService.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Create sales pipeline")
    fun create(
        @Valid @RequestBody request: CreatePipelineRequest,
    ): ResponseEntity<SalesPipelineResponse> = ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.createPipeline(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update sales pipeline")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePipelineRequest,
    ): ResponseEntity<SalesPipelineResponse> = ResponseEntity.ok(pipelineService.updatePipeline(id, request))

    @PostMapping("/{pipelineId}/stages")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Add a stage to a sales pipeline")
    fun addStage(
        @PathVariable pipelineId: UUID,
        @Valid @RequestBody request: CreatePipelineStageRequest,
    ): ResponseEntity<PipelineStageResponse> = ResponseEntity.status(HttpStatus.CREATED).body(pipelineService.addStage(pipelineId, request))

    @PutMapping("/{pipelineId}/stages/{stageId}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update a stage in a sales pipeline")
    fun updateStage(
        @PathVariable pipelineId: UUID,
        @PathVariable stageId: UUID,
        @Valid @RequestBody request: UpdatePipelineStageRequest,
    ): ResponseEntity<PipelineStageResponse> = ResponseEntity.ok(pipelineService.updateStage(pipelineId, stageId, request))

    @DeleteMapping("/{pipelineId}/stages/{stageId}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Delete a stage from a sales pipeline")
    fun deleteStage(
        @PathVariable pipelineId: UUID,
        @PathVariable stageId: UUID,
    ): ResponseEntity<Unit> {
        pipelineService.deleteStage(pipelineId, stageId)
        return ResponseEntity.noContent().build()
    }

    @PutMapping("/{pipelineId}/stages/reorder")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Reorder stages in a sales pipeline")
    fun reorderStages(
        @PathVariable pipelineId: UUID,
        @Valid @RequestBody request: ReorderStagesRequest,
    ): ResponseEntity<List<PipelineStageResponse>> = ResponseEntity.ok(pipelineService.reorderStages(pipelineId, request))
}
