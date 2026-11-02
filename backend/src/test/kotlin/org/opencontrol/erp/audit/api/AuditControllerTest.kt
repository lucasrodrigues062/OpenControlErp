package org.opencontrol.erp.audit.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.domain.AuditLog
import org.opencontrol.erp.audit.service.AuditService
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuditControllerTest {
    private val auditService = mockk<AuditService>(relaxed = true)
    private val controller = AuditController(auditService)

    @Test
    fun `searchLogs should return 200 with mapped audit DTOs`() {
        val log =
            AuditLog(
                id = UUID.randomUUID(),
                tenantId = UUID.randomUUID(),
                actorId = UUID.randomUUID(),
                actorUsername = "operator",
                action = "CREATE",
                entityType = "Invoice",
                entityId = "INV-001",
                details = "Created invoice",
                stateAfter = "{\"amount\":100}",
                createdAt = Instant.now(),
            )
        every { auditService.queryLogs(any(), any(), any(), any(), any(), any(), any()) } returns PageImpl(listOf(log))

        val response = controller.searchLogs("Invoice", null, null, null, null, null, 0, 20)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.items?.size)
        assertEquals(
            "Invoice",
            response.body
                ?.items
                ?.first()
                ?.entityType,
        )
        assertEquals(
            "operator",
            response.body
                ?.items
                ?.first()
                ?.actorUsername,
        )
    }
}
