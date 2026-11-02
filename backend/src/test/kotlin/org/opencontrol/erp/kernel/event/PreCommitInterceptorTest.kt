package org.opencontrol.erp.kernel.event

import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.event.service.DomainEventPublisher
import org.opencontrol.erp.kernel.event.service.PreCommitInterceptor
import org.opencontrol.erp.kernel.exception.PluginValidationException
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.UUID

data class TestOrderCreatedEvent(
    override val eventId: UUID = UUID.randomUUID(),
    override val eventType: String = "sales.order.created",
    override val aggregateType: String = "Order",
    override val aggregateId: String = "1001",
    override val tenantId: UUID = UUID.randomUUID(),
    override val occurredAt: Instant = Instant.now(),
    val totalAmount: Double,
) : DomainEvent

class PreCommitInterceptorTest {
    private val eventOutboxRepository = mockk<EventOutboxRepository>(relaxed = true)
    private val springEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val objectMapper =
        com.fasterxml.jackson.module.kotlin
            .jacksonObjectMapper()

    private val publisher =
        DomainEventPublisher(
            eventOutboxRepository = eventOutboxRepository,
            springEventPublisher = springEventPublisher,
            objectMapper = objectMapper,
        )

    @Test
    fun `should abort transaction when pre-commit hook throws validation exception`() {
        val blockingHook =
            PreCommitInterceptor<TestOrderCreatedEvent> { event ->
                if (event.totalAmount <= 0) {
                    throw PluginValidationException("Order amount must be greater than zero")
                }
            }

        publisher.registerPreCommitInterceptor(blockingHook)

        val invalidEvent = TestOrderCreatedEvent(totalAmount = -10.0)

        assertThrows<PluginValidationException> {
            publisher.publish(invalidEvent)
        }
    }
}
