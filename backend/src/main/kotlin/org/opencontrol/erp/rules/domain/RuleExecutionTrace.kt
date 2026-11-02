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
    val result: TraceResult,
    @Column(name = "error_detail", columnDefinition = "TEXT")
    val errorDetail: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
