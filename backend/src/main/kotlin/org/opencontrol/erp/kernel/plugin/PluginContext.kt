package org.opencontrol.erp.kernel.plugin

import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.springframework.context.ApplicationContext
import java.io.File

data class PluginContext(
    val plugin: ExtensionPlugin,
    val classLoader: ClassLoader,
    val applicationContext: ApplicationContext,
    val pluginDirectory: File,
)
