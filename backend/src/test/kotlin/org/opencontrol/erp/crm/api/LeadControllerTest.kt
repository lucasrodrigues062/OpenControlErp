package org.opencontrol.erp.crm.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.crm.api.dto.ConvertLeadRequest
import org.opencontrol.erp.crm.api.dto.CreateLeadRequest
import org.opencontrol.erp.crm.api.dto.DisqualifyLeadRequest
import org.opencontrol.erp.crm.api.dto.LeadConversionResponse
import org.opencontrol.erp.crm.api.dto.LeadResponse
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.model.LeadSource
import org.opencontrol.erp.crm.domain.model.LeadStatus
import org.opencontrol.erp.crm.service.LeadService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class LeadControllerTest {
    private val leadService: LeadService = mockk()
    private lateinit var controller: LeadController

    private val sampleLead =
        LeadResponse(
            id = UUID.randomUUID(),
            tenantId = UUID.randomUUID(),
            branchId = null,
            name = "Roberto Mendes",
            companyName = "Mendes Distribuidora",
            email = "roberto@mendes.com.br",
            phone = "11999990000",
            source = LeadSource.WEBSITE,
            status = LeadStatus.NEW,
            estimatedBudget = BigDecimal("35000.00"),
            interestSummary = null,
            assignedUserId = null,
            disqualificationReason = null,
            convertedCustomerId = null,
            convertedDealId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = LeadController(leadService)
    }

    @Test
    fun `should list leads`() {
        every { leadService.listLeads(null, null, null) } returns listOf(sampleLead)

        val response = controller.list(null, null, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `should create lead`() {
        val request = CreateLeadRequest(name = "Roberto Mendes")
        every { leadService.createLead(request) } returns sampleLead

        val response = controller.create(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("Roberto Mendes", response.body?.name)
    }

    @Test
    fun `should disqualify lead`() {
        val disqualified = sampleLead.copy(status = LeadStatus.DISQUALIFIED, disqualificationReason = "Sem verba")
        every { leadService.disqualifyLead(sampleLead.id, "Sem verba") } returns disqualified

        val response = controller.disqualify(sampleLead.id, DisqualifyLeadRequest(reason = "Sem verba"))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(LeadStatus.DISQUALIFIED, response.body?.status)
    }

    @Test
    fun `should convert lead`() {
        val convResp = LeadConversionResponse(sampleLead.id, UUID.randomUUID(), UUID.randomUUID())
        val req =
            ConvertLeadRequest(
                customerType = CustomerType.LEGAL_ENTITY,
                customerDocument = "53066208000170",
                dealTitle = "Oportunidade",
                pipelineId = UUID.randomUUID(),
                stageId = UUID.randomUUID(),
            )

        every { leadService.convertLead(sampleLead.id, req) } returns convResp

        val response = controller.convert(sampleLead.id, req)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(sampleLead.id, response.body?.leadId)
    }
}
