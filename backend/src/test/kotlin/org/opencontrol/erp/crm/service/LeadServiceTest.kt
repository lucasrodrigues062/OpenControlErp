package org.opencontrol.erp.crm.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.ConvertLeadRequest
import org.opencontrol.erp.crm.api.dto.CreateLeadRequest
import org.opencontrol.erp.crm.api.dto.CustomerResponse
import org.opencontrol.erp.crm.api.dto.DealResponse
import org.opencontrol.erp.crm.domain.events.LeadConvertedEvent
import org.opencontrol.erp.crm.domain.model.CreditStatus
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.opencontrol.erp.crm.domain.model.IcmsTaxpayerType
import org.opencontrol.erp.crm.domain.model.Lead
import org.opencontrol.erp.crm.domain.model.LeadSource
import org.opencontrol.erp.crm.domain.model.LeadStatus
import org.opencontrol.erp.crm.exception.InvalidLeadTransitionException
import org.opencontrol.erp.crm.repository.LeadRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class LeadServiceTest {
    private val leadRepository: LeadRepository = mockk()
    private val customerService: CustomerService = mockk()
    private val dealService: DealService = mockk()
    private val auditService: AuditService = mockk(relaxed = true)
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)

    private lateinit var leadService: LeadService

    private val tenantId = UUID.randomUUID()

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
        leadService =
            LeadService(
                leadRepository = leadRepository,
                customerService = customerService,
                dealService = dealService,
                auditService = auditService,
                eventPublisher = eventPublisher,
            )
    }

    @Test
    fun `should create lead with status NEW`() {
        val request =
            CreateLeadRequest(
                name = "Roberto Mendes",
                companyName = "Mendes Distribuidora",
                email = "roberto@mendes.com.br",
                phone = "11999990000",
                source = LeadSource.WEBSITE,
                estimatedBudget = BigDecimal("35000.00"),
            )

        every { leadRepository.save(any()) } answers { firstArg() }

        val response = leadService.createLead(request)

        assertNotNull(response)
        assertEquals("Roberto Mendes", response.name)
        assertEquals(LeadStatus.NEW, response.status)
        assertEquals(BigDecimal("35000.00"), response.estimatedBudget)
    }

    @Test
    fun `should disqualify lead with reason`() {
        val leadId = UUID.randomUUID()
        val lead =
            Lead(
                id = leadId,
                tenantId = tenantId,
                name = "Lead Desqualificado",
                status = LeadStatus.IN_QUALIFICATION,
            )

        every { leadRepository.findByIdAndTenantId(leadId, tenantId) } returns Optional.of(lead)
        every { leadRepository.save(any()) } answers { firstArg() }

        val response = leadService.disqualifyLead(leadId, "Sem orçamento para este ano")

        assertEquals(LeadStatus.DISQUALIFIED, response.status)
        assertEquals("Sem orçamento para este ano", response.disqualificationReason)
    }

    @Test
    fun `should throw when disqualifying lead with blank reason`() {
        val leadId = UUID.randomUUID()
        val lead = Lead(id = leadId, tenantId = tenantId, name = "Lead Test")
        every { leadRepository.findByIdAndTenantId(leadId, tenantId) } returns Optional.of(lead)

        assertThrows<IllegalArgumentException> {
            leadService.disqualifyLead(leadId, "   ")
        }
    }

    @Test
    fun `should convert lead into Customer and Deal in one click`() {
        val leadId = UUID.randomUUID()
        val pipelineId = UUID.randomUUID()
        val stageId = UUID.randomUUID()
        val customerId = UUID.randomUUID()
        val dealId = UUID.randomUUID()

        val lead =
            Lead(
                id = leadId,
                tenantId = tenantId,
                name = "Carlos SDR",
                companyName = "Carlos Comércio",
                email = "carlos@comercio.com",
                phone = "11988887777",
                status = LeadStatus.QUALIFIED,
                estimatedBudget = BigDecimal("50000.00"),
            )

        every { leadRepository.findByIdAndTenantId(leadId, tenantId) } returns Optional.of(lead)
        every { leadRepository.save(any()) } answers { firstArg() }

        val mockCustomer =
            CustomerResponse(
                id = customerId,
                tenantId = tenantId,
                branchId = null,
                type = CustomerType.LEGAL_ENTITY,
                document = "53066208000170",
                formattedDocument = "53.066.208/0001-70",
                legalName = "Carlos Comércio Ltda",
                tradeName = "Carlos Comércio",
                stateRegistration = null,
                isStateRegistrationExempt = false,
                municipalRegistration = null,
                icmsTaxpayerType = IcmsTaxpayerType.NON_CONTRIBUTOR,
                mainCnae = null,
                birthOrFoundingDate = null,
                phone1 = "11988887777",
                phone2 = null,
                email = "carlos@comercio.com",
                contactPerson = "Carlos SDR",
                website = null,
                categoryId = null,
                categoryName = null,
                categoryColorHex = null,
                economicGroupId = null,
                economicGroupName = null,
                creditLimit = BigDecimal("50000.00"),
                creditStatus = CreditStatus.APPROVED,
                notes = null,
                isActive = true,
                postalCode = null,
                street = null,
                number = null,
                complement = null,
                neighborhood = null,
                cityName = null,
                cityIbgeCode = null,
                stateCode = null,
                countryCode = "BRA",
                billingPostalCode = null,
                billingStreet = null,
                billingNumber = null,
                billingComplement = null,
                billingNeighborhood = null,
                billingCityName = null,
                billingStateCode = null,
                deliveryPostalCode = null,
                deliveryStreet = null,
                deliveryNumber = null,
                deliveryComplement = null,
                deliveryNeighborhood = null,
                deliveryCityName = null,
                deliveryStateCode = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        val mockDeal =
            DealResponse(
                id = dealId,
                tenantId = tenantId,
                branchId = null,
                pipelineId = pipelineId,
                stageId = stageId,
                stageName = "Proposta",
                stageColorHex = "#F59E0B",
                customerId = customerId,
                customerName = "Carlos Comércio Ltda",
                leadId = leadId,
                title = "Implantação ERP",
                estimatedValue = BigDecimal("50000.00"),
                currency = "BRL",
                expectedCloseDate = null,
                status = DealStatus.OPEN,
                lossReason = null,
                assignedUserId = null,
                closedAt = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { customerService.createCustomer(any()) } returns mockCustomer
        every { dealService.createDeal(any()) } returns mockDeal

        val convertRequest =
            ConvertLeadRequest(
                customerType = CustomerType.LEGAL_ENTITY,
                customerDocument = "53066208000170",
                customerLegalName = "Carlos Comércio Ltda",
                dealTitle = "Implantação ERP",
                dealEstimatedValue = BigDecimal("50000.00"),
                pipelineId = pipelineId,
                stageId = stageId,
            )

        val response = leadService.convertLead(leadId, convertRequest)

        assertEquals(leadId, response.leadId)
        assertEquals(customerId, response.customerId)
        assertEquals(dealId, response.dealId)
        assertEquals(LeadStatus.CONVERTED, lead.status)
        verify { eventPublisher.publishEvent(any<LeadConvertedEvent>()) }
    }

    @Test
    fun `should reject conversion of already converted lead`() {
        val leadId = UUID.randomUUID()
        val lead =
            Lead(
                id = leadId,
                tenantId = tenantId,
                name = "Lead Ja Convertido",
                status = LeadStatus.CONVERTED,
            )
        every { leadRepository.findByIdAndTenantId(leadId, tenantId) } returns Optional.of(lead)

        val convertRequest =
            ConvertLeadRequest(
                customerType = CustomerType.LEGAL_ENTITY,
                customerDocument = "53066208000170",
                dealTitle = "Qualquer",
                dealEstimatedValue = BigDecimal("1000.00"),
                pipelineId = UUID.randomUUID(),
                stageId = UUID.randomUUID(),
            )

        assertThrows<InvalidLeadTransitionException> {
            leadService.convertLead(leadId, convertRequest)
        }
    }
}
