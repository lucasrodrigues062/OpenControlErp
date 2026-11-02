package org.opencontrol.erp.companies.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.domain.AuditLog
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.companies.api.dto.AccountantDto
import org.opencontrol.erp.companies.api.dto.AddressDto
import org.opencontrol.erp.companies.api.dto.CreateCompanyBranchRequest
import org.opencontrol.erp.companies.api.dto.FiscalSettingsDto
import org.opencontrol.erp.companies.api.dto.UpdateCompanyBranchRequest
import org.opencontrol.erp.companies.domain.Address
import org.opencontrol.erp.companies.domain.BranchFiscalSettings
import org.opencontrol.erp.companies.domain.CompanyBranch
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.exception.BranchHierarchyException
import org.opencontrol.erp.companies.exception.DuplicateCnpjException
import org.opencontrol.erp.companies.exception.DuplicateCompanyCodeException
import org.opencontrol.erp.companies.exception.InvalidCnpjException
import org.opencontrol.erp.companies.repository.BranchAccountantRepository
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.BranchFiscalSettingsRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CompanyBranchServiceTest {
    private val companyBranchRepository: CompanyBranchRepository = mockk(relaxed = true)
    private val branchFiscalSettingsRepository: BranchFiscalSettingsRepository = mockk(relaxed = true)
    private val branchAccountantRepository: BranchAccountantRepository = mockk(relaxed = true)
    private val branchCertificateRepository: BranchCertificateRepository = mockk(relaxed = true)
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: CompanyBranchService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            CompanyBranchService(
                companyBranchRepository,
                branchFiscalSettingsRepository,
                branchAccountantRepository,
                branchCertificateRepository,
                auditService,
            )
        SecurityContextHolder.setContext(
            UserContext(
                userId = "admin-1",
                username = "admin",
                roles = setOf("COMPANIES_MANAGE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT1", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `should create Headquarters company successfully`() {
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "OpenControl Matriz",
                legalName = "OpenControl Tecnologia LTDA",
                cnpj = "11.222.333/0001-81",
                address =
                    AddressDto(
                        cityName = "Sao Paulo",
                        stateCode = "SP",
                        cityIbgeCode = 3550308,
                    ),
            )

        every { companyBranchRepository.existsByTenantIdAndCnpj(tenantId, "11222333000181") } returns false
        every { companyBranchRepository.existsByTenantIdAndCode(tenantId, "01") } returns false
        every { companyBranchRepository.save(any()) } answers { firstArg() }
        every { branchFiscalSettingsRepository.save(any()) } answers { firstArg() }
        every { branchFiscalSettingsRepository.findByBranchId(any()) } returns
            Optional.of(
                BranchFiscalSettings(tenantId = tenantId, branchId = UUID.randomUUID()),
            )
        every { branchAccountantRepository.findByBranchId(any()) } returns Optional.empty()
        every { branchCertificateRepository.findByBranchId(any()) } returns Optional.empty()
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val result = service.createCompany(req)

        assertNotNull(result)
        assertEquals("OpenControl Matriz", result.tradeName)
        assertEquals("11.222.333/0001-81", result.cnpj)
        assertEquals(EstablishmentType.HEADQUARTERS, result.type)
        verify { companyBranchRepository.save(any()) }
        verify { branchFiscalSettingsRepository.save(any()) }
        verify {
            auditService.recordAudit(
                entityType = "CompanyBranch",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = null,
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should throw exception when creating company with invalid CNPJ`() {
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "Invalid",
                legalName = "Invalid Corp",
                cnpj = "11111111111111", // Invalid check digits
                address = AddressDto(cityName = "SP", stateCode = "SP"),
            )

        assertThrows<InvalidCnpjException> {
            service.createCompany(req)
        }
    }

    @Test
    fun `should throw exception when creating company with duplicate CNPJ in tenant`() {
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "Duplicate",
                legalName = "Duplicate Corp",
                cnpj = "11.222.333/0001-81",
                address = AddressDto(cityName = "SP", stateCode = "SP"),
            )

        every { companyBranchRepository.existsByTenantIdAndCnpj(tenantId, "11222333000181") } returns true

        assertThrows<DuplicateCnpjException> {
            service.createCompany(req)
        }
    }

    @Test
    fun `should throw exception when creating company with duplicate code in tenant`() {
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                tradeName = "Duplicate",
                legalName = "Duplicate Corp",
                cnpj = "11.222.333/0001-81",
                address = AddressDto(cityName = "SP", stateCode = "SP"),
            )

        every { companyBranchRepository.existsByTenantIdAndCnpj(tenantId, "11222333000181") } returns false
        every { companyBranchRepository.existsByTenantIdAndCode(tenantId, "01") } returns true

        assertThrows<DuplicateCompanyCodeException> {
            service.createCompany(req)
        }
    }

    @Test
    fun `should throw exception when branch does not have parent Headquarters`() {
        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.BRANCH,
                parentId = null,
                code = "02",
                tradeName = "Orphan Branch",
                legalName = "Orphan Branch Corp",
                cnpj = "11.222.333/0002-62",
                address = AddressDto(cityName = "Campinas", stateCode = "SP"),
            )

        every { companyBranchRepository.existsByTenantIdAndCnpj(tenantId, "11222333000262") } returns false
        every { companyBranchRepository.existsByTenantIdAndCode(tenantId, "02") } returns false

        val ex =
            assertThrows<BranchHierarchyException> {
                service.createCompany(req)
            }
        assertTrue(ex.message!!.contains("parent Headquarters"))
    }

    @Test
    fun `should create Branch linked to parent Headquarters`() {
        val parentId = UUID.randomUUID()
        val parent =
            CompanyBranch(
                id = parentId,
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )

        val req =
            CreateCompanyBranchRequest(
                type = EstablishmentType.BRANCH,
                parentId = parentId,
                code = "02",
                tradeName = "Filial Campinas",
                legalName = "Filial Campinas",
                cnpj = "11.222.333/0002-62",
                address = AddressDto(cityName = "Campinas", stateCode = "SP"),
            )

        every { companyBranchRepository.existsByTenantIdAndCnpj(tenantId, "11222333000262") } returns false
        every { companyBranchRepository.existsByTenantIdAndCode(tenantId, "02") } returns false
        every { companyBranchRepository.findByIdAndTenantId(parentId, tenantId) } returns Optional.of(parent)
        every { companyBranchRepository.save(any()) } answers { firstArg() }
        every { branchFiscalSettingsRepository.save(any()) } answers { firstArg() }
        every { branchFiscalSettingsRepository.findByBranchId(any()) } returns
            Optional.of(
                BranchFiscalSettings(tenantId = tenantId, branchId = UUID.randomUUID()),
            )
        every { branchAccountantRepository.findByBranchId(any()) } returns Optional.empty()
        every { branchCertificateRepository.findByBranchId(any()) } returns Optional.empty()
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val result = service.createCompany(req)

        assertNotNull(result)
        assertEquals(EstablishmentType.BRANCH, result.type)
        assertEquals(parentId, result.parentId)
    }

    @Test
    fun `should update company details successfully`() {
        val id = UUID.randomUUID()
        val branch =
            CompanyBranch(
                id = id,
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Old Legal",
                tradeName = "Old Trade",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )

        every { companyBranchRepository.findByIdAndTenantId(id, tenantId) } returns Optional.of(branch)
        every { companyBranchRepository.save(any()) } answers { firstArg() }
        every { branchFiscalSettingsRepository.findByBranchId(id) } returns Optional.empty()
        every { branchAccountantRepository.findByBranchId(id) } returns Optional.empty()
        every { branchCertificateRepository.findByBranchId(id) } returns Optional.empty()
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val updateReq =
            UpdateCompanyBranchRequest(
                tradeName = "Updated Trade",
                legalName = "Updated Legal",
                address = AddressDto(cityName = "Santos", stateCode = "SP"),
            )

        val updated = service.updateCompany(id, updateReq)

        assertEquals("Updated Trade", updated.tradeName)
        assertEquals("Santos", updated.address.cityName)
        verify {
            auditService.recordAudit(
                entityType = "CompanyBranch",
                entityId = id.toString(),
                action = "UPDATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should prevent deactivation of Headquarters with active branches`() {
        val id = UUID.randomUUID()
        val branch =
            CompanyBranch(
                id = id,
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
                isActive = true,
            )

        every { companyBranchRepository.findByIdAndTenantId(id, tenantId) } returns Optional.of(branch)
        every { companyBranchRepository.countByTenantIdAndParentIdAndIsActiveTrue(tenantId, id) } returns 2L

        val ex =
            assertThrows<BranchHierarchyException> {
                service.toggleStatus(id)
            }
        assertTrue(ex.message!!.contains("2 active branch(es)"))
    }

    @Test
    fun `should toggle status successfully when no active children`() {
        val id = UUID.randomUUID()
        val branch =
            CompanyBranch(
                id = id,
                tenantId = tenantId,
                type = EstablishmentType.BRANCH,
                code = "02",
                legalName = "Filial",
                tradeName = "Filial",
                cnpj = "11222333000262",
                address = Address(cityName = "Campinas", stateCode = "SP"),
                isActive = true,
            )

        every { companyBranchRepository.findByIdAndTenantId(id, tenantId) } returns Optional.of(branch)
        every { companyBranchRepository.save(any()) } answers { firstArg() }
        every { branchFiscalSettingsRepository.findByBranchId(id) } returns Optional.empty()
        every { branchAccountantRepository.findByBranchId(id) } returns Optional.empty()
        every { branchCertificateRepository.findByBranchId(id) } returns Optional.empty()
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val toggled = service.toggleStatus(id)
        assertFalse(toggled.isActive)
        verify {
            auditService.recordAudit(
                entityType = "CompanyBranch",
                entityId = id.toString(),
                action = "STATUS_CHANGE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should update fiscal settings and accountant`() {
        val id = UUID.randomUUID()
        val branch =
            CompanyBranch(
                id = id,
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )

        every { companyBranchRepository.findByIdAndTenantId(id, tenantId) } returns Optional.of(branch)
        every { branchFiscalSettingsRepository.findByBranchIdAndTenantId(id, tenantId) } returns Optional.empty()
        every { branchFiscalSettingsRepository.save(any()) } answers { firstArg() }
        every { branchAccountantRepository.findByBranchIdAndTenantId(id, tenantId) } returns Optional.empty()
        every { branchAccountantRepository.save(any()) } answers { firstArg() }
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val fiscalRes = service.updateFiscalSettings(id, FiscalSettingsDto(defaultInvoiceMessage = "Mensagem"))
        assertEquals("Mensagem", fiscalRes.defaultInvoiceMessage)

        val accountantRes = service.updateAccountant(id, AccountantDto(name = "Contador Silva", crc = "SP-123456"))
        assertEquals("Contador Silva", accountantRes.name)
        assertEquals("SP-123456", accountantRes.crc)
    }
}
