package org.opencontrol.erp.kernel.event.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    DEAD_LETTER,
}

@Entity
@Table(name = "event_outbox")
class EventOutbox(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "event_id", nullable = false, unique = true)
    val eventId: UUID,
    @Column(name = "event_type", nullable = false, length = 120)
    val eventType: String,
    @Column(name = "aggregate_type", nullable = false, length = 80)
    val aggregateType: String,
    @Column(name = "aggregate_id", nullable = false, length = 80)
    val aggregateId: String,
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    val payload: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: OutboxStatus = OutboxStatus.PENDING,
    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0,
    @Column(name = "last_error", columnDefinition = "TEXT")
    var lastError: String? = null,
    @Column(name = "next_retry_at")
    var nextRetryAt: Instant? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "published_at")
    var publishedAt: Instant? = null,
)
