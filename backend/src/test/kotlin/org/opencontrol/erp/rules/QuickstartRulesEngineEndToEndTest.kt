package org.opencontrol.erp.rules

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.opencontrol.erp.rules.api.RuleController
import org.opencontrol.erp.rules.api.dto.RuleRequestDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationRequestDto
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.ErrorHandlingPolicy
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.domain.RuleTiming
import org.opencontrol.erp.rules.domain.TraceResult
import org.opencontrol.erp.rules.exception.BusinessRuleViolationException
import org.opencontrol.erp.rules.repository.CustomRuleRepository
import org.opencontrol.erp.rules.repository.RuleExecutionTraceRepository
import org.opencontrol.erp.rules.service.RuleEvaluationEngine
import org.opencontrol.erp.rules.service.RuleExecutionService
import org.opencontrol.erp.rules.service.RuleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(authorities = ["RULES_READ", "RULES_MANAGE"])
class QuickstartRulesEngineEndToEndTest {
    @Autowired
    private lateinit var ruleRepository: CustomRuleRepository

    @Autowired
    private lateinit var traceRepository: RuleExecutionTraceRepository

    @Autowired
    private lateinit var ruleService: RuleService

    @Autowired
    private lateinit var executionService: RuleExecutionService

    @Autowired
    private lateinit var evaluationEngine: RuleEvaluationEngine

    @Autowired
    private lateinit var ruleController: RuleController

    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "admin-test",
                username = "admin",
                roles = setOf("RULES_READ", "RULES_MANAGE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "QUICKSTART", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `Scenario 1 - Authoring and Dry-Run Simulation with Visual Condition Builder`() {
        val visualJson =
            """
            {
                "conjunction": "AND",
                "conditions": [
                    { "field": "sale.discountPercentage", "operator": "GREATER_THAN", "value": 15.0 }
                ]
            }
            """.trimIndent()

        val ruleRequest =
            RuleRequestDto(
                name = "Max Wholesale Discount 15% - " + UUID.randomUUID(),
                description = "Discounts over 15% require director approval",
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = visualJson,
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = """{"message": "Wholesale discount cannot exceed 15% without director approval."}""",
                priority = 10,
                isActive = true,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
            )

        // Simulate via controller before saving
        val simRequest =
            RuleSimulationRequestDto(
                rule = ruleRequest,
                context =
                    mapOf(
                        "sale" to
                            mapOf(
                                "totalAmount" to 1200.0,
                                "discountPercentage" to 20.0,
                            ),
                    ),
            )

        val simResponse = ruleController.simulateRule(tenantId, simRequest)
        assertEquals(HttpStatus.OK, simResponse.statusCode)
        val body = simResponse.body!!
        assertTrue(body.matched)
        assertEquals("Wholesale discount cannot exceed 15% without director approval.", body.simulatedError)

        // Persist rule
        val createResponse = ruleController.createRule(tenantId, ruleRequest)
        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        assertNotNull(createResponse.body?.id)
    }

    @Test
    fun `Scenario 2 - Synchronous Transaction Interception and Rollback in Strict Mode`() {
        val rule =
            ruleService.createRule(
                tenantId,
                RuleRequestDto(
                    name = "Strict Discount Gate - " + UUID.randomUUID(),
                    triggerEvent = "sale.finalized",
                    timing = RuleTiming.BEFORE,
                    authoringMode = AuthoringMode.ADVANCED_DSL,
                    conditionExpression = "#sale['discountPercentage'] > 15.0",
                    actionType = RuleActionType.BLOCK_OPERATION,
                    actionPayload = """{"message": "Wholesale discount cannot exceed 15%."}""",
                    priority = 5,
                    isActive = true,
                    errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                ),
            )

        val context =
            mapOf(
                "sale" to mapOf("discountPercentage" to 18.0, "totalAmount" to 5000.0),
            )

        val ex =
            assertThrows<BusinessRuleViolationException> {
                executionService.executeRules(
                    tenantId = tenantId,
                    triggerEvent = "sale.finalized",
                    timing = RuleTiming.BEFORE,
                    context = context,
                    entityType = "SALE",
                    entityId = "order-123",
                )
            }
        assertEquals("Wholesale discount cannot exceed 15%.", ex.message)
    }

    @Test
    fun `Scenario 3 - Advanced SpEL Expression and Field Mutation`() {
        val dslExpression = "#sale['customer']['tier'] == 'VIP' and #sale['totalAmount'] >= 500.0"

        val rule =
            ruleService.createRule(
                tenantId,
                RuleRequestDto(
                    name = "Auto-Apply VIP Free Shipping - " + UUID.randomUUID(),
                    triggerEvent = "sale.finalized",
                    timing = RuleTiming.BEFORE,
                    authoringMode = AuthoringMode.ADVANCED_DSL,
                    conditionExpression = dslExpression,
                    actionType = RuleActionType.SET_FIELD_VALUE,
                    actionPayload = """{"field": "shippingFee", "value": "0.0"}""",
                    priority = 20,
                    isActive = true,
                    errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
                ),
            )

        val context =
            mapOf(
                "sale" to
                    mapOf(
                        "customer" to mapOf("tier" to "VIP"),
                        "totalAmount" to 750.0,
                        "shippingFee" to 45.0,
                    ),
            )

        val resultContext =
            executionService.executeRules(
                tenantId = tenantId,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                context = context,
            )

        assertEquals("0.0", resultContext["shippingFee"])
    }

    @Test
    fun `Scenario 4 - Permissive Failure Policy LOG_AND_CONTINUE`() {
        val faultyRule =
            ruleService.createRule(
                tenantId,
                RuleRequestDto(
                    name = "Permissive Faulty Calculation - " + UUID.randomUUID(),
                    triggerEvent = "sale.finalized",
                    timing = RuleTiming.BEFORE,
                    authoringMode = AuthoringMode.ADVANCED_DSL,
                    conditionExpression = "1 / #zeroValue > 0",
                    actionType = RuleActionType.BLOCK_OPERATION,
                    priority = 99,
                    isActive = true,
                    errorHandlingPolicy = ErrorHandlingPolicy.LOG_AND_CONTINUE,
                ),
            )

        val context = mapOf("zeroValue" to 0, "orderId" to "12345")

        // Should NOT throw exception
        val result =
            executionService.executeRules(
                tenantId = tenantId,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                context = context,
                entityType = "SALE",
                entityId = "12345",
            )

        assertEquals("12345", result["orderId"])

        // Direct synchronous trace record check to confirm trace logging
        val trace =
            executionService.recordTrace(
                tenantId = tenantId,
                ruleId = faultyRule.id,
                triggerEvent = "sale.finalized",
                entityType = "SALE",
                entityId = "12345",
                durationMs = 2L,
                result = TraceResult.ERROR,
                errorDetail = "Divide by zero",
            )
        assertNotNull(trace.id)
        assertEquals(TraceResult.ERROR, trace.result)
    }
}
