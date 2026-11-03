package org.opencontrol.erp.crm.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreatePipelineRequest
import org.opencontrol.erp.crm.api.dto.CreatePipelineStageRequest
import org.opencontrol.erp.crm.api.dto.PipelineStageResponse
import org.opencontrol.erp.crm.api.dto.ReorderStagesRequest
import org.opencontrol.erp.crm.api.dto.SalesPipelineResponse
import org.opencontrol.erp.crm.api.dto.UpdatePipelineRequest
import org.opencontrol.erp.crm.api.dto.UpdatePipelineStageRequest
import org.opencontrol.erp.crm.domain.model.PipelineStage
import org.opencontrol.erp.crm.domain.model.SalesPipeline
import org.opencontrol.erp.crm.domain.model.StageType
import org.opencontrol.erp.crm.exception.DuplicateCodeException
import org.opencontrol.erp.crm.exception.PipelineNotFoundException
import org.opencontrol.erp.crm.exception.PipelineStageNotFoundException
import org.opencontrol.erp.crm.exception.StageHasActiveDealsException
import org.opencontrol.erp.crm.repository.DealRepository
import org.opencontrol.erp.crm.repository.PipelineStageRepository
import org.opencontrol.erp.crm.repository.SalesPipelineRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class SalesPipelineService(
    private val pipelineRepository: SalesPipelineRepository,
    private val stageRepository: PipelineStageRepository,
    private val dealRepository: DealRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    fun getOrCreateDefaultPipeline(tenantId: UUID): SalesPipeline {
        val existing = pipelineRepository.findByTenantIdAndIsDefaultTrue(tenantId)
        if (existing.isPresent) return existing.get()

        val all = pipelineRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId)
        if (all.isNotEmpty()) return all.first()

        // Create standard default pipeline
        val pipeline =
            SalesPipeline(
                tenantId = tenantId,
                code = "DEFAULT",
                name = "Funil de Vendas Padrão",
                isDefault = true,
                isActive = true,
            )
        val savedPipeline = pipelineRepository.save(pipeline)

        val stages =
            listOf(
                PipelineStage(
                    pipelineId = savedPipeline.id,
                    name = "Prospecção",
                    orderIndex = 0,
                    winProbabilityPct = 10,
                    colorHex = "#6366F1",
                    stageType = StageType.IN_PROGRESS,
                ),
                PipelineStage(
                    pipelineId = savedPipeline.id,
                    name = "Qualificação",
                    orderIndex = 1,
                    winProbabilityPct = 25,
                    colorHex = "#3B82F6",
                    stageType = StageType.IN_PROGRESS,
                ),
                PipelineStage(
                    pipelineId = savedPipeline.id,
                    name = "Proposta Enviada",
                    orderIndex = 2,
                    winProbabilityPct = 50,
                    colorHex = "#F59E0B",
                    stageType = StageType.IN_PROGRESS,
                ),
                PipelineStage(
                    pipelineId = savedPipeline.id,
                    name = "Negociação",
                    orderIndex = 3,
                    winProbabilityPct = 75,
                    colorHex = "#8B5CF6",
                    stageType = StageType.IN_PROGRESS,
                ),
                PipelineStage(
                    pipelineId = savedPipeline.id,
                    name = "Fechado / Ganho",
                    orderIndex = 4,
                    winProbabilityPct = 100,
                    colorHex = "#10B981",
                    stageType = StageType.WON,
                ),
                PipelineStage(
                    pipelineId = savedPipeline.id,
                    name = "Fechado / Perdido",
                    orderIndex = 5,
                    winProbabilityPct = 0,
                    colorHex = "#EF4444",
                    stageType = StageType.LOST,
                ),
            )
        stageRepository.saveAll(stages)

        return savedPipeline
    }

    fun listPipelines(): List<SalesPipelineResponse> {
        val tenantId = getCurrentTenantId()
        var pipelines = pipelineRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId)
        if (pipelines.isEmpty()) {
            getOrCreateDefaultPipeline(tenantId)
            pipelines = pipelineRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId)
        }

        return pipelines.map { p ->
            val stages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(p.id)
            SalesPipelineResponse.fromEntity(p, stages)
        }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): SalesPipelineResponse {
        val tenantId = getCurrentTenantId()
        val pipeline =
            pipelineRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { PipelineNotFoundException(id) }
        val stages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipeline.id)
        return SalesPipelineResponse.fromEntity(pipeline, stages)
    }

    fun createPipeline(request: CreatePipelineRequest): SalesPipelineResponse {
        val tenantId = getCurrentTenantId()
        val code = request.code.trim().uppercase()
        if (pipelineRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw DuplicateCodeException(code)
        }

        val pipeline =
            SalesPipeline(
                tenantId = tenantId,
                code = code,
                name = request.name.trim(),
                isDefault = request.isDefault,
                isActive = true,
            )
        val saved = pipelineRepository.save(pipeline)

        // Seed default stages for new pipeline
        val stages =
            listOf(
                PipelineStage(
                    pipelineId = saved.id,
                    name = "Contato Inicial",
                    orderIndex = 0,
                    winProbabilityPct = 20,
                    colorHex = "#6366F1",
                    stageType = StageType.IN_PROGRESS,
                ),
                PipelineStage(
                    pipelineId = saved.id,
                    name = "Proposta",
                    orderIndex = 1,
                    winProbabilityPct = 60,
                    colorHex = "#F59E0B",
                    stageType = StageType.IN_PROGRESS,
                ),
                PipelineStage(
                    pipelineId = saved.id,
                    name = "Ganho",
                    orderIndex = 2,
                    winProbabilityPct = 100,
                    colorHex = "#10B981",
                    stageType = StageType.WON,
                ),
                PipelineStage(
                    pipelineId = saved.id,
                    name = "Perdido",
                    orderIndex = 3,
                    winProbabilityPct = 0,
                    colorHex = "#EF4444",
                    stageType = StageType.LOST,
                ),
            )
        val savedStages = stageRepository.saveAll(stages)

        auditService.recordAudit(
            entityType = "SalesPipeline",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created sales pipeline ${saved.code} - ${saved.name}",
            stateAfter = saved,
        )

        return SalesPipelineResponse.fromEntity(saved, savedStages)
    }

    fun updatePipeline(
        id: UUID,
        request: UpdatePipelineRequest,
    ): SalesPipelineResponse {
        val tenantId = getCurrentTenantId()
        val pipeline =
            pipelineRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { PipelineNotFoundException(id) }

        pipeline.name = request.name.trim()
        pipeline.isDefault = request.isDefault
        pipeline.updatedAt = Instant.now()

        val saved = pipelineRepository.save(pipeline)
        val stages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(id)

        auditService.recordAudit(
            entityType = "SalesPipeline",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated sales pipeline ${saved.code}",
            stateAfter = saved,
        )

        return SalesPipelineResponse.fromEntity(saved, stages)
    }

    fun addStage(
        pipelineId: UUID,
        request: CreatePipelineStageRequest,
    ): PipelineStageResponse {
        val tenantId = getCurrentTenantId()
        pipelineRepository
            .findByIdAndTenantId(pipelineId, tenantId)
            .orElseThrow { PipelineNotFoundException(pipelineId) }

        val existingStages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipelineId)
        val nextOrderIndex = request.orderIndex ?: (existingStages.maxOfOrNull { it.orderIndex }?.plus(1) ?: 0)

        val stage =
            PipelineStage(
                pipelineId = pipelineId,
                name = request.name.trim(),
                orderIndex = nextOrderIndex,
                winProbabilityPct = request.winProbabilityPct,
                colorHex = request.colorHex,
                maxStagnantDays = request.maxStagnantDays,
                stageType = request.stageType,
            )

        val saved = stageRepository.save(stage)

        auditService.recordAudit(
            entityType = "PipelineStage",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Added stage '${saved.name}' to pipeline $pipelineId",
            stateAfter = saved,
        )

        return PipelineStageResponse.fromEntity(saved)
    }

    fun updateStage(
        pipelineId: UUID,
        stageId: UUID,
        request: UpdatePipelineStageRequest,
    ): PipelineStageResponse {
        val tenantId = getCurrentTenantId()
        pipelineRepository
            .findByIdAndTenantId(pipelineId, tenantId)
            .orElseThrow { PipelineNotFoundException(pipelineId) }

        val stage =
            stageRepository
                .findByIdAndPipelineId(stageId, pipelineId)
                .orElseThrow { PipelineStageNotFoundException(stageId) }

        stage.name = request.name.trim()
        stage.winProbabilityPct = request.winProbabilityPct
        stage.colorHex = request.colorHex
        stage.maxStagnantDays = request.maxStagnantDays
        stage.stageType = request.stageType

        val saved = stageRepository.save(stage)

        auditService.recordAudit(
            entityType = "PipelineStage",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated stage '${saved.name}' in pipeline $pipelineId",
            stateAfter = saved,
        )

        return PipelineStageResponse.fromEntity(saved)
    }

    fun deleteStage(
        pipelineId: UUID,
        stageId: UUID,
    ) {
        val tenantId = getCurrentTenantId()
        pipelineRepository
            .findByIdAndTenantId(pipelineId, tenantId)
            .orElseThrow { PipelineNotFoundException(pipelineId) }

        val stage =
            stageRepository
                .findByIdAndPipelineId(stageId, pipelineId)
                .orElseThrow { PipelineStageNotFoundException(stageId) }

        if (dealRepository.existsByStageId(stageId)) {
            throw StageHasActiveDealsException("Não é possível excluir esta etapa pois existem oportunidades vinculadas a ela.")
        }

        stageRepository.delete(stage)

        auditService.recordAudit(
            entityType = "PipelineStage",
            entityId = stageId.toString(),
            action = "DELETE",
            details = "Deleted stage '${stage.name}' from pipeline $pipelineId",
        )
    }

    fun reorderStages(
        pipelineId: UUID,
        request: ReorderStagesRequest,
    ): List<PipelineStageResponse> {
        val tenantId = getCurrentTenantId()
        pipelineRepository
            .findByIdAndTenantId(pipelineId, tenantId)
            .orElseThrow { PipelineNotFoundException(pipelineId) }

        val stages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipelineId)
        val stageMap = stages.associateBy { it.id }

        request.stageIds.forEachIndexed { index, stageId ->
            stageMap[stageId]?.let { stage ->
                stage.orderIndex = index
            }
        }

        stageRepository.saveAll(stages)

        val updatedStages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipelineId)
        return updatedStages.map { PipelineStageResponse.fromEntity(it) }
    }
}
