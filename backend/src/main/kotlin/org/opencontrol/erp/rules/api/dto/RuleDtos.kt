package org.opencontrol.erp.rules.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.ErrorHandlingPolicy
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.domain.RuleTiming
import org.opencontrol.erp.rules.domain.TraceResult
import java.time.Instant
import java.util.UUID

data class RuleRequestDto(
    @field:NotBlank(message = "Rule name is required")
    @field:Size(max = 150, message = "Rule name cannot exceed 150 characters")
    val name: String,
    val description: String? = null,
    @field:NotBlank(message = "Trigger event is required")
    val triggerEvent: String,
    val timing: RuleTiming = RuleTiming.BEFORE,
    val authoringMode: AuthoringMode = AuthoringMode.VISUAL_BUILDER,
    @field:NotBlank(message = "Condition expression is required")
    val conditionExpression: String,
    @field:NotNull(message = "Action type is required")
    val actionType: RuleActionType,
    val actionPayload: String? = null,
    val priority: Int = 100,
    val isActive: Boolean = true,
    val errorHandlingPolicy: ErrorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
)

data class RuleResponseDto(
    val id: UUID,
    val tenantId: UUID,
    val name: String,
    val description: String?,
    val triggerEvent: String,
    val timing: RuleTiming,
    val authoringMode: AuthoringMode,
    val conditionExpression: String,
    val actionType: RuleActionType,
    val actionPayload: String?,
    val priority: Int,
    val isActive: Boolean,
    val errorHandlingPolicy: ErrorHandlingPolicy,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class RuleSummaryDto(
    val id: UUID,
    val name: String,
    val description: String?,
    val triggerEvent: String,
    val timing: RuleTiming,
    val authoringMode: AuthoringMode,
    val actionType: RuleActionType,
    val priority: Int,
    val isActive: Boolean,
    val errorHandlingPolicy: ErrorHandlingPolicy,
    val createdAt: Instant,
)

data class RuleSimulationRequestDto(
    val rule: RuleRequestDto? = null,
    val ruleId: UUID? = null,
    val context: Map<String, Any?> = emptyMap(),
)

data class RuleSimulationResponseDto(
    val matched: Boolean,
    val actionType: RuleActionType? = null,
    val actionPayload: String? = null,
    val simulatedError: String? = null,
    val mutatedContext: Map<String, Any?>? = null,
    val durationMs: Long,
    val errorDetail: String? = null,
)

data class RuleTraceResponseDto(
    val id: UUID,
    val tenantId: UUID,
    val ruleId: UUID,
    val triggerEvent: String,
    val entityType: String,
    val entityId: String,
    val durationMs: Long,
    val result: TraceResult,
    val errorDetail: String?,
    val createdAt: Instant,
)
