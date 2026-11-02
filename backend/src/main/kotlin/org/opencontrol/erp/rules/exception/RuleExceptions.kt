package org.opencontrol.erp.rules.exception

import org.opencontrol.erp.kernel.exception.OpenControlException
import org.springframework.http.HttpStatus
import java.util.UUID

open class BusinessRuleViolationException(
    val ruleName: String,
    override val message: String,
    val ruleId: UUID? = null,
) : OpenControlException(
        message = message,
        errorCode = "BUSINESS_RULE_VIOLATION",
        status = HttpStatus.UNPROCESSABLE_ENTITY,
    )

open class RuleEvaluationException(
    override val message: String,
    val ruleId: UUID? = null,
    val ruleName: String? = null,
    override val cause: Throwable? = null,
) : OpenControlException(
        message = message,
        errorCode = "RULE_EVALUATION_ERROR",
        status = HttpStatus.BAD_REQUEST,
        cause = cause,
    )

open class RuleSecurityException(
    override val message: String,
) : OpenControlException(
        message = message,
        errorCode = "RULE_SECURITY_VIOLATION",
        status = HttpStatus.FORBIDDEN,
    )
