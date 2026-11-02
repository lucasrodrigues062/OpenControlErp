package org.opencontrol.erp.companies.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.companies.api.dto.AddressDto
import org.opencontrol.erp.companies.api.dto.CompanyBranchDetailResponse
import org.opencontrol.erp.companies.api.dto.CompanyBranchSummaryResponse
import org.opencontrol.erp.companies.api.dto.CreateCompanyBranchRequest
import org.opencontrol.erp.companies.api.dto.FiscalSettingsDto
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.service.BranchCertificateService
import org.opencontrol.erp.companies.service.BranchContextService
import org.opencontrol.erp.companies.service.CompanyBranchService
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CompanyBranchControllerTest {
    private val companyBranchService: CompanyBranchService = mockk(relaxed = true)
    private val branchCertificateService: BranchCertificateService = mockk(relaxed = true)
    private val branchContextService: BranchContextService = mockk(relaxed = true)

    private lateinit var controller: CompanyBranchController

    @BeforeEach
    fun setUp() {
        controller =
            CompanyBranchController(
                companyBranchService,
                branchCertificateService,
                branchContextService,
            )
    }

    @Test
    fun `should list companies`() {
        val summary =
            CompanyBranchSummaryResponse(
                id = UUID.randomUUID(),
                type = EstablishmentType.HEADQUARTERS,
                parentId = null,
                code = "01",
                tradeName = "Matriz",
                legalName = "Matriz LTDA",
                cnpj = "11.222.333/0001-81",
                cityName = "Sao Paulo",
                stateCode = "SP",
                isActive = true,
            )
        every { companyBranchService.listCompanies(any(), any()) } returns listOf(summary)

        val res = controller.listCompanies(null, false)

        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals(1, res.body?.size)
    }

    @Test
    fun `should create company`() {
        val id = UUID.randomUUID()
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "Matriz",
                legalName = "Matriz LTDA",
                cnpj = "11.222.333/0001-81",
                address = AddressDto(cityName = "SP", stateCode = "SP"),
            )

        val detail =
            CompanyBranchDetailResponse(
                id = id,
                type = EstablishmentType.HEADQUARTERS,
                parentId = null,
                code = "01",
                tradeName = "Matriz",
                legalName = "Matriz LTDA",
                cnpj = "11.222.333/0001-81",
                stateRegistration = null,
                isStateRegistrationExempt = false,
                municipalRegistration = null,
                mainCnae = null,
                nfseCnae = null,
                phone1 = null,
                phone2 = null,
                email = null,
                responsiblePerson = null,
                website = null,
                address = req.address,
                isActive = true,
                fiscalSettings = null,
                accountant = null,
                certificate = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { companyBranchService.createCompany(req) } returns detail

        val res = controller.createCompany(req)

        assertEquals(HttpStatus.CREATED, res.statusCode)
        assertNotNull(res.body)
        assertEquals(id, res.body?.id)
    }

    @Test
    fun `should get company by id and toggle status`() {
        val id = UUID.randomUUID()
        val detail =
            CompanyBranchDetailResponse(
                id = id,
                type = EstablishmentType.HEADQUARTERS,
                parentId = null,
                code = "01",
                tradeName = "Matriz",
                legalName = "Matriz LTDA",
                cnpj = "11.222.333/0001-81",
                stateRegistration = null,
                isStateRegistrationExempt = false,
                municipalRegistration = null,
                mainCnae = null,
                nfseCnae = null,
                phone1 = null,
                phone2 = null,
                email = null,
                responsiblePerson = null,
                website = null,
                address = AddressDto(cityName = "SP", stateCode = "SP"),
                isActive = true,
                fiscalSettings = null,
                accountant = null,
                certificate = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        every { companyBranchService.getCompany(id) } returns detail
        every { companyBranchService.toggleStatus(id) } returns detail

        val resGet = controller.getCompanyById(id)
        val resToggle = controller.toggleCompanyStatus(id)

        assertEquals(HttpStatus.OK, resGet.statusCode)
        assertEquals(HttpStatus.OK, resToggle.statusCode)
    }

    @Test
    fun `should update fiscal settings`() {
        val id = UUID.randomUUID()
        val req = FiscalSettingsDto(defaultInvoiceMessage = "Test message")
        every { companyBranchService.updateFiscalSettings(id, req) } returns req

        val res = controller.updateFiscalSettings(id, req)
        assertEquals(HttpStatus.OK, res.statusCode)
        assertEquals("Test message", res.body?.defaultInvoiceMessage)
    }
}
