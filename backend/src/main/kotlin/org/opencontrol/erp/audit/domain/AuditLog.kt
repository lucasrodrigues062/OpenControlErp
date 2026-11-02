package org.opencontrol.erp.audit.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_log")
class AuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "actor_id")
    val actorId: UUID? = null,
    @Column(name = "actor_username", nullable = false, length = 100)
    val actorUsername: String,
    @Column(nullable = false, length = 30)
    val action: String,
    @Column(name = "entity_type", nullable = false, length = 100)
    val entityType: String,
    @Column(name = "entity_id", nullable = false, length = 100)
    val entityId: String,
    @Column(columnDefinition = "TEXT")
    val details: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_before", columnDefinition = "jsonb")
    val stateBefore: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "state_after", columnDefinition = "jsonb")
    val stateAfter: String? = null,
    @Column(name = "client_ip", length = 45)
    val clientIp: String? = null,
    @Column(name = "user_agent", length = 255)
    val userAgent: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
