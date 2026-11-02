package org.opencontrol.erp.kernel.plugin

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.PluginValidationException
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginAction
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.loader.ManifestParser
import org.opencontrol.erp.kernel.plugin.repository.ExtensionAuditLogRepository
import org.opencontrol.erp.kernel.plugin.repository.ExtensionPluginRepository
import org.opencontrol.erp.kernel.plugin.service.DependencyResolver
import org.opencontrol.erp.kernel.plugin.service.PluginLifecycleService
import org.opencontrol.erp.kernel.spi.service.ExtensionRegistry
import org.springframework.context.ApplicationContext
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class PluginLifecycleServiceTest {
    private val pluginRepository = mockk<ExtensionPluginRepository>(relaxed = true)
    private val auditLogRepository = mockk<ExtensionAuditLogRepository>(relaxed = true)
    private val manifestParser = mockk<ManifestParser>(relaxed = true)
    private val dependencyResolver = mockk<DependencyResolver>(relaxed = true)
    private val applicationContext = mockk<ApplicationContext>(relaxed = true)
    private val extensionRegistry = mockk<ExtensionRegistry>(relaxed = true)

    private val service =
        PluginLifecycleService(
            pluginRepository = pluginRepository,
            auditLogRepository = auditLogRepository,
            manifestParser = manifestParser,
            dependencyResolver = dependencyResolver,
            applicationContext = applicationContext,
            extensionRegistry = extensionRegistry,
        )

    @BeforeEach
    fun setUp() {
        every { auditLogRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `activate should return existing plugin if already active`() {
        val pluginId = UUID.randomUUID()
        val plugin =
            ExtensionPlugin(
                id = pluginId,
                pluginId = "org.opencontrol.sample",
                name = "Sample",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )

        every { pluginRepository.findById(pluginId) } returns Optional.of(plugin)

        val result = service.activate(pluginId)
        assertEquals(PluginStatus.ACTIVE, result.status)
        verify(exactly = 0) { extensionRegistry.registerProvider(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `activate should transition to ERROR when archive file is missing`() {
        val pluginId = UUID.randomUUID()
        val plugin =
            ExtensionPlugin(
                id = pluginId,
                pluginId = "org.opencontrol.missing",
                name = "Missing",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.INSTALLED,
                filePath = "non-existent-path.jar",
            )

        every { pluginRepository.findById(pluginId) } returns Optional.of(plugin)
        every { pluginRepository.findAll() } returns listOf(plugin)
        every { pluginRepository.save(any()) } answers { firstArg() }

        assertThrows<PluginValidationException> {
            service.activate(pluginId)
        }

        assertEquals(PluginStatus.ERROR, plugin.status)
        verify { auditLogRepository.save(match { it.action == PluginAction.ERROR }) }
    }

    @Test
    fun `deactivate should return existing plugin if not active`() {
        val pluginId = UUID.randomUUID()
        val plugin =
            ExtensionPlugin(
                id = pluginId,
                pluginId = "org.opencontrol.inactive",
                name = "Inactive",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.INACTIVE,
            )

        every { pluginRepository.findById(pluginId) } returns Optional.of(plugin)

        val result = service.deactivate(pluginId)
        assertEquals(PluginStatus.INACTIVE, result.status)
    }

    @Test
    fun `deactivate should unregister SPIs and set status to INACTIVE`() {
        val pluginId = UUID.randomUUID()
        val plugin =
            ExtensionPlugin(
                id = pluginId,
                pluginId = "org.opencontrol.to-deactivate",
                name = "Active Plugin",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )

        every { pluginRepository.findById(pluginId) } returns Optional.of(plugin)
        every { pluginRepository.findAllByStatus(PluginStatus.ACTIVE) } returns listOf(plugin)
        every { pluginRepository.save(any()) } answers { firstArg() }

        val result = service.deactivate(pluginId)
        assertEquals(PluginStatus.INACTIVE, result.status)
        verify { extensionRegistry.unregisterProvidersByPluginId("org.opencontrol.to-deactivate") }
        verify { auditLogRepository.save(match { it.action == PluginAction.DEACTIVATE }) }
    }

    @Test
    fun `getById should throw EntityNotFoundException when plugin does not exist`() {
        val pluginId = UUID.randomUUID()
        every { pluginRepository.findById(pluginId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            service.getById(pluginId)
        }
    }
}
