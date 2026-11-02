package org.opencontrol.erp.kernel.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.event.worker.OutboxDispatchWorker
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

data class EventOutboxDto(
    val id: UUID,
    val eventId: UUID,
    val eventType: String,
    val aggregateType: String,
    val aggregateId: String,
    val status: OutboxStatus,
    val retryCount: Int,
    val lastError: String?,
    val createdAt: Instant,
    val publishedAt: Instant?,
)

@RestController
@RequestMapping("/api/v1/extensions/events/outbox")
@Tag(name = "Event Outbox Management", description = "Monitor domain events, failures and trigger manual retries")
class EventOutboxController(
    private val eventOutboxRepository: EventOutboxRepository,
    private val outboxDispatchWorker: OutboxDispatchWorker,
) {
    @GetMapping
    @Operation(summary = "Inspect domain event outbox status and failed deliveries")
    fun listEventOutbox(
        @RequestParam(required = false) status: OutboxStatus?,
    ): ResponseEntity<List<EventOutboxDto>> {
        val list =
            if (status != null) {
                eventOutboxRepository.findAllByStatus(status)
            } else {
                eventOutboxRepository.findAll()
            }
        val dtos =
            list.map {
                EventOutboxDto(
                    id = it.id,
                    eventId = it.eventId,
                    eventType = it.eventType,
                    aggregateType = it.aggregateType,
                    aggregateId = it.aggregateId,
                    status = it.status,
                    retryCount = it.retryCount,
                    lastError = it.lastError,
                    createdAt = it.createdAt,
                    publishedAt = it.publishedAt,
                )
            }
        return ResponseEntity.ok(dtos)
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Manually retry a failed or dead-letter event")
    fun retryEvent(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val outbox =
            eventOutboxRepository.findById(id).orElseThrow {
                EntityNotFoundException("Outbox event not found with ID: $id")
            }
        outbox.status = OutboxStatus.PENDING
        outbox.nextRetryAt = Instant.now()
        eventOutboxRepository.save(outbox)
        outboxDispatchWorker.processEvent(outbox)
        return ResponseEntity.accepted().build()
    }
}
