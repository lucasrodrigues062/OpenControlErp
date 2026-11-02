package org.opencontrol.erp.rules

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.service.RuleEvaluationEngine
import java.util.UUID

class CustomFieldRuleIntegrationTest {
    private val engine = RuleEvaluationEngine()
    private val tenantId = UUID.randomUUID()

    @Test
    fun `should evaluate rule matching custom field value in context`() {
        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Vip Customer Spouse Notification",
                triggerEvent = "sale.created",
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression =
                    "#customer['customFields']['spouse_name'] != null and " +
                        "#customer['customFields']['spouse_name'] == 'Mary Jane'",
                actionType = RuleActionType.DISPATCH_NOTIFICATION,
            )

        val matchingContext =
            mapOf(
                "customer" to
                    mapOf(
                        "name" to "John Doe",
                        "customFields" to
                            mapOf(
                                "spouse_name" to "Mary Jane",
                                "tier" to "GOLD",
                            ),
                    ),
            )

        val res = engine.evaluate(rule, matchingContext)
        assertTrue(res.matched)
    }

    @Test
    fun `should not match when custom field value differs`() {
        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Vip Customer Spouse Notification",
                triggerEvent = "sale.created",
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = "#customer['customFields']['spouse_name'] == 'Mary Jane'",
                actionType = RuleActionType.DISPATCH_NOTIFICATION,
            )

        val nonMatchingContext =
            mapOf(
                "customer" to
                    mapOf(
                        "name" to "John Doe",
                        "customFields" to
                            mapOf(
                                "spouse_name" to "Jane Smith",
                            ),
                    ),
            )

        val res = engine.evaluate(rule, nonMatchingContext)
        assertFalse(res.matched)
    }

    @Test
    fun `should evaluate visual builder condition checking custom field`() {
        val visualJson =
            """
            {
              "conjunction": "AND",
              "conditions": [
                {
                  "field": "customer.customFields['spouse_name']",
                  "operator": "EQUALS",
                  "value": "Mary Jane"
                }
              ]
            }
            """.trimIndent()

        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Visual Custom Field Rule",
                triggerEvent = "customer.created",
                authoringMode = AuthoringMode.VISUAL_BUILDER,
                conditionExpression = visualJson,
                actionType = RuleActionType.DISPATCH_NOTIFICATION,
            )

        val context =
            mapOf(
                "customer" to
                    mapOf(
                        "customFields" to
                            mapOf(
                                "spouse_name" to "Mary Jane",
                            ),
                    ),
            )

        val res = engine.evaluate(rule, context)
        assertTrue(res.matched)
    }
}
