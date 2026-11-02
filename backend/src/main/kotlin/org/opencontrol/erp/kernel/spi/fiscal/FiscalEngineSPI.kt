package org.opencontrol.erp.kernel.spi.fiscal

import org.opencontrol.erp.kernel.spi.ContextMatcher
import org.opencontrol.erp.kernel.spi.ExecutionContext
import org.opencontrol.erp.kernel.spi.ExtensionPoint
import org.springframework.stereotype.Component

data class FiscalDocumentRequest(
    val documentNumber: String,
    val series: String,
    val totalAmount: Double,
    val recipientDocument: String,
    val itemsCount: Int,
)

data class FiscalIssuanceResult(
    val status: String,
    val accessKey: String?,
    val protocolNumber: String?,
    val message: String,
)

interface FiscalEngineSPI :
    ExtensionPoint,
    ContextMatcher {
    fun issueDocument(request: FiscalDocumentRequest): FiscalIssuanceResult
}

@Component
class DefaultNoOpFiscalEngine : FiscalEngineSPI {
    override fun matches(context: ExecutionContext): Boolean = true

    override fun issueDocument(request: FiscalDocumentRequest): FiscalIssuanceResult =
        FiscalIssuanceResult(
            status = "SIMULATED",
            accessKey = "SIMULATED-KEY-${request.documentNumber}",
            protocolNumber = "NOOP-PROTO-001",
            message = "Simulated document issuance by core default fallback engine",
        )
}
