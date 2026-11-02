package org.opencontrol.erp.rules.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.exception.BusinessRuleViolationException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuleEvaluationEngineTest {
    private val engine = RuleEvaluationEngine()
    private val tenantId = UUID.randomUUID()

    @Test
    fun `should evaluate Visual Builder single condition with GREATER_THAN`() {
        val visualJson =
            """
            {
                "field": "sale.totalAmount",
                "operator": "GREATER_THAN",
                "value": 1000.0
            }
            """.trimIndent()

        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Wholesale Total Limit",
                triggerEvent = "sale.finalized",
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = visualJson,
                actionType = RuleActionType.APPLY_DISCOUNT,
                actionPayload = """{"discount": 10.0}""",
            )

        val context = mapOf("sale" to mapOf("totalAmount" to 1500.0))
        val result = engine.evaluate(rule, context)

        assertTrue(result.matched)
        assertEquals(RuleActionType.APPLY_DISCOUNT, result.actionType)
        assertEquals(10.0, result.mutatedContext?.get("appliedDiscount"))
    }

    @Test
    fun `should evaluate Visual Builder group conditions with AND conjunction`() {
        val visualJson =
            """
            {
                "conjunction": "AND",
                "conditions": [
                    { "field": "discountPercentage", "operator": "GREATER_THAN", "value": 15.0 },
                    { "field": "isApproved", "operator": "EQUALS", "value": false }
                ]
            }
            """.trimIndent()

        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Unapproved High Discount",
                triggerEvent = "sale.item_added",
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = visualJson,
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = """{"message": "High discount requires manager approval"}""",
            )

        val matchingContext = mapOf("discountPercentage" to 20.0, "isApproved" to false)
        val ex =
            assertThrows<BusinessRuleViolationException> {
                engine.evaluate(rule, matchingContext)
            }
        assertEquals("High discount requires manager approval", ex.message)

        val nonMatchingContext = mapOf("discountPercentage" to 20.0, "isApproved" to true)
        val nonMatchingResult = engine.evaluate(rule, nonMatchingContext)
        assertFalse(nonMatchingResult.matched)
    }

    @Test
    fun `should evaluate Visual Builder group conditions with OR conjunction`() {
        val visualJson =
            """
            {
                "conjunction": "OR",
                "conditions": [
                    { "field": "customerTier", "operator": "EQUALS", "value": "VIP" },
                    { "field": "totalAmount", "operator": "GREATER_OR_EQUAL", "value": 2000.0 }
                ]
            }
            """.trimIndent()

        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "VIP or Bulk Qualification",
                triggerEvent = "sale.created",
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = visualJson,
                actionType = RuleActionType.SET_FIELD_VALUE,
                actionPayload = """{"field": "shippingMethod", "value": "EXPRESS"}""",
            )

        val context = mapOf("customerTier" to "STANDARD", "totalAmount" to 2500.0)
        val result = engine.evaluate(rule, context)

        assertTrue(result.matched)
        assertEquals("EXPRESS", result.mutatedContext?.get("shippingMethod"))
    }

    @Test
    fun `should evaluate Advanced DSL with string operators and nested map navigation`() {
        val dslExpression = "#customer['city'] == 'São Paulo' and #total >= 50.0"

        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Free Local Shipping",
                triggerEvent = "sale.finalized",
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = dslExpression,
                actionType = RuleActionType.DISPATCH_NOTIFICATION,
            )

        val context =
            mapOf(
                "customer" to mapOf("city" to "São Paulo"),
                "total" to 75.0,
            )

        val result = engine.evaluate(rule, context)
        assertTrue(result.matched)
        assertEquals(true, result.mutatedContext?.get("notificationDispatched"))
    }

    @Test
    fun `should handle operators CONTAINS and IN in Visual Builder`() {
        val containsJson =
            """
            { "field": "description", "operator": "CONTAINS", "value": "PROMO" }
            """.trimIndent()

        val ruleContains =
            CustomRule(
                tenantId = tenantId,
                name = "Promo Check",
                triggerEvent = "product.created",
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = containsJson,
                actionType = RuleActionType.SET_FIELD_VALUE,
                actionPayload = """{"field": "tag", "value": "PROMOTIONAL"}""",
            )

        val contextContains = mapOf("description" to "WINTER PROMO PACK")
        val result = engine.evaluate(ruleContains, contextContains)
        assertTrue(result.matched)
        assertEquals("PROMOTIONAL", result.mutatedContext?.get("tag"))

        val inJson =
            """
            { "field": "status", "operator": "IN", "value": ["ACTIVE", "APPROVED"] }
            """.trimIndent()

        val ruleIn =
            CustomRule(
                tenantId = tenantId,
                name = "Status In",
                triggerEvent = "sale.finalized",
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = inJson,
                actionType = RuleActionType.CUSTOM_ACTION,
            )

        val contextIn = mapOf("status" to "APPROVED")
        val resultIn = engine.evaluate(ruleIn, contextIn)
        assertTrue(resultIn.matched)
    }

    @Test
    fun `should handle evaluation error safely and return error message`() {
        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Faulty Expression",
                triggerEvent = "sale.finalized",
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = "1 / #zeroValue > 0",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        val context = mapOf("zeroValue" to 0)
        val result = engine.evaluate(rule, context)

        assertFalse(result.matched)
        assertNotNull(result.errorMessage)
    }
}
