package org.opencontrol.erp.rules.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.opencontrol.erp.kernel.event.DomainEvent
import org.opencontrol.erp.kernel.event.service.DomainEventPublisher
import org.opencontrol.erp.kernel.event.service.PreCommitInterceptor
import org.opencontrol.erp.rules.domain.RuleTiming
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.Executors

@Component
class RuleInterceptor(
    private val ruleExecutionService: RuleExecutionService,
    private val domainEventPublisher: DomainEventPublisher,
    private val objectMapper: ObjectMapper,
) : PreCommitInterceptor<DomainEvent> {
    private val logger = LoggerFactory.getLogger(RuleInterceptor::class.java)
    private val virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()

    @PostConstruct
    fun init() {
        domainEventPublisher.registerPreCommitInterceptor(this)
        logger.info("RuleInterceptor registered as PreCommitInterceptor with DomainEventPublisher")
    }

    override fun intercept(event: DomainEvent) {
        val context = extractEventContext(event)
        ruleExecutionService.executeRules(
            tenantId = event.tenantId,
            triggerEvent = event.eventType,
            timing = RuleTiming.BEFORE,
            context = context,
            entityType = event.aggregateType,
            entityId = event.aggregateId,
        )
    }

    @EventListener
    fun onAfterCommit(event: DomainEvent) {
        virtualThreadExecutor.submit {
            try {
                val context = extractEventContext(event)
                ruleExecutionService.executeRules(
                    tenantId = event.tenantId,
                    triggerEvent = event.eventType,
                    timing = RuleTiming.AFTER,
                    context = context,
                    entityType = event.aggregateType,
                    entityId = event.aggregateId,
                )
            } catch (e: Exception) {
                logger.error("Error executing AFTER rules for event '{}': {}", event.eventType, e.message)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractEventContext(event: DomainEvent): Map<String, Any?> =
        try {
            val json = objectMapper.writeValueAsString(event)
            val map = objectMapper.readValue(json, Map::class.java) as Map<String, Any?>
            val entityKey = event.aggregateType.lowercase()
            map +
                mapOf(
                    entityKey to map,
                    "eventType" to event.eventType,
                    "aggregateType" to event.aggregateType,
                    "aggregateId" to event.aggregateId,
                )
        } catch (e: Exception) {
            mapOf(
                "eventId" to event.eventId.toString(),
                "eventType" to event.eventType,
                "aggregateType" to event.aggregateType,
                "aggregateId" to event.aggregateId,
            )
        }
}
