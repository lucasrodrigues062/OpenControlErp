package org.opencontrol.erp.inbound.domain

import org.springframework.expression.ExpressionParser
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

@Component
class CostFormulaEngine {
    private val parser: ExpressionParser = SpelExpressionParser()

    /**
     * Evaluates an Excel-like cost formula expression with provided variables and custom user variables.
     * E.g.: "= (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA_PCT)) + (FREIGHT_ALLOCATED / QUANTITY) - ICMS_CREDIT"
     */
    fun evaluate(
        formula: String,
        variables: Map<String, BigDecimal>,
    ): BigDecimal {
        val sanitized = sanitizeFormula(formula)
        if (sanitized.isBlank()) return BigDecimal.ZERO

        val upperVariables = variables.mapKeys { it.key.trim().uppercase(Locale.ROOT) }

        // Standardize Excel-like functions to SpEL syntax
        var spelExpression = sanitized
            .replace(Regex("(?i)\\bIF\\s*\\(([^,]+),([^,]+),([^\\)]+)\\)")) { m ->
                "(${m.groupValues[1]} ? ${m.groupValues[2]} : ${m.groupValues[3]})"
            }
            .replace(Regex("(?i)\\bROUND\\s*\\(([^,]+),([^\\)]+)\\)")) { m ->
                "#round(${m.groupValues[1]}, ${m.groupValues[2]})"
            }
            .replace(Regex("(?i)\\bMAX\\s*\\(([^,]+),([^\\)]+)\\)")) { m ->
                "#max(${m.groupValues[1]}, ${m.groupValues[2]})"
            }
            .replace(Regex("(?i)\\bMIN\\s*\\(([^,]+),([^\\)]+)\\)")) { m ->
                "#min(${m.groupValues[1]}, ${m.groupValues[2]})"
            }
            .replace(Regex("(?i)\\bCOALESCE\\s*\\(([^,]+),([^\\)]+)\\)")) { m ->
                "(#val(${m.groupValues[1]}) != null ? #val(${m.groupValues[1]}) : ${m.groupValues[2]})"
            }

        // Replace variable tokens with `#var` references
        val variableTokenRegex = Regex("\\b([A-Z_][A-Z0-9_]*)\\b")
        val processedExpression = variableTokenRegex.replace(spelExpression) { match ->
            val token = match.groupValues[1].uppercase(Locale.ROOT)
            if (token in RESERVED_KEYWORDS) {
                token
            } else {
                "#$token"
            }
        }

        // Build sandboxed evaluation context (NO reflection, strictly registered math functions and variables)
        val context = SimpleEvaluationContext.forReadOnlyDataBinding()
            .withMethodResolvers()
            .build()

        // Register custom math helper functions
        try {
            context.setVariable("round", MathFunctions::class.java.getDeclaredMethod("round", Double::class.java, Int::class.java))
            context.setVariable("max", MathFunctions::class.java.getDeclaredMethod("max", Double::class.java, Double::class.java))
            context.setVariable("min", MathFunctions::class.java.getDeclaredMethod("min", Double::class.java, Double::class.java))
        } catch (_: Exception) {
            // Ignore if already registered
        }

        // Populate variable values as Double for SpEL arithmetic, defaulting to 0.0
        upperVariables.forEach { (name, value) ->
            context.setVariable(name, value.toDouble())
        }

        // Find any unpopulated variables referenced in expression and set them to 0.0 to avoid SpEL null evaluation errors
        variableTokenRegex.findAll(spelExpression).forEach { match ->
            val varName = match.groupValues[1].uppercase(Locale.ROOT)
            if (varName !in RESERVED_KEYWORDS && !upperVariables.containsKey(varName)) {
                context.setVariable(varName, 0.0)
            }
        }

        return try {
            val expr = parser.parseExpression(processedExpression)
            val result = expr.getValue(context)
            when (result) {
                is Number -> BigDecimal(result.toDouble()).setScale(4, RoundingMode.HALF_UP)
                is Boolean -> if (result) BigDecimal.ONE else BigDecimal.ZERO
                else -> BigDecimal.ZERO
            }
        } catch (ex: Exception) {
            throw IllegalArgumentException("Erro ao avaliar fórmula de custo '$formula': ${ex.message}", ex)
        }
    }

    private fun sanitizeFormula(formula: String): String {
        var clean = formula.trim()
        if (clean.startsWith("=")) {
            clean = clean.substring(1).trim()
        }
        return clean
    }

    companion object {
        private val RESERVED_KEYWORDS = setOf(
            "AND", "OR", "NOT", "TRUE", "FALSE", "NULL", "ROUND", "IF", "MAX", "MIN", "COALESCE", "T"
        )
    }

    object MathFunctions {
        @JvmStatic
        fun round(value: Double, decimals: Int): Double {
            return BigDecimal(value).setScale(decimals, RoundingMode.HALF_UP).toDouble()
        }

        @JvmStatic
        fun max(a: Double, b: Double): Double = kotlin.math.max(a, b)

        @JvmStatic
        fun min(a: Double, b: Double): Double = kotlin.math.min(a, b)
    }
}
