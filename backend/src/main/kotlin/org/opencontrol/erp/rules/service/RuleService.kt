package org.opencontrol.erp.rules.service

import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.OpenControlException
import org.opencontrol.erp.rules.api.dto.RuleRequestDto
import org.opencontrol.erp.rules.api.dto.RuleResponseDto
import org.opencontrol.erp.rules.api.dto.RuleSummaryDto
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.repository.CustomRuleRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class RuleService(
    private val ruleRepository: CustomRuleRepository,
) {
    fun createRule(
        tenantId: UUID,
        request: RuleRequestDto,
    ): RuleResponseDto {
        if (ruleRepository.existsByTenantIdAndName(tenantId, request.name)) {
            throw OpenControlException(
                message = "A rule with name '${request.name}' already exists for this tenant",
                errorCode = "RULE_DUPLICATE_NAME",
                status = HttpStatus.CONFLICT,
            )
        }

        val rule =
            CustomRule(
                tenantId = tenantId,
                name = request.name,
                description = request.description,
                triggerEvent = request.triggerEvent,
                timing = request.timing,
                authoringMode = request.authoringMode,
                conditionExpression = request.conditionExpression,
                actionType = request.actionType,
                actionPayload = request.actionPayload,
                priority = request.priority,
                isActive = request.isActive,
                errorHandlingPolicy = request.errorHandlingPolicy,
            )

        return ruleRepository.save(rule).toResponseDto()
    }

    fun updateRule(
        tenantId: UUID,
        id: UUID,
        request: RuleRequestDto,
    ): RuleResponseDto {
        val rule =
            ruleRepository
                .findByTenantIdAndId(tenantId, id)
                .orElseThrow { EntityNotFoundException("Custom rule not found: $id") }

        if (ruleRepository.existsByTenantIdAndNameAndIdNot(tenantId, request.name, id)) {
            throw OpenControlException(
                message = "A rule with name '${request.name}' already exists for this tenant",
                errorCode = "RULE_DUPLICATE_NAME",
                status = HttpStatus.CONFLICT,
            )
        }

        rule.name = request.name
        rule.description = request.description
        rule.triggerEvent = request.triggerEvent
        rule.timing = request.timing
        rule.authoringMode = request.authoringMode
        rule.conditionExpression = request.conditionExpression
        rule.actionType = request.actionType
        rule.actionPayload = request.actionPayload
        rule.priority = request.priority
        rule.isActive = request.isActive
        rule.errorHandlingPolicy = request.errorHandlingPolicy
        rule.updatedAt = Instant.now()

        return ruleRepository.save(rule).toResponseDto()
    }

    @Transactional(readOnly = true)
    fun getRule(
        tenantId: UUID,
        id: UUID,
    ): RuleResponseDto =
        ruleRepository
            .findByTenantIdAndId(tenantId, id)
            .map { it.toResponseDto() }
            .orElseThrow { EntityNotFoundException("Custom rule not found: $id") }

    @Transactional(readOnly = true)
    fun listRules(
        tenantId: UUID,
        pageable: Pageable,
    ): Page<RuleSummaryDto> =
        ruleRepository
            .findAllByTenantId(tenantId, pageable)
            .map { it.toSummaryDto() }

    fun toggleRule(
        tenantId: UUID,
        id: UUID,
    ): RuleResponseDto {
        val rule =
            ruleRepository
                .findByTenantIdAndId(tenantId, id)
                .orElseThrow { EntityNotFoundException("Custom rule not found: $id") }

        rule.isActive = !rule.isActive
        rule.updatedAt = Instant.now()

        return ruleRepository.save(rule).toResponseDto()
    }

    fun deleteRule(
        tenantId: UUID,
        id: UUID,
    ) {
        val rule =
            ruleRepository
                .findByTenantIdAndId(tenantId, id)
                .orElseThrow { EntityNotFoundException("Custom rule not found: $id") }
        ruleRepository.delete(rule)
    }

    private fun CustomRule.toResponseDto() =
        RuleResponseDto(
            id = id,
            tenantId = tenantId,
            name = name,
            description = description,
            triggerEvent = triggerEvent,
            timing = timing,
            authoringMode = authoringMode,
            conditionExpression = conditionExpression,
            actionType = actionType,
            actionPayload = actionPayload,
            priority = priority,
            isActive = isActive,
            errorHandlingPolicy = errorHandlingPolicy,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun CustomRule.toSummaryDto() =
        RuleSummaryDto(
            id = id,
            name = name,
            description = description,
            triggerEvent = triggerEvent,
            timing = timing,
            authoringMode = authoringMode,
            actionType = actionType,
            priority = priority,
            isActive = isActive,
            errorHandlingPolicy = errorHandlingPolicy,
            createdAt = createdAt,
        )
}
