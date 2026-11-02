package org.opencontrol.erp.rules.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.event.DomainEvent
import org.opencontrol.erp.kernel.event.service.DomainEventPublisher
import org.opencontrol.erp.rules.domain.RuleTiming
import java.time.Instant
import java.util.UUID

class RuleInterceptorTest {
    private val executionService: RuleExecutionService = mockk(relaxed = true)
    private val publisher: DomainEventPublisher = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()

    private lateinit var interceptor: RuleInterceptor
    private val tenantId = UUID.randomUUID()

    data class TestSaleEvent(
        override val eventId: UUID = UUID.randomUUID(),
        override val eventType: String = "sale.finalized",
        override val aggregateType: String = "SALE",
        override val aggregateId: String = "sale-123",
        override val tenantId: UUID,
        override val occurredAt: Instant = Instant.now(),
        val totalAmount: Double = 500.0,
    ) : DomainEvent

    @BeforeEach
    fun setUp() {
        interceptor = RuleInterceptor(executionService, publisher, objectMapper)
    }

    @Test
    fun `should register as pre-commit interceptor on init`() {
        interceptor.init()
        verify { publisher.registerPreCommitInterceptor(interceptor) }
    }

    @Test
    fun `should execute BEFORE rules synchronously on intercept`() {
        val event = TestSaleEvent(tenantId = tenantId)

        interceptor.intercept(event)

        verify {
            executionService.executeRules(
                tenantId = tenantId,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.BEFORE,
                context = any(),
                entityType = "SALE",
                entityId = "sale-123",
            )
        }
    }

    @Test
    fun `should execute AFTER rules asynchronously on onAfterCommit`() {
        val event = TestSaleEvent(tenantId = tenantId)

        interceptor.onAfterCommit(event)
        Thread.sleep(100) // Allow virtual thread to execute

        verify {
            executionService.executeRules(
                tenantId = tenantId,
                triggerEvent = "sale.finalized",
                timing = RuleTiming.AFTER,
                context = any(),
                entityType = "SALE",
                entityId = "sale-123",
            )
        }
    }
}
