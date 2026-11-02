package org.opencontrol.erp.rules.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.rules.api.dto.RuleRequestDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationRequestDto
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.ErrorHandlingPolicy
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.domain.RuleExecutionTrace
import org.opencontrol.erp.rules.domain.RuleTiming
import org.opencontrol.erp.rules.domain.TraceResult
import org.opencontrol.erp.rules.exception.BusinessRuleViolationException
import org.opencontrol.erp.rules.exception.RuleEvaluationException
import org.opencontrol.erp.rules.repository.CustomRuleRepository
import org.opencontrol.erp.rules.repository.RuleExecutionTraceRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleExecutionServiceTest {
    private val ruleRepository: CustomRuleRepository = mockk(relaxed = true)
    private val traceRepository: RuleExecutionTraceRepository = mockk(relaxed = true)
    private val evaluationEngine: RuleEvaluationEngine = mockk(relaxed = true)

    private lateinit var service: RuleExecutionService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = RuleExecutionService(ruleRepository, traceRepository, evaluationEngine)
    }

    @Test
    fun `should simulate rule dry-run with inline rule successfully`() {
        val request =
            RuleSimulationRequestDto(
                rule =
                    RuleRequestDto(
                        name = "Test Discount",
                        triggerEvent = "sale.item_added",
                        timing = RuleTiming.BEFORE,
                        authoringMode = AuthoringMode.ADVANCED_DSL,
                        conditionExpression = "#item.discount > 10.0",
                        actionType = RuleActionType.APPLY_DISCOUNT,
                        actionPayload = """{"discount": 5.0}""",
                    ),
                context = mapOf("item" to mapOf("discount" to 15.0)),
            )

        every { evaluationEngine.evaluate(any<CustomRule>(), any()) } returns
            EvaluationResult(
                matched = true,
                actionType = RuleActionType.APPLY_DISCOUNT,
                actionPayload = """{"discount": 5.0}""",
                mutatedContext = mapOf("appliedDiscount" to 5.0),
                durationMs = 1L,
            )

        val response = service.simulateRule(tenantId, request)
        assertTrue(response.matched)
        assertEquals(RuleActionType.APPLY_DISCOUNT, response.actionType)
        assertEquals(5.0, response.mutatedContext?.get("appliedDiscount"))
    }

    @Test
    fun `should simulate rule dry-run capturing simulated violation error`() {
        val ruleId = UUID.randomUUID()
        val rule =
            CustomRule(
                id = ruleId,
                tenantId = tenantId,
                name = "Block Sale",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        every { ruleRepository.findByTenantIdAndId(tenantId, ruleId) } returns Optional.of(rule)
        every { evaluationEngine.evaluate(rule, any()) } throws BusinessRuleViolationException("Block Sale", "Sale blocked")

        val response = service.simulateRule(tenantId, RuleSimulationRequestDto(ruleId = ruleId, context = emptyMap()))
        assertTrue(response.matched)
        assertEquals("Sale blocked", response.simulatedError)
    }

    @Test
    fun `should execute rules in priority order and abort on strict violation`() {
        val rule1 =
            CustomRule(
                tenantId = tenantId,
                name = "Strict Block",
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                priority = 10,
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
            )

        every {
            ruleRepository.findAllByTenantIdAndTriggerEventAndIsActiveOrderByPriorityAsc(tenantId, "sale.finalized", true)
        } returns listOf(rule1)

        every { evaluationEngine.evaluate(rule1, any()) } throws BusinessRuleViolationException("Strict Block", "Blocked")

        val ex =
            assertThrows<BusinessRuleViolationException> {
                service.executeRules(tenantId, "sale.finalized", RuleTiming.BEFORE, mapOf("total" to 100))
            }
        assertEquals("Blocked", ex.message)
    }

    @Test
    fun `should execute rules and continue on permissive violation policy`() {
        val rulePermissive =
            CustomRule(
                tenantId = tenantId,
                name = "Permissive Notification",
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                priority = 10,
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                errorHandlingPolicy = ErrorHandlingPolicy.LOG_AND_CONTINUE,
            )

        every {
            ruleRepository.findAllByTenantIdAndTriggerEventAndIsActiveOrderByPriorityAsc(tenantId, "sale.finalized", true)
        } returns listOf(rulePermissive)

        every { evaluationEngine.evaluate(rulePermissive, any()) } throws BusinessRuleViolationException("Permissive", "Error")

        val result = service.executeRules(tenantId, "sale.finalized", RuleTiming.BEFORE, mapOf("total" to 100))
        assertEquals(100, result["total"])
    }

    @Test
    fun `should handle evaluation failure with ABORT_TRANSACTION throwing RuleEvaluationException`() {
        val faultyRule =
            CustomRule(
                tenantId = tenantId,
                name = "Faulty Rule",
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                conditionExpression = "invalid syntax",
                actionType = RuleActionType.BLOCK_OPERATION,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
            )

        every {
            ruleRepository.findAllByTenantIdAndTriggerEventAndIsActiveOrderByPriorityAsc(tenantId, "sale.finalized", true)
        } returns listOf(faultyRule)

        every { evaluationEngine.evaluate(faultyRule, any()) } returns
            EvaluationResult(
                matched = false,
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = null,
                mutatedContext = null,
                durationMs = 1L,
                errorMessage = "Syntax error",
            )

        assertThrows<RuleEvaluationException> {
            service.executeRules(tenantId, "sale.finalized", RuleTiming.BEFORE, emptyMap())
        }
    }

    @Test
    fun `should list execution traces with pagination`() {
        val pageable = PageRequest.of(0, 10)
        val trace =
            RuleExecutionTrace(
                tenantId = tenantId,
                ruleId = UUID.randomUUID(),
                triggerEvent = "sale.finalized",
                entityType = "SALE",
                entityId = "123",
                durationMs = 2L,
                result = TraceResult.MATCHED,
            )

        every { traceRepository.findAllByTenantId(tenantId, pageable) } returns PageImpl(listOf(trace), pageable, 1)

        val result = service.listTraces(tenantId, null, pageable)
        assertEquals(1, result.totalElements)
        assertEquals(TraceResult.MATCHED, result.content.first().result)
    }
}
