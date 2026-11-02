package org.opencontrol.erp.rules.domain

enum class RuleTiming {
    BEFORE, // Synchronous Pre-Commit
    AFTER, // Asynchronous Post-Commit
}

enum class AuthoringMode {
    VISUAL_BUILDER,
    ADVANCED_DSL,
}

enum class RuleActionType {
    BLOCK_OPERATION,
    APPLY_DISCOUNT,
    SET_FIELD_VALUE,
    DISPATCH_NOTIFICATION,
    CUSTOM_ACTION,
}

enum class ErrorHandlingPolicy {
    ABORT_TRANSACTION,
    LOG_AND_CONTINUE,
}

enum class TraceResult {
    MATCHED,
    SKIPPED,
    ERROR,
}
