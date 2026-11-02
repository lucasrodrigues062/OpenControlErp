package org.opencontrol.erp.rules.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.opencontrol.erp.rules.api.dto.RuleRequestDto
import org.opencontrol.erp.rules.api.dto.RuleResponseDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationRequestDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationResponseDto
import org.opencontrol.erp.rules.api.dto.RuleSummaryDto
import org.opencontrol.erp.rules.api.dto.RuleTraceResponseDto
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.ErrorHandlingPolicy
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.domain.RuleTiming
import org.opencontrol.erp.rules.domain.TraceResult
import org.opencontrol.erp.rules.service.RuleExecutionService
import org.opencontrol.erp.rules.service.RuleService
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class RuleControllerTest {
    private val ruleService: RuleService = mockk(relaxed = true)
    private val executionService: RuleExecutionService = mockk(relaxed = true)

    private lateinit var controller: RuleController
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        controller = RuleController(ruleService, executionService)
        SecurityContextHolder.setContext(
            UserContext(
                userId = "test-user",
                username = "admin",
                roles = setOf("RULES_READ", "RULES_MANAGE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `should list rules with 200 OK`() {
        val summary =
            RuleSummaryDto(
                id = UUID.randomUUID(),
                name = "Test Rule",
                description = "Desc",
                triggerEvent = "sale.item_added",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.ADVANCED_DSL,
                actionType = RuleActionType.BLOCK_OPERATION,
                priority = 10,
                isActive = true,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                createdAt = Instant.now(),
            )

        every { ruleService.listRules(tenantId, any()) } returns PageImpl(listOf(summary))

        val response = controller.listRules(null, 0, 20)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.totalElements)
    }

    @Test
    fun `should create rule with 201 Created`() {
        val request =
            RuleRequestDto(
                name = "New Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        val created =
            RuleResponseDto(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                name = "New Rule",
                description = null,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = null,
                priority = 100,
                isActive = true,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { ruleService.createRule(tenantId, request) } returns created

        val response = controller.createRule(null, request)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("New Rule", response.body?.name)
    }

    @Test
    fun `should get rule by id with 200 OK`() {
        val ruleId = UUID.randomUUID()
        val dto =
            RuleResponseDto(
                id = ruleId,
                tenantId = tenantId,
                name = "Rule",
                description = null,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = null,
                priority = 100,
                isActive = true,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { ruleService.getRule(tenantId, ruleId) } returns dto

        val response = controller.getRule(null, ruleId)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(ruleId, response.body?.id)
    }

    @Test
    fun `should update rule with 200 OK`() {
        val ruleId = UUID.randomUUID()
        val request =
            RuleRequestDto(
                name = "Updated Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        val updated =
            RuleResponseDto(
                id = ruleId,
                tenantId = tenantId,
                name = "Updated Rule",
                description = null,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = null,
                priority = 100,
                isActive = true,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { ruleService.updateRule(tenantId, ruleId, request) } returns updated

        val response = controller.updateRule(null, ruleId, request)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Updated Rule", response.body?.name)
    }

    @Test
    fun `should delete rule with 204 No Content`() {
        val ruleId = UUID.randomUUID()

        val response = controller.deleteRule(null, ruleId)
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify { ruleService.deleteRule(tenantId, ruleId) }
    }

    @Test
    fun `should toggle rule active status with 200 OK`() {
        val ruleId = UUID.randomUUID()
        val toggled =
            RuleResponseDto(
                id = ruleId,
                tenantId = tenantId,
                name = "Rule",
                description = null,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = null,
                priority = 100,
                isActive = false,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { ruleService.toggleRule(tenantId, ruleId) } returns toggled

        val response = controller.toggleRule(null, ruleId)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(false, response.body?.isActive)
    }

    @Test
    fun `should simulate rule dry-run with 200 OK`() {
        val simRequest = RuleSimulationRequestDto(context = mapOf("total" to 100))
        val simResponse =
            RuleSimulationResponseDto(
                matched = true,
                actionType = RuleActionType.BLOCK_OPERATION,
                simulatedError = "Blocked",
                durationMs = 2L,
            )

        every { executionService.simulateRule(tenantId, simRequest) } returns simResponse

        val response = controller.simulateRule(null, simRequest)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(true, response.body?.matched)
    }

    @Test
    fun `should list traces with 200 OK`() {
        val trace =
            RuleTraceResponseDto(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                ruleId = UUID.randomUUID(),
                triggerEvent = "sale.finalized",
                entityType = "SALE",
                entityId = "100",
                durationMs = 3L,
                result = TraceResult.MATCHED,
                errorDetail = null,
                createdAt = Instant.now(),
            )

        every { executionService.listTraces(tenantId, null, any()) } returns PageImpl(listOf(trace))

        val response = controller.listTraces(null, null, 0, 20)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.totalElements)
    }
}
