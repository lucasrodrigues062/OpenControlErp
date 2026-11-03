package org.opencontrol.erp.crm.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateCrmActivityRequest
import org.opencontrol.erp.crm.domain.model.ActivityEntityType
import org.opencontrol.erp.crm.domain.model.ActivityType
import org.opencontrol.erp.crm.domain.model.CrmActivity
import org.opencontrol.erp.crm.repository.CrmActivityRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.time.Instant
import java.util.UUID

class CrmActivityServiceTest {
    private val repository: CrmActivityRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)
    private lateinit var service: CrmActivityService

    private val tenantId = UUID.randomUUID()
    private val customerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("CRM_ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = CrmActivityService(repository, auditService)
    }

    @Test
    fun `should create activity on customer`() {
        val request =
            CreateCrmActivityRequest(
                entityType = ActivityEntityType.CUSTOMER,
                entityId = customerId,
                activityType = ActivityType.CALL,
                summary = "Ligação de alinhamento comercial",
                description = "Cliente confirmou interesse na proposta.",
            )

        every { repository.save(any()) } answers { firstArg() }

        val response = service.createActivity(request)

        assertNotNull(response)
        assertEquals(ActivityType.CALL, response.activityType)
        assertEquals(ActivityEntityType.CUSTOMER, response.entityType)
        assertEquals(customerId, response.entityId)
        assertEquals("Ligação de alinhamento comercial", response.summary)
    }

    @Test
    fun `should list activities in descending chronological order`() {
        val act1 =
            CrmActivity(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                entityType = ActivityEntityType.CUSTOMER,
                entityId = customerId,
                activityType = ActivityType.NOTE,
                summary = "Anotação 1",
                performedAt = Instant.now(),
            )

        every {
            repository.findAllByTenantIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(tenantId, ActivityEntityType.CUSTOMER, customerId)
        } returns listOf(act1)

        val list = service.listActivities(ActivityEntityType.CUSTOMER, customerId)

        assertEquals(1, list.size)
        assertEquals("Anotação 1", list.first().summary)
    }
}
