package org.opencontrol.erp.kernel.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.event.domain.EventOutbox
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.repository.ExtensionPluginRepository
import java.util.UUID
import kotlin.test.assertEquals

class ExtensionMetricsTest {
    private val meterRegistry = SimpleMeterRegistry()
    private val pluginRepository = mockk<ExtensionPluginRepository>(relaxed = true)
    private val outboxRepository = mockk<EventOutboxRepository>(relaxed = true)

    private val metrics = ExtensionMetrics(meterRegistry, pluginRepository, outboxRepository)

    @Test
    fun `should register and update plugin and outbox gauges`() {
        val activePlugin =
            ExtensionPlugin(
                pluginId = "plugin.1",
                name = "Active",
                version = "1.0.0",
                vendor = "V",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )
        val pendingOutbox =
            EventOutbox(
                eventId = UUID.randomUUID(),
                eventType = "e",
                aggregateType = "A",
                aggregateId = "1",
                tenantId = UUID.randomUUID(),
                payload = "{}",
                status = OutboxStatus.PENDING,
            )

        every { pluginRepository.findAllByStatus(PluginStatus.ACTIVE) } returns listOf(activePlugin)
        every { pluginRepository.findAllByStatus(PluginStatus.ERROR) } returns emptyList()
        every { outboxRepository.findAllByStatus(OutboxStatus.PENDING) } returns listOf(pendingOutbox)
        every { outboxRepository.findAllByStatus(OutboxStatus.DEAD_LETTER) } returns emptyList()

        metrics.recordMetrics()

        assertEquals(1.0, meterRegistry.get("opencontrol.plugins.active.count").gauge().value())
        assertEquals(0.0, meterRegistry.get("opencontrol.plugins.error.count").gauge().value())
        assertEquals(1.0, meterRegistry.get("opencontrol.events.outbox.pending.count").gauge().value())
        assertEquals(0.0, meterRegistry.get("opencontrol.events.outbox.dead_letter.count").gauge().value())
    }
}
