package org.opencontrol.erp.crm.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.crm.api.dto.CreateCrmActivityRequest
import org.opencontrol.erp.crm.api.dto.CrmActivityResponse
import org.opencontrol.erp.crm.domain.model.ActivityEntityType
import org.opencontrol.erp.crm.domain.model.ActivityType
import org.opencontrol.erp.crm.service.CrmActivityService
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID

class CrmActivityControllerTest {
    private val service: CrmActivityService = mockk()
    private lateinit var controller: CrmActivityController

    private val customerId = UUID.randomUUID()
    private val sampleActivity =
        CrmActivityResponse(
            id = UUID.randomUUID(),
            tenantId = UUID.randomUUID(),
            entityType = ActivityEntityType.CUSTOMER,
            entityId = customerId,
            activityType = ActivityType.WHATSAPP,
            summary = "Enviada tabela de preços",
            description = "Cliente recebeu e irá avaliar com a diretoria.",
            performedAt = Instant.now(),
            dueDate = null,
            isCompleted = true,
            authorUserId = null,
            createdAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = CrmActivityController(service)
    }

    @Test
    fun `should list activities for target entity`() {
        every { service.listActivities(ActivityEntityType.CUSTOMER, customerId) } returns listOf(sampleActivity)

        val response = controller.list(ActivityEntityType.CUSTOMER, customerId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `should log new activity`() {
        val req =
            CreateCrmActivityRequest(
                entityType = ActivityEntityType.CUSTOMER,
                entityId = customerId,
                activityType = ActivityType.WHATSAPP,
                summary = "Enviada tabela de preços",
            )
        every { service.createActivity(req) } returns sampleActivity

        val response = controller.create(req)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(ActivityType.WHATSAPP, response.body?.activityType)
    }
}
