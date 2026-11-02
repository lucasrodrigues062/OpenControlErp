package org.opencontrol.erp.kernel.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tags
import org.opencontrol.erp.kernel.event.domain.OutboxStatus
import org.opencontrol.erp.kernel.event.repository.EventOutboxRepository
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.repository.ExtensionPluginRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExtensionMetrics(
    private val meterRegistry: MeterRegistry,
    private val pluginRepository: ExtensionPluginRepository,
    private val outboxRepository: EventOutboxRepository
) {

    @Scheduled(fixedRate = 10000)
    fun recordMetrics() {
        val activePlugins = pluginRepository.findAllByStatus(PluginStatus.ACTIVE).size.toDouble()
        val errorPlugins = pluginRepository.findAllByStatus(PluginStatus.ERROR).size.toDouble()
        val pendingOutbox = outboxRepository.findAllByStatus(OutboxStatus.PENDING).size.toDouble()
        val deadLetterOutbox = outboxRepository.findAllByStatus(OutboxStatus.DEAD_LETTER).size.toDouble()

        meterRegistry.gauge("opencontrol.plugins.active.count", Tags.empty(), activePlugins)
        meterRegistry.gauge("opencontrol.plugins.error.count", Tags.empty(), errorPlugins)
        meterRegistry.gauge("opencontrol.events.outbox.pending.count", Tags.empty(), pendingOutbox)
        meterRegistry.gauge("opencontrol.events.outbox.dead_letter.count", Tags.empty(), deadLetterOutbox)
    }
}
