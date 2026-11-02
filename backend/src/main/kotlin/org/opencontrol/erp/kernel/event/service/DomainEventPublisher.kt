package org.opencontrol.erp.kernel.event.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.opencontrol.erp.kernel.event.DomainEvent
import org.opencontrol.erp.kernel.event.domain.EventOutbox
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CopyOnWriteArrayList

fun interface PreCommitInterceptor<T : DomainEvent> {
    fun intercept(event: T)
}

@Service
class DomainEventPublisher(
    private val eventOutboxRepository: EventOutboxRepository,
    private val springEventPublisher: ApplicationEventPublisher,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val preCommitInterceptors = CopyOnWriteArrayList<PreCommitInterceptor<DomainEvent>>()

    @Suppress("UNCHECKED_CAST")
    fun <T : DomainEvent> registerPreCommitInterceptor(interceptor: PreCommitInterceptor<T>) {
        preCommitInterceptors.add(interceptor as PreCommitInterceptor<DomainEvent>)
        logger.info("Registered pre-commit interceptor: {}", interceptor)
    }

    @Transactional
    fun publish(event: DomainEvent) {
        logger.debug("Publishing domain event: {} (id: {})", event.eventType, event.eventId)

        // 1. Execute synchronous pre-commit interceptors
        for (interceptor in preCommitInterceptors) {
            try {
                interceptor.intercept(event)
            } catch (e: Exception) {
                logger.warn("Pre-commit interceptor rejected event '{}': {}", event.eventType, e.message)
                throw e
            }
        }

        // 2. Publish locally to Spring Context (for internal synchronous listeners)
        springEventPublisher.publishEvent(event)

        // 3. Persist to transactional outbox for durable asynchronous delivery
        val payloadJson = objectMapper.writeValueAsString(event)
        val outbox = EventOutbox(
            eventId = event.eventId,
            eventType = event.eventType,
            aggregateType = event.aggregateType,
            aggregateId = event.aggregateId,
            tenantId = event.tenantId,
            payload = payloadJson,
            status = OutboxStatus.PENDING
        )
        eventOutboxRepository.save(outbox)
        logger.debug("Event '{}' recorded in outbox", event.eventId)
    }
}
