package org.opencontrol.erp.rules.service

import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.rules.api.dto.RuleSimulationRequestDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationResponseDto
import org.opencontrol.erp.rules.api.dto.RuleTraceResponseDto
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.ErrorHandlingPolicy
import org.opencontrol.erp.rules.domain.RuleExecutionTrace
import org.opencontrol.erp.rules.domain.RuleTiming
import org.opencontrol.erp.rules.domain.TraceResult
import org.opencontrol.erp.rules.exception.BusinessRuleViolationException
import org.opencontrol.erp.rules.exception.RuleEvaluationException
import org.opencontrol.erp.rules.repository.CustomRuleRepository
import org.opencontrol.erp.rules.repository.RuleExecutionTraceRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.Executors

@Service
class RuleExecutionService(
    private val ruleRepository: CustomRuleRepository,
    private val traceRepository: RuleExecutionTraceRepository,
    private val evaluationEngine: RuleEvaluationEngine,
) {
    private val logger = LoggerFactory.getLogger(RuleExecutionService::class.java)
    private val virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()

    fun simulateRule(
        tenantId: UUID,
        request: RuleSimulationRequestDto,
    ): RuleSimulationResponseDto {
        val rule: CustomRule =
            when {
                request.ruleId != null -> {
                    ruleRepository
                        .findByTenantIdAndId(tenantId, request.ruleId)
                        .orElseThrow { EntityNotFoundException("Custom rule not found: ${request.ruleId}") }
                }
                request.rule != null -> {
                    val dto = request.rule
                    CustomRule(
                        tenantId = tenantId,
                        name = dto.name,
                        description = dto.description,
                        triggerEvent = dto.triggerEvent,
                        timing = dto.timing,
                        authoringMode = dto.authoringMode,
                        conditionExpression = dto.conditionExpression,
                        actionType = dto.actionType,
                        actionPayload = dto.actionPayload,
                        priority = dto.priority,
                        isActive = dto.isActive,
                        errorHandlingPolicy = dto.errorHandlingPolicy,
                    )
                }
                else -> throw RuleEvaluationException("Either ruleId or inline rule definition must be provided")
            }

        val startTime = System.currentTimeMillis()
        return try {
            val eval = evaluationEngine.evaluate(rule, request.context)
            RuleSimulationResponseDto(
                matched = eval.matched,
                actionType = eval.actionType,
                actionPayload = eval.actionPayload,
                mutatedContext = eval.mutatedContext,
                durationMs = eval.durationMs,
                errorDetail = eval.errorMessage,
            )
        } catch (ex: BusinessRuleViolationException) {
            val duration = System.currentTimeMillis() - startTime
            RuleSimulationResponseDto(
                matched = true,
                actionType = rule.actionType,
                actionPayload = rule.actionPayload,
                simulatedError = ex.message,
                durationMs = duration,
            )
        } catch (ex: Exception) {
            val duration = System.currentTimeMillis() - startTime
            RuleSimulationResponseDto(
                matched = false,
                actionType = rule.actionType,
                actionPayload = rule.actionPayload,
                durationMs = duration,
                errorDetail = ex.message,
            )
        }
    }

    @Transactional
    fun executeRules(
        tenantId: UUID,
        triggerEvent: String,
        timing: RuleTiming,
        context: Map<String, Any?>,
        entityType: String = "DOMAIN_ENTITY",
        entityId: String = "UNKNOWN",
    ): Map<String, Any?> {
        val rules =
            ruleRepository
                .findAllByTenantIdAndTriggerEventAndIsActiveOrderByPriorityAsc(
                    tenantId = tenantId,
                    triggerEvent = triggerEvent,
                    isActive = true,
                ).filter { it.timing == timing }

        if (rules.isEmpty()) {
            return context
        }

        var currentContext = context
        val maxDepth = 5
        var executionCount = 0

        for (rule in rules) {
            if (++executionCount > maxDepth) {
                logger.warn("Rule execution loop detected for event '{}', aborting chain", triggerEvent)
                break
            }

            try {
                val eval = evaluationEngine.evaluate(rule, currentContext)
                if (eval.matched) {
                    recordTraceAsync(
                        tenantId = tenantId,
                        ruleId = rule.id,
                        triggerEvent = triggerEvent,
                        entityType = entityType,
                        entityId = entityId,
                        durationMs = eval.durationMs,
                        result = TraceResult.MATCHED,
                    )
                    if (eval.mutatedContext != null) {
                        currentContext = eval.mutatedContext
                    }
                } else if (eval.errorMessage != null) {
                    recordTraceAsync(
                        tenantId = tenantId,
                        ruleId = rule.id,
                        triggerEvent = triggerEvent,
                        entityType = entityType,
                        entityId = entityId,
                        durationMs = eval.durationMs,
                        result = TraceResult.ERROR,
                        errorDetail = eval.errorMessage,
                    )
                    if (rule.errorHandlingPolicy == ErrorHandlingPolicy.ABORT_TRANSACTION) {
                        throw RuleEvaluationException(
                            message = "Rule '${rule.name}' evaluation failed: ${eval.errorMessage}",
                            ruleId = rule.id,
                            ruleName = rule.name,
                        )
                    }
                } else {
                    recordTraceAsync(
                        tenantId = tenantId,
                        ruleId = rule.id,
                        triggerEvent = triggerEvent,
                        entityType = entityType,
                        entityId = entityId,
                        durationMs = eval.durationMs,
                        result = TraceResult.SKIPPED,
                    )
                }
            } catch (ex: BusinessRuleViolationException) {
                recordTraceAsync(
                    tenantId = tenantId,
                    ruleId = rule.id,
                    triggerEvent = triggerEvent,
                    entityType = entityType,
                    entityId = entityId,
                    durationMs = 1L,
                    result = TraceResult.MATCHED,
                    errorDetail = ex.message,
                )
                if (rule.errorHandlingPolicy == ErrorHandlingPolicy.ABORT_TRANSACTION) {
                    throw ex
                } else {
                    logger.info("Rule '{}' violation ignored due to LOG_AND_CONTINUE policy", rule.name)
                }
            }
        }

        return currentContext
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordTrace(
        tenantId: UUID,
        ruleId: UUID,
        triggerEvent: String,
        entityType: String,
        entityId: String,
        durationMs: Long,
        result: TraceResult,
        errorDetail: String? = null,
    ): RuleExecutionTrace {
        val trace =
            RuleExecutionTrace(
                tenantId = tenantId,
                ruleId = ruleId,
                triggerEvent = triggerEvent,
                entityType = entityType,
                entityId = entityId,
                durationMs = durationMs,
                result = result,
                errorDetail = errorDetail,
            )
        return traceRepository.save(trace)
    }

    private fun recordTraceAsync(
        tenantId: UUID,
        ruleId: UUID,
        triggerEvent: String,
        entityType: String,
        entityId: String,
        durationMs: Long,
        result: TraceResult,
        errorDetail: String? = null,
    ) {
        virtualThreadExecutor.submit {
            try {
                recordTrace(
                    tenantId = tenantId,
                    ruleId = ruleId,
                    triggerEvent = triggerEvent,
                    entityType = entityType,
                    entityId = entityId,
                    durationMs = durationMs,
                    result = result,
                    errorDetail = errorDetail,
                )
            } catch (e: Exception) {
                logger.error("Failed to persist rule execution trace: {}", e.message)
            }
        }
    }

    @Transactional(readOnly = true)
    fun listTraces(
        tenantId: UUID,
        ruleId: UUID?,
        pageable: Pageable,
    ): Page<RuleTraceResponseDto> {
        val page =
            if (ruleId != null) {
                traceRepository.findAllByTenantIdAndRuleId(tenantId, ruleId, pageable)
            } else {
                traceRepository.findAllByTenantId(tenantId, pageable)
            }

        return page.map {
            RuleTraceResponseDto(
                id = it.id,
                tenantId = it.tenantId,
                ruleId = it.ruleId,
                triggerEvent = it.triggerEvent,
                entityType = it.entityType,
                entityId = it.entityId,
                durationMs = it.durationMs,
                result = it.result,
                errorDetail = it.errorDetail,
                createdAt = it.createdAt,
            )
        }
    }
}
