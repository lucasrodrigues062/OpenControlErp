package org.opencontrol.erp.rules.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.exception.BusinessRuleViolationException
import org.opencontrol.erp.rules.exception.RuleEvaluationException
import org.opencontrol.erp.rules.exception.RuleSecurityException
import org.slf4j.LoggerFactory
import org.springframework.context.expression.MapAccessor
import org.springframework.expression.Expression
import org.springframework.expression.spel.SpelCompilerMode
import org.springframework.expression.spel.SpelParserConfiguration
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.DataBindingPropertyAccessor
import org.springframework.expression.spel.support.SimpleEvaluationContext
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

data class EvaluationResult(
    val matched: Boolean,
    val actionType: RuleActionType,
    val actionPayload: String?,
    val mutatedContext: Map<String, Any?>?,
    val durationMs: Long,
    val errorMessage: String? = null,
)

@Service
class RuleEvaluationEngine {
    private val logger = LoggerFactory.getLogger(RuleEvaluationEngine::class.java)
    private val objectMapper = jacksonObjectMapper()

    private val parserConfig = SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, this.javaClass.classLoader)
    private val parser = SpelExpressionParser(parserConfig)
    private val expressionCache = ConcurrentHashMap<String, Expression>()

    private val forbiddenTokens =
        setOf(
            "T(",
            "java.lang",
            "java.io",
            "java.net",
            "java.nio",
            "Runtime",
            "ProcessBuilder",
            "System",
            "ClassLoader",
            "Class.",
            "reflect",
            "exec",
            "exit",
            "getClass",
            "getMethod",
            "invoke",
        )

    fun evaluate(
        rule: CustomRule,
        context: Map<String, Any?>,
    ): EvaluationResult {
        val startTime = System.currentTimeMillis()
        try {
            val spelExpression = compileToSpel(rule.authoringMode, rule.conditionExpression)
            validateSecurity(spelExpression)

            val parsedExp =
                expressionCache.computeIfAbsent(spelExpression) {
                    parser.parseExpression(it)
                }

            // SimpleEvaluationContext prevents reflection or accessing arbitrary Java types
            val evaluationContext =
                SimpleEvaluationContext
                    .forPropertyAccessors(MapAccessor(), DataBindingPropertyAccessor.forReadOnlyAccess())
                    .build()

            context.forEach { (key, value) ->
                evaluationContext.setVariable(key, value)
            }

            val rawResult = parsedExp.getValue(evaluationContext, context)
            val matched = (rawResult as? Boolean) ?: false
            val duration = System.currentTimeMillis() - startTime

            var mutated: MutableMap<String, Any?>? = null
            if (matched) {
                mutated = executeAction(rule, context)
            }

            return EvaluationResult(
                matched = matched,
                actionType = rule.actionType,
                actionPayload = rule.actionPayload,
                mutatedContext = mutated,
                durationMs = duration,
            )
        } catch (ex: RuleSecurityException) {
            throw ex
        } catch (ex: BusinessRuleViolationException) {
            throw ex
        } catch (ex: Exception) {
            val duration = System.currentTimeMillis() - startTime
            logger.warn("Rule '${rule.name}' evaluation error: {}", ex.message)
            return EvaluationResult(
                matched = false,
                actionType = rule.actionType,
                actionPayload = rule.actionPayload,
                mutatedContext = null,
                durationMs = duration,
                errorMessage = ex.message ?: "Unknown evaluation error",
            )
        }
    }

    fun compileToSpel(
        authoringMode: AuthoringMode,
        expression: String,
    ): String =
        when (authoringMode) {
            AuthoringMode.ADVANCED_DSL -> expression.trim()
            AuthoringMode.VISUAL_BUILDER -> convertVisualJsonToSpel(expression.trim())
        }

    private fun convertVisualJsonToSpel(jsonStr: String): String {
        if (!jsonStr.startsWith("{") && !jsonStr.startsWith("[")) {
            return jsonStr
        }

        try {
            val root = objectMapper.readTree(jsonStr)
            if (root.has("conditions")) {
                val conjunction = root.path("conjunction").asText("AND").uppercase()
                val conditionsNode = root.path("conditions")
                if (conditionsNode.isArray && conditionsNode.size() > 0) {
                    val clauses = mutableListOf<String>()
                    for (node in conditionsNode) {
                        clauses.add(renderSingleCondition(node))
                    }
                    val joiner = if (conjunction == "OR") " or " else " and "
                    return clauses.joinToString(separator = joiner, prefix = "(", postfix = ")")
                }
            } else if (root.has("field") && root.has("operator")) {
                return renderSingleCondition(root)
            }
        } catch (e: Exception) {
            throw RuleEvaluationException("Failed to parse Visual Builder JSON: ${e.message}", cause = e)
        }

        return "true"
    }

    private fun renderSingleCondition(node: JsonNode): String {
        val field = node.path("field").asText()
        val operator = node.path("operator").asText()
        val valueNode = node.path("value")

        val targetPath = if (field.startsWith("#")) field else "#$field"

        return when (operator.uppercase()) {
            "EQUALS" -> "$targetPath == ${formatValue(valueNode)}"
            "NOT_EQUALS" -> "$targetPath != ${formatValue(valueNode)}"
            "GREATER_THAN" -> "$targetPath > ${formatValue(valueNode)}"
            "LESS_THAN" -> "$targetPath < ${formatValue(valueNode)}"
            "GREATER_OR_EQUAL" -> "$targetPath >= ${formatValue(valueNode)}"
            "LESS_OR_EQUAL" -> "$targetPath <= ${formatValue(valueNode)}"
            "CONTAINS" -> "($targetPath != null and $targetPath matches '(?i).*' + ${formatValue(valueNode)} + '.*')"
            "IN" -> {
                if (valueNode.isArray && valueNode.size() > 0) {
                    val orClauses = valueNode.map { "$targetPath == ${formatValue(it)}" }
                    orClauses.joinToString(separator = " or ", prefix = "(", postfix = ")")
                } else {
                    "$targetPath == ${formatValue(valueNode)}"
                }
            }
            else -> "$targetPath == ${formatValue(valueNode)}"
        }
    }

    private fun formatValue(valueNode: JsonNode): String =
        when {
            valueNode.isNumber -> valueNode.asText()
            valueNode.isBoolean -> valueNode.asBoolean().toString()
            else -> "'${valueNode.asText().replace("'", "\\'")}'"
        }

    fun validateSecurity(expression: String) {
        for (token in forbiddenTokens) {
            if (expression.contains(token, ignoreCase = true)) {
                throw RuleSecurityException("Forbidden security token detected in expression: '$token'")
            }
        }
    }

    private fun executeAction(
        rule: CustomRule,
        context: Map<String, Any?>,
    ): MutableMap<String, Any?>? {
        when (rule.actionType) {
            RuleActionType.BLOCK_OPERATION -> {
                val message =
                    extractMessage(rule.actionPayload)
                        ?: "Operation blocked by business rule '${rule.name}'"
                throw BusinessRuleViolationException(
                    ruleName = rule.name,
                    message = message,
                    ruleId = rule.id,
                )
            }
            RuleActionType.SET_FIELD_VALUE -> {
                val mutableContext = HashMap(context)
                try {
                    if (!rule.actionPayload.isNullOrBlank()) {
                        val payloadNode = objectMapper.readTree(rule.actionPayload)
                        val field = payloadNode.path("field").asText()
                        val value = payloadNode.path("value")
                        if (field.isNotBlank()) {
                            mutableContext[field] = value.asText()
                        }
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to apply SET_FIELD_VALUE action payload: {}", e.message)
                }
                return mutableContext
            }
            RuleActionType.APPLY_DISCOUNT -> {
                val mutableContext = HashMap(context)
                try {
                    if (!rule.actionPayload.isNullOrBlank()) {
                        val payloadNode = objectMapper.readTree(rule.actionPayload)
                        val discount = payloadNode.path("discount").asDouble(0.0)
                        mutableContext["appliedDiscount"] = discount
                    }
                } catch (e: Exception) {
                    logger.warn("Failed to apply APPLY_DISCOUNT action payload: {}", e.message)
                }
                return mutableContext
            }
            RuleActionType.DISPATCH_NOTIFICATION,
            RuleActionType.CUSTOM_ACTION,
            -> {
                val mutableContext = HashMap(context)
                mutableContext["notificationDispatched"] = true
                return mutableContext
            }
        }
    }

    private fun extractMessage(payload: String?): String? {
        if (payload.isNullOrBlank()) return null
        return try {
            if (payload.trim().startsWith("{")) {
                val tree = objectMapper.readTree(payload)
                tree.path("message").asText(payload)
            } else {
                payload
            }
        } catch (_: Exception) {
            payload
        }
    }
}
