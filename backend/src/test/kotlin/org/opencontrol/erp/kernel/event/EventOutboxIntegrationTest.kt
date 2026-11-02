package org.opencontrol.erp.kernel.event

import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.event.service.DomainEventPublisher
import org.opencontrol.erp.kernel.event.worker.OutboxDispatchWorker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class EventOutboxIntegrationTest {
    @Autowired
    private lateinit var domainEventPublisher: DomainEventPublisher

    @Autowired
    private lateinit var eventOutboxRepository: EventOutboxRepository

    @Autowired
    private lateinit var outboxDispatchWorker: OutboxDispatchWorker

    @Test
    fun `should persist event in outbox and dispatch asynchronously`() {
        val event =
            TestOrderCreatedEvent(
                eventId = UUID.randomUUID(),
                totalAmount = 250.0,
            )

        domainEventPublisher.publish(event)

        val outbox = eventOutboxRepository.findByEventId(event.eventId)
        assertNotNull(outbox)
        assertEquals(OutboxStatus.PENDING, outbox.status)

        outboxDispatchWorker.processEvent(outbox)

        val published = eventOutboxRepository.findByEventId(event.eventId)
        assertNotNull(published)
        assertEquals(OutboxStatus.PUBLISHED, published.status)
        assertNotNull(published.publishedAt)
    }
}
