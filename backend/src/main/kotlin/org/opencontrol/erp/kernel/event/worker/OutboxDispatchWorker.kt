package org.opencontrol.erp.kernel.event.worker

import org.opencontrol.erp.kernel.event.domain.EventOutbox
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import kotlin.math.min
import kotlin.math.pow

@Component
@EnableScheduling
class OutboxDispatchWorker(
    private val eventOutboxRepository: EventOutboxRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()

    companion object {
        const val MAX_RETRIES = 5
        const val BATCH_SIZE = 50
    }

    @Scheduled(fixedDelayString = "\${opencontrol.events.outbox.poll-interval-ms:2000}")
    fun pollAndDispatch() {
        val now = Instant.now()
        val pendingEvents = eventOutboxRepository.findPendingForDispatch(now, PageRequest.of(0, BATCH_SIZE))

        if (pendingEvents.isEmpty()) return

        for (outbox in pendingEvents) {
            virtualThreadExecutor.submit {
                processEvent(outbox)
            }
        }
    }

    @Transactional
    fun processEvent(outbox: EventOutbox) {
        val startTime = System.currentTimeMillis()
        try {
            outbox.status = OutboxStatus.PROCESSING
            eventOutboxRepository.save(outbox)

            // Simulate dispatch to active plugin subscribers / external endpoints
            logger.info("Dispatching outbox event '{}' of type '{}'...", outbox.eventId, outbox.eventType)

            outbox.status = OutboxStatus.PUBLISHED
            outbox.publishedAt = Instant.now()
            outbox.lastError = null
            eventOutboxRepository.save(outbox)
            logger.debug("Successfully dispatched event '{}' in {}ms", outbox.eventId, System.currentTimeMillis() - startTime)
        } catch (e: Exception) {
            val nextRetryCount = outbox.retryCount + 1
            outbox.retryCount = nextRetryCount
            outbox.lastError = e.message

            if (nextRetryCount >= MAX_RETRIES) {
                outbox.status = OutboxStatus.DEAD_LETTER
                outbox.nextRetryAt = null
                logger.error("Outbox event '{}' exceeded max retries. Moved to DEAD_LETTER", outbox.eventId, e)
            } else {
                outbox.status = OutboxStatus.FAILED
                val delaySeconds = (2.0.pow(nextRetryCount.toDouble()) * 2).toLong()
                outbox.nextRetryAt = Instant.now().plus(Duration.ofSeconds(min(delaySeconds, 3600)))
                logger.warn("Failed to dispatch outbox event '{}'. Scheduled retry in {}s", outbox.eventId, delaySeconds)
            }
            eventOutboxRepository.save(outbox)
        }
    }
}
