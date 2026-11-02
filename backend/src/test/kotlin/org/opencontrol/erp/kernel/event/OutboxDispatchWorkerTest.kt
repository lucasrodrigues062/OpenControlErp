package org.opencontrol.erp.kernel.event

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.event.domain.EventOutbox
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.event.worker.OutboxDispatchWorker
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OutboxDispatchWorkerTest {
    private val eventOutboxRepository = mockk<EventOutboxRepository>(relaxed = true)
    private val worker = OutboxDispatchWorker(eventOutboxRepository)

    @Test
    fun `processEvent should transition outbox to PUBLISHED on success`() {
        val outbox =
            EventOutbox(
                eventId = UUID.randomUUID(),
                eventType = "test.event",
                aggregateType = "Test",
                aggregateId = "1",
                tenantId = UUID.randomUUID(),
                payload = "{}",
                status = OutboxStatus.PENDING,
            )

        every { eventOutboxRepository.save(any()) } answers { firstArg() }

        worker.processEvent(outbox)

        assertEquals(OutboxStatus.PUBLISHED, outbox.status)
        assertNotNull(outbox.publishedAt)
        assertNull(outbox.lastError)
    }

    @Test
    fun `processEvent should transition to DEAD_LETTER when retryCount reaches MAX_RETRIES`() {
        val outbox =
            EventOutbox(
                eventId = UUID.randomUUID(),
                eventType = "test.failing.event",
                aggregateType = "Test",
                aggregateId = "1",
                tenantId = UUID.randomUUID(),
                payload = "{}",
                status = OutboxStatus.FAILED,
                retryCount = OutboxDispatchWorker.MAX_RETRIES - 1,
            )

        // Make save fail when attempting to set PROCESSING to simulate failure
        var callCount = 0
        every { eventOutboxRepository.save(any()) } answers {
            callCount++
            if (callCount == 1) {
                throw RuntimeException("Simulated delivery error to external service")
            }
            firstArg()
        }

        worker.processEvent(outbox)

        assertEquals(OutboxStatus.DEAD_LETTER, outbox.status)
        assertEquals(OutboxDispatchWorker.MAX_RETRIES, outbox.retryCount)
        assertNotNull(outbox.lastError)
    }
}
