# Data Model: Dynamic Custom Business Rules Engine

**Feature**: `004-custom-rules-engine`  
**Date**: 2026-11-02  
**Status**: Ready  

---

## 1. Database Schema (PostgreSQL / Flyway `V1_0_7`)

### Table: `custom_rule`

```sql
CREATE TABLE custom_rule (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    trigger_event VARCHAR(100) NOT NULL,
    timing VARCHAR(20) NOT NULL DEFAULT 'BEFORE',
    authoring_mode VARCHAR(30) NOT NULL DEFAULT 'VISUAL_BUILDER',
    condition_expression TEXT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_payload TEXT,
    priority INT NOT NULL DEFAULT 100,
    is_active BOOLEAN NOT NULL DEFAULT true,
    error_handling_policy VARCHAR(30) NOT NULL DEFAULT 'ABORT_TRANSACTION',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_custom_rule_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX idx_custom_rule_tenant_trigger 
    ON custom_rule (tenant_id, trigger_event, is_active, priority ASC);
```

### Table: `rule_execution_trace`

```sql
CREATE TABLE rule_execution_trace (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    rule_id UUID NOT NULL,
    trigger_event VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    duration_ms BIGINT NOT NULL,
    result VARCHAR(30) NOT NULL,
    error_detail TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rule_execution_trace_tenant_rule 
    ON rule_execution_trace (tenant_id, rule_id, created_at DESC);
```

---

## 2. Enums and Entities (Kotlin JPA)

### Enums
```kotlin
enum class RuleTiming {
    BEFORE, // Synchronous Pre-Commit
    AFTER   // Asynchronous Post-Commit
}

enum class AuthoringMode {
    VISUAL_BUILDER,
    ADVANCED_DSL
}

enum class RuleActionType {
    BLOCK_OPERATION,
    APPLY_DISCOUNT,
    SET_FIELD_VALUE,
    DISPATCH_NOTIFICATION,
    CUSTOM_ACTION
}

enum class ErrorHandlingPolicy {
    ABORT_TRANSACTION,
    LOG_AND_CONTINUE
}

enum class RuleExecutionResult {
    MATCHED,
    SKIPPED,
    ERROR
}
```

### `CustomRule` Entity
```kotlin
@Entity
@Table(name = "custom_rule")
class CustomRule(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(nullable = false, length = 150)
    var name: String,
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @Column(name = "trigger_event", nullable = false, length = 100)
    var triggerEvent: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var timing: RuleTiming = RuleTiming.BEFORE,
    @Enumerated(EnumType.STRING)
    @Column(name = "authoring_mode", nullable = false, length = 30)
    var authoringMode: AuthoringMode = AuthoringMode.VISUAL_BUILDER,
    @Column(name = "condition_expression", nullable = false, columnDefinition = "TEXT")
    var conditionExpression: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    var actionType: RuleActionType = RuleActionType.BLOCK_OPERATION,
    @Column(name = "action_payload", columnDefinition = "TEXT")
    var actionPayload: String? = null,
    @Column(nullable = false)
    var priority: Int = 100,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Enumerated(EnumType.STRING)
    @Column(name = "error_handling_policy", nullable = false, length = 30)
    var errorHandlingPolicy: ErrorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
```

### `RuleExecutionTrace` Entity
```kotlin
@Entity
@Table(name = "rule_execution_trace")
class RuleExecutionTrace(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "rule_id", nullable = false)
    val ruleId: UUID,
    @Column(name = "trigger_event", nullable = false, length = 100)
    val triggerEvent: String,
    @Column(name = "entity_type", nullable = false, length = 100)
    val entityType: String,
    @Column(name = "entity_id", nullable = false, length = 100)
    val entityId: String,
    @Column(name = "duration_ms", nullable = false)
    val durationMs: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val result: RuleExecutionResult,
    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
```

---

## 3. Data Transfer Objects (DTOs)

```kotlin
data class CustomRuleResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val triggerEvent: String,
    val timing: String,
    val authoringMode: String,
    val conditionExpression: String,
    val actionType: String,
    val actionPayload: String?,
    val priority: Int,
    val isActive: Boolean,
    val errorHandlingPolicy: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class CreateCustomRuleRequest(
    val name: String,
    val description: String?,
    val triggerEvent: String,
    val timing: String = "BEFORE",
    val authoringMode: String = "VISUAL_BUILDER",
    val conditionExpression: String,
    val actionType: String = "BLOCK_OPERATION",
    val actionPayload: String?,
    val priority: Int = 100,
    val errorHandlingPolicy: String = "ABORT_TRANSACTION",
)

data class TestRuleRequest(
    val conditionExpression: String,
    val actionType: String,
    val actionPayload: String?,
    val payloadJson: String,
)

data class TestRuleResponse(
    val matched: Boolean,
    val actionApplied: String?,
    val durationMs: Long,
    val error: String?,
)
```
