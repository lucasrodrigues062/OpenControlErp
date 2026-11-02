package org.opencontrol.erp.rules.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

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
    var actionType: RuleActionType,
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
