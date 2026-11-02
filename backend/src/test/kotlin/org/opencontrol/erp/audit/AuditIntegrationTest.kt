package org.opencontrol.erp.audit

import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.repository.AuditLogRepository
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditIntegrationTest {
    @Autowired
    private lateinit var auditService: AuditService

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Test
    fun `should persist and query immutable audit log records`() {
        val tenantId = UUID.randomUUID()
        val actorId = UUID.randomUUID()
        SecurityContextHolder.setContext(
            UserContext(
                userId = actorId.toString(),
                username = "auditor.test",
                roles = setOf("AUDIT_READ"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "AUDIT_TEST", jurisdiction = "BR"),
            ),
        )

        val uniqueEntityId = "ORDER-${UUID.randomUUID().toString().take(8)}"
        val savedLog =
            auditService.recordAudit(
                entityType = "CustomerOrder",
                entityId = uniqueEntityId,
                action = "CREATE",
                details = "Order created with total 500.0",
                stateAfter = mapOf("orderId" to uniqueEntityId, "total" to 500.0),
            )

        assertNotNull(savedLog.id)
        assertEquals("auditor.test", savedLog.actorUsername)
        assertEquals("CustomerOrder", savedLog.entityType)

        val queryResult =
            auditService.queryLogs(
                entityType = "CustomerOrder",
                entityId = uniqueEntityId,
                actorId = null,
                action = "CREATE",
                fromDate = null,
                toDate = null,
                pageable = PageRequest.of(0, 10),
            )

        assertEquals(1, queryResult.totalElements)
        val firstItem = queryResult.content.first()
        assertEquals(uniqueEntityId, firstItem.entityId)
        assertNotNull(firstItem.stateAfter)
    }
}
