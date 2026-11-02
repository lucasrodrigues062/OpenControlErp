package org.opencontrol.erp.kernel.plugin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.opencontrol.erp.kernel.plugin.loader.PluginClassLoader
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PluginClassLoaderTest {
    @Test
    fun `should load resource from plugin jar file`(
        @TempDir tempDir: File,
    ) {
        val jarFile = File(tempDir, "test-plugin.jar")
        JarOutputStream(FileOutputStream(jarFile)).use { jos ->
            jos.putNextEntry(JarEntry("test-resource.txt"))
            jos.write("hello-from-plugin".toByteArray())
            jos.closeEntry()
        }

        val classLoader = PluginClassLoader("org.opencontrol.test", jarFile)
        classLoader.use { cl ->
            val resource = cl.getResourceAsStream("test-resource.txt")
            assertNotNull(resource)
            val content = resource.bufferedReader().readText()
            assertEquals("hello-from-plugin", content)
            assertTrue(cl.toString().contains("org.opencontrol.test"))
        }
    }
}
