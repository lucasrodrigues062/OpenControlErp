package org.opencontrol.erp.kernel.spi

import java.util.UUID

data class ExecutionContext(
    val tenantId: UUID,
    val jurisdiction: String = "BR",
    val channel: String? = null,
    val properties: Map<String, Any> = emptyMap(),
) {
    companion object {
        fun defaultFor(jurisdiction: String = "BR"): ExecutionContext =
            ExecutionContext(
                tenantId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
                jurisdiction = jurisdiction,
            )
    }
}
