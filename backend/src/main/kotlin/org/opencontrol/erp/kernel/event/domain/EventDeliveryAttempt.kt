package org.opencontrol.erp.kernel.event.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "event_delivery_attempt")
class EventDeliveryAttempt(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outbox_fk", nullable = false)
    val outbox: EventOutbox,

    @Column(name = "listener_identifier", nullable = false, length = 255)
    val listenerIdentifier: String,

    @Column(nullable = false, length = 30)
    val status: String,

    @Column(name = "error_trace", columnDefinition = "TEXT")
    val errorTrace: String? = null,

    @Column(name = "duration_ms", nullable = false)
    val durationMs: Int,

    @Column(name = "attempted_at", nullable = false)
    val attemptedAt: Instant = Instant.now()
)
