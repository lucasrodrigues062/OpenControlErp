package org.opencontrol.erp.kernel.spi

import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.spi.fiscal.DefaultNoOpFiscalEngine
import org.opencontrol.erp.kernel.spi.fiscal.FiscalDocumentRequest
import org.opencontrol.erp.kernel.spi.fiscal.FiscalEngineSPI
import org.opencontrol.erp.kernel.spi.fiscal.FiscalIssuanceResult
import org.opencontrol.erp.kernel.spi.service.ExtensionRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
class ExtensionRegistryIntegrationTest {
    @Autowired
    private lateinit var extensionRegistry: ExtensionRegistry

    @Test
    fun `should dynamically register, resolve and unregister SPI provider`() {
        val customProvider =
            object : FiscalEngineSPI {
                override fun matches(context: ExecutionContext): Boolean = context.jurisdiction == "BR"

                override fun issueDocument(request: FiscalDocumentRequest) = FiscalIssuanceResult("CUSTOM_BR", "KEY123", "P123", "ok")
            }

        extensionRegistry.registerDefaultFallback(FiscalEngineSPI::class.java, DefaultNoOpFiscalEngine())
        extensionRegistry.registerProvider(
            FiscalEngineSPI::class.java,
            customProvider,
            priority = 500,
            pluginId = "org.opencontrol.custom.fiscal",
        )

        val context = ExecutionContext.defaultFor("BR")
        val resolved = extensionRegistry.resolve(FiscalEngineSPI::class.java, context)
        assertEquals("CUSTOM_BR", resolved.issueDocument(FiscalDocumentRequest("1", "1", 50.0, "111", 1)).status)

        // Unregister plugin providers (simulate deactivation)
        extensionRegistry.unregisterProvidersByPluginId("org.opencontrol.custom.fiscal")

        val fallbackResolved = extensionRegistry.resolve(FiscalEngineSPI::class.java, context)
        assertEquals("SIMULATED", fallbackResolved.issueDocument(FiscalDocumentRequest("1", "1", 50.0, "111", 1)).status)
    }
}
