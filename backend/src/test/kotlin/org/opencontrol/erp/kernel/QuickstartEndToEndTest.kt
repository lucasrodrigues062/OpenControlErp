package org.opencontrol.erp.kernel

import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.event.TestOrderCreatedEvent
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.event.service.DomainEventPublisher
import org.opencontrol.erp.kernel.event.worker.OutboxDispatchWorker
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.repository.ExtensionPluginRepository
import org.opencontrol.erp.kernel.plugin.service.PluginLifecycleService
import org.opencontrol.erp.kernel.spi.ExecutionContext
import org.opencontrol.erp.kernel.spi.fiscal.DefaultNoOpFiscalEngine
import org.opencontrol.erp.kernel.spi.fiscal.FiscalDocumentRequest
import org.opencontrol.erp.kernel.spi.fiscal.FiscalEngineSPI
import org.opencontrol.erp.kernel.spi.service.ExtensionRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
class QuickstartEndToEndTest {
    @Autowired
    private lateinit var pluginLifecycleService: PluginLifecycleService

    @Autowired
    private lateinit var pluginRepository: ExtensionPluginRepository

    @Autowired
    private lateinit var extensionRegistry: ExtensionRegistry

    @Autowired
    private lateinit var domainEventPublisher: DomainEventPublisher

    @Autowired
    private lateinit var eventOutboxRepository: EventOutboxRepository

    @Autowired
    private lateinit var outboxDispatchWorker: OutboxDispatchWorker

    @Test
    fun `execute end-to-end quickstart scenarios`() {
        // Scenario 1: Plugin Lifecycle Management
        val uniquePluginId = "org.opencontrol.quickstart.app-${UUID.randomUUID().toString().take(8)}"
        val testPlugin =
            ExtensionPlugin(
                pluginId = uniquePluginId,
                name = "Quickstart App",
                version = "1.0.0",
                vendor = "OpenControl Core",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )
        val saved = pluginRepository.save(testPlugin)
        val deactivated = pluginLifecycleService.deactivate(saved.id)
        assertEquals(PluginStatus.INACTIVE, deactivated.status)

        // Scenario 2: Contextual SPI Resolution & Fallback
        extensionRegistry.registerDefaultFallback(FiscalEngineSPI::class.java, DefaultNoOpFiscalEngine())
        val context = ExecutionContext.defaultFor("BR")
        val resolved = extensionRegistry.resolve(FiscalEngineSPI::class.java, context)
        val docResult = resolved.issueDocument(FiscalDocumentRequest("101", "1", 500.0, "11122233344", 2))
        assertEquals("SIMULATED", docResult.status)

        // Scenario 3: Transactional Outbox Event Dispatch
        val event =
            TestOrderCreatedEvent(
                eventId = UUID.randomUUID(),
                totalAmount = 1500.0,
            )
        domainEventPublisher.publish(event)
        val outboxItem = eventOutboxRepository.findByEventId(event.eventId)
        assertNotNull(outboxItem)
        assertEquals(OutboxStatus.PENDING, outboxItem.status)

        outboxDispatchWorker.processEvent(outboxItem)
        val dispatchedItem = eventOutboxRepository.findByEventId(event.eventId)
        assertNotNull(dispatchedItem)
        assertEquals(OutboxStatus.PUBLISHED, dispatchedItem.status)
    }
}
