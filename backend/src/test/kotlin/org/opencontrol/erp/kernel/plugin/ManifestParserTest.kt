package org.opencontrol.erp.kernel.plugin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.kernel.exception.PluginValidationException
import org.opencontrol.erp.kernel.plugin.loader.ManifestParser
import kotlin.test.assertEquals

class ManifestParserTest {
    private val parser = ManifestParser()

    @Test
    fun `should parse valid manifest JSON successfully`() {
        val json =
            """
            {
              "id": "org.opencontrol.fiscal.br",
              "name": "Brazilian Fiscal Engine",
              "version": "1.0.0",
              "vendor": "OpenControl Community",
              "minKernelVersion": "1.0.0",
              "dependencies": [
                {
                  "pluginId": "org.opencontrol.base",
                  "versionRange": ">=1.0.0"
                }
              ]
            }
            """.trimIndent()

        val manifest = parser.parse(json.byteInputStream())
        assertEquals("org.opencontrol.fiscal.br", manifest.id)
        assertEquals("Brazilian Fiscal Engine", manifest.name)
        assertEquals("1.0.0", manifest.version)
        assertEquals(1, manifest.dependencies.size)
        assertEquals("org.opencontrol.base", manifest.dependencies[0].pluginId)
    }

    @Test
    fun `should fail validation when plugin ID has invalid format`() {
        val json =
            """
            {
              "id": "INVALID_ID",
              "name": "Test Plugin",
              "version": "1.0.0",
              "vendor": "Test Vendor",
              "minKernelVersion": "1.0.0"
            }
            """.trimIndent()

        assertThrows<PluginValidationException> {
            parser.parse(json.byteInputStream())
        }
    }

    @Test
    fun `should fail validation when SemVer is malformed`() {
        val json =
            """
            {
              "id": "org.opencontrol.test",
              "name": "Test Plugin",
              "version": "not-a-version",
              "vendor": "Test Vendor",
              "minKernelVersion": "1.0.0"
            }
            """.trimIndent()

        assertThrows<PluginValidationException> {
            parser.parse(json.byteInputStream())
        }
    }
}
