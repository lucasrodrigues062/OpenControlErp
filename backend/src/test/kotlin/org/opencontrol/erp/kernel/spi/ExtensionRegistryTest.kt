package org.opencontrol.erp.kernel.spi

import org.junit.jupiter.api.Test
import org.opencontrol.erp.kernel.spi.fiscal.DefaultNoOpFiscalEngine
import org.opencontrol.erp.kernel.spi.fiscal.FiscalDocumentRequest
import org.opencontrol.erp.kernel.spi.fiscal.FiscalEngineSPI
import org.opencontrol.erp.kernel.spi.fiscal.FiscalIssuanceResult
import org.opencontrol.erp.kernel.spi.service.ExtensionRegistry
import kotlin.test.assertEquals

class ExtensionRegistryTest {
    private val registry = ExtensionRegistry()

    @Test
    fun `should resolve highest priority provider matching context`() {
        val lowPriorityProvider =
            object : FiscalEngineSPI {
                override fun matches(context: ExecutionContext): Boolean = context.jurisdiction == "BR"

                override fun issueDocument(request: FiscalDocumentRequest) = FiscalIssuanceResult("LOW", null, null, "low")
            }

        val highPriorityProvider =
            object : FiscalEngineSPI {
                override fun matches(context: ExecutionContext): Boolean = context.jurisdiction == "BR"

                override fun issueDocument(request: FiscalDocumentRequest) = FiscalIssuanceResult("HIGH", null, null, "high")
            }

        registry.registerProvider(FiscalEngineSPI::class.java, lowPriorityProvider, priority = 50, pluginId = "plugin.low")
        registry.registerProvider(FiscalEngineSPI::class.java, highPriorityProvider, priority = 200, pluginId = "plugin.high")

        val context = ExecutionContext.defaultFor("BR")
        val resolved = registry.resolve(FiscalEngineSPI::class.java, context)
        val result = resolved.issueDocument(FiscalDocumentRequest("1", "1", 100.0, "12345678000199", 1))

        assertEquals("HIGH", result.status)
    }

    @Test
    fun `should fall back to default when no plugin provider matches context`() {
        registry.registerDefaultFallback(FiscalEngineSPI::class.java, DefaultNoOpFiscalEngine())

        val context = ExecutionContext.defaultFor("US")
        val resolved = registry.resolve(FiscalEngineSPI::class.java, context)
        val result = resolved.issueDocument(FiscalDocumentRequest("1", "1", 100.0, "12345678000199", 1))

        assertEquals("SIMULATED", result.status)
    }
}
