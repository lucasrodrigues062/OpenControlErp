package org.opencontrol.erp.companies

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.companies.api.CompanyBranchController
import org.opencontrol.erp.companies.api.dto.AddressDto
import org.opencontrol.erp.companies.api.dto.CreateCompanyBranchRequest
import org.opencontrol.erp.companies.api.dto.FiscalSettingsDto
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.domain.TaxRegime
import org.opencontrol.erp.companies.exception.BranchHierarchyException
import org.opencontrol.erp.companies.exception.InvalidCnpjException
import org.opencontrol.erp.companies.repository.BranchAccountantRepository
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.BranchFiscalSettingsRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(authorities = ["COMPANIES_READ", "COMPANIES_MANAGE"])
@Transactional
class QuickstartCompanyBranchEndToEndTest {
    @Autowired
    private lateinit var controller: CompanyBranchController

    @Autowired
    private lateinit var companyBranchRepository: CompanyBranchRepository

    @Autowired
    private lateinit var fiscalSettingsRepository: BranchFiscalSettingsRepository

    @Autowired
    private lateinit var accountantRepository: BranchAccountantRepository

    @Autowired
    private lateinit var certificateRepository: BranchCertificateRepository

    private val tenantId = UUID.fromString("22222222-3333-4444-5555-666666666666")

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "admin-user",
                username = "admin",
                roles = setOf("ADMIN", "COMPANIES_MANAGE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_COMPANIES_E2E", jurisdiction = "BR"),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        certificateRepository.findAll().filter { it.tenantId == tenantId }.forEach { certificateRepository.delete(it) }
        accountantRepository.findAll().filter { it.tenantId == tenantId }.forEach { accountantRepository.delete(it) }
        fiscalSettingsRepository.findAll().filter { it.tenantId == tenantId }.forEach { fiscalSettingsRepository.delete(it) }
        companyBranchRepository.findAll().filter { it.tenantId == tenantId }.forEach { companyBranchRepository.delete(it) }
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `quickstart scenarios - create headquarters, branch, update fiscal, list user branches`() {
        // Scenario 1: Create Headquarters Company (Matriz)
        val hqReq =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "OpenControl Matriz SP",
                legalName = "OpenControl Tecnologia e Gestao Empresarial LTDA",
                cnpj = "11.222.333/0001-81",
                stateRegistration = "111222333444",
                isStateRegistrationExempt = false,
                municipalRegistration = "98765432",
                mainCnae = "6202300",
                phone1 = "(11) 3344-5566",
                email = "contato@opencontrol.com.br",
                responsiblePerson = "Lucas Rodrigues",
                address =
                    AddressDto(
                        postalCode = "01310100",
                        street = "Avenida Paulista",
                        number = "1000",
                        complement = "Sala 101",
                        neighborhood = "Bela Vista",
                        cityName = "Sao Paulo",
                        cityIbgeCode = 3550308,
                        stateCode = "SP",
                        countryCode = "BRA",
                    ),
            )

        val hqRes = controller.createCompany(hqReq)
        assertEquals(HttpStatus.CREATED, hqRes.statusCode)
        val hqDetail = hqRes.body!!
        assertEquals("01", hqDetail.code)
        assertEquals(EstablishmentType.HEADQUARTERS, hqDetail.type)
        assertEquals("11.222.333/0001-81", hqDetail.cnpj)
        assertTrue(hqDetail.isActive)
        assertNotNull(hqDetail.fiscalSettings)

        // Scenario 2: Create Operational Branch (Filial)
        val branchReq =
            CreateCompanyBranchRequest(
                type = EstablishmentType.BRANCH,
                parentId = hqDetail.id,
                code = "02",
                tradeName = "OpenControl Filial Campinas",
                legalName = "OpenControl Tecnologia e Gestao Empresarial LTDA",
                cnpj = "11.222.333/0002-62",
                stateRegistration = "555666777888",
                isStateRegistrationExempt = false,
                municipalRegistration = "12345678",
                mainCnae = "6202300",
                phone1 = "(19) 3322-1100",
                email = "campinas@opencontrol.com.br",
                responsiblePerson = "Carlos Silva",
                address =
                    AddressDto(
                        postalCode = "13010000",
                        street = "Rua Barao de Jaguara",
                        number = "500",
                        neighborhood = "Centro",
                        cityName = "Campinas",
                        cityIbgeCode = 3509502,
                        stateCode = "SP",
                        countryCode = "BRA",
                    ),
            )

        val branchRes = controller.createCompany(branchReq)
        assertEquals(HttpStatus.CREATED, branchRes.statusCode)
        val branchDetail = branchRes.body!!
        assertEquals(EstablishmentType.BRANCH, branchDetail.type)
        assertEquals(hqDetail.id, branchDetail.parentId)
        assertEquals("11.222.333/0002-62", branchDetail.cnpj)

        // Scenario 3: Update Fiscal Settings for Branch
        val fiscalReq =
            FiscalSettingsDto(
                taxRegime = TaxRegime.SIMPLES_NACIONAL,
                icmsSnCreditRate = BigDecimal("2.500"),
                issRate = BigDecimal("5.000"),
                defaultInvoiceMessage = "Mensagem legal teste",
                nfeSeries = "1",
                nfeModel = "55",
            )
        val fiscalRes = controller.updateFiscalSettings(branchDetail.id, fiscalReq)
        assertEquals(HttpStatus.OK, fiscalRes.statusCode)
        assertEquals(TaxRegime.SIMPLES_NACIONAL, fiscalRes.body?.taxRegime)
        assertEquals("Mensagem legal teste", fiscalRes.body?.defaultInvoiceMessage)

        // Scenario 4: User Branches List
        val userBranchesRes = controller.getUserBranches()
        assertEquals(HttpStatus.OK, userBranchesRes.statusCode)
        val userBranches = userBranchesRes.body!!
        assertEquals(2, userBranches.branches.size)
        assertEquals(hqDetail.id, userBranches.activeBranchId)

        // Validation safeguards: cannot deactivate HQ while Branch is active
        val ex =
            assertThrows<BranchHierarchyException> {
                controller.toggleCompanyStatus(hqDetail.id)
            }
        assertTrue(ex.message!!.contains("active branch"))

        // Deactivate branch first
        val toggledBranch = controller.toggleCompanyStatus(branchDetail.id)
        assertFalse(toggledBranch.body!!.isActive)

        // Now HQ can be deactivated
        val toggledHq = controller.toggleCompanyStatus(hqDetail.id)
        assertFalse(toggledHq.body!!.isActive)
    }

    @Test
    fun `should reject invalid CNPJ on create`() {
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "Invalid",
                legalName = "Invalid",
                cnpj = "123456",
                address = AddressDto(cityName = "SP", stateCode = "SP"),
            )

        assertThrows<InvalidCnpjException> {
            controller.createCompany(req)
        }
    }
}
