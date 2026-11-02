package org.opencontrol.erp.kernel.plugin.loader

import java.io.Closeable
import java.io.File
import java.net.URLClassLoader

/**
 * Isolated ClassLoader dedicated to a specific plugin archive.
 * Inherits from the parent application classloader to access platform APIs and SPIs,
 * while preventing classes in the plugin from leaking across sibling plugins.
 */
class PluginClassLoader(
    val pluginId: String,
    jarFile: File,
    parent: ClassLoader = PluginClassLoader::class.java.classLoader,
) : URLClassLoader(arrayOf(jarFile.toURI().toURL()), parent),
    Closeable {
    override fun toString(): String = "PluginClassLoader(pluginId='$pluginId', urls=${urLs.joinToString()})"
}
