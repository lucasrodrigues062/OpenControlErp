package org.opencontrol.erp.crm.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.opencontrol.erp.crm.domain.model.Deal
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.opencontrol.erp.crm.domain.model.PipelineStage
import org.opencontrol.erp.crm.domain.model.SalesPipeline
import org.opencontrol.erp.crm.domain.model.StageType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PipelineStageResponse(
    val id: UUID,
    val pipelineId: UUID,
    val name: String,
    val orderIndex: Int,
    val winProbabilityPct: Int,
    val colorHex: String,
    val maxStagnantDays: Int,
    val stageType: StageType,
) {
    companion object {
        fun fromEntity(entity: PipelineStage) =
            PipelineStageResponse(
                id = entity.id,
                pipelineId = entity.pipelineId,
                name = entity.name,
                orderIndex = entity.orderIndex,
                winProbabilityPct = entity.winProbabilityPct,
                colorHex = entity.colorHex,
                maxStagnantDays = entity.maxStagnantDays,
                stageType = entity.stageType,
            )
    }
}

data class CreatePipelineStageRequest(
    @field:NotBlank(message = "Stage name is required")
    val name: String,
    val orderIndex: Int? = null,
    @field:Min(0) @field:Max(100)
    val winProbabilityPct: Int = 10,
    val colorHex: String = "#6366F1",
    @field:Min(1)
    val maxStagnantDays: Int = 15,
    val stageType: StageType = StageType.IN_PROGRESS,
)

data class UpdatePipelineStageRequest(
    @field:NotBlank(message = "Stage name is required")
    val name: String,
    @field:Min(0) @field:Max(100)
    val winProbabilityPct: Int = 10,
    val colorHex: String = "#6366F1",
    @field:Min(1)
    val maxStagnantDays: Int = 15,
    val stageType: StageType = StageType.IN_PROGRESS,
)

data class ReorderStagesRequest(
    val stageIds: List<UUID>,
)

data class SalesPipelineResponse(
    val id: UUID,
    val code: String,
    val name: String,
    @get:JsonProperty("isDefault")
    val isDefault: Boolean,
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val stages: List<PipelineStageResponse>,
) {
    companion object {
        fun fromEntity(
            entity: SalesPipeline,
            stages: List<PipelineStage>,
        ) = SalesPipelineResponse(
            id = entity.id,
            code = entity.code,
            name = entity.name,
            isDefault = entity.isDefault,
            isActive = entity.isActive,
            stages = stages.map { PipelineStageResponse.fromEntity(it) },
        )
    }
}

data class CreatePipelineRequest(
    @field:NotBlank(message = "Pipeline code is required")
    val code: String,
    @field:NotBlank(message = "Pipeline name is required")
    val name: String,
    val isDefault: Boolean = false,
)

data class UpdatePipelineRequest(
    @field:NotBlank(message = "Pipeline name is required")
    val name: String,
    val isDefault: Boolean = false,
)

data class DealResponse(
    val id: UUID,
    val tenantId: UUID,
    val branchId: UUID?,
    val pipelineId: UUID,
    val stageId: UUID,
    val stageName: String?,
    val stageColorHex: String?,
    val customerId: UUID,
    val customerName: String?,
    val leadId: UUID?,
    val title: String,
    val estimatedValue: BigDecimal,
    val currency: String,
    val expectedCloseDate: LocalDate?,
    val status: DealStatus,
    val lossReason: String?,
    val assignedUserId: UUID?,
    val closedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: Deal) =
            DealResponse(
                id = entity.id,
                tenantId = entity.tenantId,
                branchId = entity.branchId,
                pipelineId = entity.pipelineId,
                stageId = entity.stageId,
                stageName = entity.stage?.name,
                stageColorHex = entity.stage?.colorHex,
                customerId = entity.customerId,
                customerName = entity.customer?.legalName,
                leadId = entity.leadId,
                title = entity.title,
                estimatedValue = entity.estimatedValue,
                currency = entity.currency,
                expectedCloseDate = entity.expectedCloseDate,
                status = entity.status,
                lossReason = entity.lossReason,
                assignedUserId = entity.assignedUserId,
                closedAt = entity.closedAt,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

data class CreateDealRequest(
    @field:NotBlank(message = "Deal title is required")
    val title: String,
    @field:NotNull(message = "Pipeline ID is required")
    val pipelineId: UUID,
    @field:NotNull(message = "Stage ID is required")
    val stageId: UUID,
    @field:NotNull(message = "Customer ID is required")
    val customerId: UUID,
    val leadId: UUID? = null,
    val estimatedValue: BigDecimal? = BigDecimal.ZERO,
    val currency: String? = "BRL",
    val expectedCloseDate: LocalDate? = null,
    val assignedUserId: UUID? = null,
    val branchId: UUID? = null,
)

data class UpdateDealStageRequest(
    @field:NotNull(message = "Target stage ID is required")
    val stageId: UUID,
    val lossReason: String? = null,
)

data class PipelineMetricsResponse(
    val totalOpenDeals: Int,
    val totalPipelineValue: BigDecimal,
    val weightedPipelineValue: BigDecimal,
    val winRatePct: BigDecimal,
)
