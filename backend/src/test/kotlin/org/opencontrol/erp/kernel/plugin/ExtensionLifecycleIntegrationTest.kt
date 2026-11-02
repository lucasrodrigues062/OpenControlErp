package org.opencontrol.erp.kernel.plugin

import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.repository.ExtensionAuditLogRepository
import org.opencontrol.erp.kernel.plugin.repository.ExtensionPluginRepository
import org.opencontrol.erp.kernel.plugin.service.PluginLifecycleService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ExtensionLifecycleIntegrationTest {
    @Autowired
    private lateinit var pluginLifecycleService: PluginLifecycleService

    @Autowired
    private lateinit var pluginRepository: ExtensionPluginRepository

    @Autowired
    private lateinit var auditLogRepository: ExtensionAuditLogRepository

    @Test
    fun `should manage full plugin lifecycle and audit trail`() {
        val uniqueId = "org.opencontrol.test.sample-${UUID.randomUUID().toString().take(8)}"
        val plugin =
            ExtensionPlugin(
                pluginId = uniqueId,
                name = "Sample Plugin",
                version = "1.0.0",
                vendor = "Test Vendor",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )
        val saved = pluginRepository.save(plugin)
        assertNotNull(saved.id)

        // Deactivate test
        val deactivated = pluginLifecycleService.deactivate(saved.id)
        assertEquals(PluginStatus.INACTIVE, deactivated.status)

        // Verify audit log exists
        val logs = auditLogRepository.findAllByPluginFkOrderByCreatedAtDesc(saved.id)
        assertNotNull(logs)
    }
}
