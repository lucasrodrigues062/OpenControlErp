package org.opencontrol.erp.kernel.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.event.domain.EventOutbox
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.event.worker.OutboxDispatchWorker
import org.springframework.http.HttpStatus
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EventOutboxControllerTest {

    private val repository = mockk<EventOutboxRepository>(relaxed = true)
    private val worker = mockk<OutboxDispatchWorker>(relaxed = true)
    private val controller = EventOutboxController(repository, worker)

    @Test
    fun `listEventOutbox should return mapped outbox dtos`() {
        val outbox = EventOutbox(
            eventId = UUID.randomUUID(),
            eventType = "order.created",
            aggregateType = "Order",
            aggregateId = "123",
            tenantId = UUID.randomUUID(),
            payload = "{}",
            status = OutboxStatus.PENDING
        )

        every { repository.findAllByStatus(OutboxStatus.PENDING) } returns listOf(outbox)

        val response = controller.listEventOutbox(OutboxStatus.PENDING)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
        assertEquals("order.created", response.body?.first()?.eventType)
    }

    @Test
    fun `retryEvent should reset status and trigger worker dispatch`() {
        val id = UUID.randomUUID()
        val outbox = EventOutbox(
            id = id,
            eventId = UUID.randomUUID(),
            eventType = "order.created",
            aggregateType = "Order",
            aggregateId = "123",
            tenantId = UUID.randomUUID(),
            payload = "{}",
            status = OutboxStatus.FAILED
        )

        every { repository.findById(id) } returns Optional.of(outbox)
        every { repository.save(any()) } answers { firstArg() }

        val response = controller.retryEvent(id)

        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        assertEquals(OutboxStatus.PENDING, outbox.status)
        verify { worker.processEvent(outbox) }
    }
}
