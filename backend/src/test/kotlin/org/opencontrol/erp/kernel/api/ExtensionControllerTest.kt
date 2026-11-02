package org.opencontrol.erp.kernel.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.service.PluginLifecycleService
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExtensionControllerTest {
    private val service = mockk<PluginLifecycleService>(relaxed = true)
    private val controller = ExtensionController(service)

    @Test
    fun `listExtensions should return 200 with mapped DTOs`() {
        val plugin =
            ExtensionPlugin(
                pluginId = "org.opencontrol.test",
                name = "Test Plugin",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )

        every { service.list(PluginStatus.ACTIVE) } returns listOf(plugin)

        val response = controller.listExtensions(PluginStatus.ACTIVE)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
        assertEquals("org.opencontrol.test", response.body?.first()?.pluginId)
    }

    @Test
    fun `activateExtension should return 200 with activated status`() {
        val id = UUID.randomUUID()
        val plugin =
            ExtensionPlugin(
                id = id,
                pluginId = "org.opencontrol.test",
                name = "Test Plugin",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.ACTIVE,
            )

        every { service.activate(id) } returns plugin

        val response = controller.activateExtension(id)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(PluginStatus.ACTIVE, response.body?.status)
        verify { service.activate(id) }
    }

    @Test
    fun `deactivateExtension should return 200 with inactive status`() {
        val id = UUID.randomUUID()
        val plugin =
            ExtensionPlugin(
                id = id,
                pluginId = "org.opencontrol.test",
                name = "Test Plugin",
                version = "1.0.0",
                vendor = "OpenControl",
                minKernelVersion = "1.0.0",
                status = PluginStatus.INACTIVE,
            )

        every { service.deactivate(id) } returns plugin

        val response = controller.deactivateExtension(id)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(PluginStatus.INACTIVE, response.body?.status)
        verify { service.deactivate(id) }
    }

    @Test
    fun `uninstallExtension should return 204 No Content`() {
        val id = UUID.randomUUID()

        val response = controller.uninstallExtension(id, purgeData = true)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify { service.uninstall(id, true) }
    }
}
