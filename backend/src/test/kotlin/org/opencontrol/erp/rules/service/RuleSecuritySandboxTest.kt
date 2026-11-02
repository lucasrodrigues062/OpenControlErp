package org.opencontrol.erp.rules.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.exception.RuleSecurityException
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuleSecuritySandboxTest {
    private val engine = RuleEvaluationEngine()
    private val tenantId = UUID.randomUUID()

    @Test
    fun `should block expression attempting to access Runtime or ProcessBuilder`() {
        val dangerousExpressions =
            listOf(
                "T(java.lang.Runtime).getRuntime().exec('calc')",
                "T(java.lang.ProcessBuilder).start()",
                "T(System).exit(0)",
                "T(ClassLoader).getSystemClassLoader()",
                "T(java.lang.Class).forName('java.lang.System')",
                "#entity.getClass().getMethod('exit').invoke()",
            )

        for (expr in dangerousExpressions) {
            val rule =
                CustomRule(
                    tenantId = tenantId,
                    name = "Malicious Rule",
                    triggerEvent = "sale.finalized",
                    authoringMode = AuthoringMode.ADVANCED_DSL,
                    conditionExpression = expr,
                    actionType = RuleActionType.BLOCK_OPERATION,
                )

            assertThrows<RuleSecurityException>("Expression should be blocked: $expr") {
                engine.evaluate(rule, mapOf("total" to 100))
            }
        }
    }

    @Test
    fun `should safely evaluate clean arithmetic and boolean formulas`() {
        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Safe Rule",
                triggerEvent = "sale.item_added",
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = "#item.price * #item.quantity > 500 and #item.category == 'ELECTRONICS'",
                actionType = RuleActionType.CUSTOM_ACTION,
            )

        val context =
            mapOf(
                "item" to
                    mapOf(
                        "price" to 150.0,
                        "quantity" to 4,
                        "category" to "ELECTRONICS",
                    ),
            )

        val result = engine.evaluate(rule, context)
        assertTrue(result.matched)
    }

    @Test
    fun `should return false when condition does not match`() {
        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "Discount Check",
                triggerEvent = "sale.item_added",
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = "#item.discount > 20.0",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        val context =
            mapOf(
                "item" to mapOf("discount" to 10.0),
            )

        val result = engine.evaluate(rule, context)
        assertFalse(result.matched)
    }
}
