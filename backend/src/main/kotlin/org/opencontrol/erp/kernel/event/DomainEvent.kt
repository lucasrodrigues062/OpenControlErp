package org.opencontrol.erp.kernel.event

import java.time.Instant
import java.util.UUID

/**
 * Base interface for all immutable domain events across the ERP.
 */
interface DomainEvent {
    val eventId: UUID
    val eventType: String
    val aggregateType: String
    val aggregateId: String
    val tenantId: UUID
    val occurredAt: Instant
}

enum class EventMode {
    SYNC_PRE_COMMIT,
    ASYNC_POST_COMMIT,
}
