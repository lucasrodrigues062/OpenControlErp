package org.opencontrol.erp.kernel.event.repository

import org.opencontrol.erp.kernel.event.domain.EventOutbox
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface EventOutboxRepository : JpaRepository<EventOutbox, UUID> {
    fun findByEventId(eventId: UUID): EventOutbox?

    fun findAllByStatus(status: OutboxStatus): List<EventOutbox>

    @Query(
        """
        SELECT e FROM EventOutbox e
        WHERE (e.status = 'PENDING' OR (e.status = 'FAILED' AND e.nextRetryAt <= :now))
        ORDER BY e.createdAt ASC
    """,
    )
    fun findPendingForDispatch(
        now: Instant,
        pageable: Pageable,
    ): List<EventOutbox>
}
