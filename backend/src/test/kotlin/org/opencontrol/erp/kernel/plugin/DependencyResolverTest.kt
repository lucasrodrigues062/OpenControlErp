package org.opencontrol.erp.kernel.plugin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.kernel.exception.PluginDependencyException
import org.opencontrol.erp.kernel.plugin.domain.ExtensionDependency
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.service.DependencyResolver
import kotlin.test.assertEquals

class DependencyResolverTest {

    private val resolver = DependencyResolver()

    @Test
    fun `should resolve correct activation order for linear dependencies`() {
        val pluginA = createPlugin("org.opencontrol.a")
        val pluginB = createPlugin("org.opencontrol.b")
        pluginB.dependencies.add(ExtensionDependency(plugin = pluginB, dependsOnPluginId = "org.opencontrol.a", versionRange = ">=1.0.0"))

        val order = resolver.resolveActivationOrder(listOf(pluginB, pluginA))
        assertEquals(listOf("org.opencontrol.a", "org.opencontrol.b"), order.map { it.pluginId })
    }

    @Test
    fun `should detect circular dependencies and throw PluginDependencyException`() {
        val pluginA = createPlugin("org.opencontrol.a")
        val pluginB = createPlugin("org.opencontrol.b")

        pluginA.dependencies.add(ExtensionDependency(plugin = pluginA, dependsOnPluginId = "org.opencontrol.b", versionRange = ">=1.0.0"))
        pluginB.dependencies.add(ExtensionDependency(plugin = pluginB, dependsOnPluginId = "org.opencontrol.a", versionRange = ">=1.0.0"))

        assertThrows<PluginDependencyException> {
            resolver.resolveActivationOrder(listOf(pluginA, pluginB))
        }
    }

    @Test
    fun `should reject deactivation when active dependents exist`() {
        val pluginA = createPlugin("org.opencontrol.a")
        val pluginB = createPlugin("org.opencontrol.b")
        pluginB.dependencies.add(ExtensionDependency(plugin = pluginB, dependsOnPluginId = "org.opencontrol.a", versionRange = ">=1.0.0"))

        assertThrows<PluginDependencyException> {
            resolver.validateCanDeactivate(pluginA, listOf(pluginA, pluginB))
        }
    }

    private fun createPlugin(id: String): ExtensionPlugin {
        return ExtensionPlugin(
            pluginId = id,
            name = id,
            version = "1.0.0",
            vendor = "Test",
            minKernelVersion = "1.0.0",
            status = PluginStatus.INSTALLED
        )
    }
}
