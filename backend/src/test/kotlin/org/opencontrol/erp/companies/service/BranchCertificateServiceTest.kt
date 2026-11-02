package org.opencontrol.erp.companies.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.domain.AuditLog
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.companies.domain.Address
import org.opencontrol.erp.companies.domain.BranchCertificate
import org.opencontrol.erp.companies.domain.CertificateType
import org.opencontrol.erp.companies.domain.CompanyBranch
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.exception.CertificateProcessingException
import org.opencontrol.erp.companies.exception.CompanyNotFoundException
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BranchCertificateServiceTest {
    private val companyBranchRepository: CompanyBranchRepository = mockk(relaxed = true)
    private val branchCertificateRepository: BranchCertificateRepository = mockk(relaxed = true)
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: BranchCertificateService
    private val tenantId = UUID.randomUUID()
    private val branchId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            BranchCertificateService(
                companyBranchRepository,
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
    fun `should throw exception when branch not found`() {
        every { companyBranchRepository.findByIdAndTenantId(branchId, tenantId) } returns Optional.empty()

        assertThrows<CompanyNotFoundException> {
            service.getCertificateMetadata(branchId)
        }
    }

    @Test
    fun `should return certificate metadata when present`() {
        val branch =
            CompanyBranch(
                id = branchId,
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )

        val cert =
            BranchCertificate(
                tenantId = tenantId,
                branchId = branchId,
                certificateType = CertificateType.A1,
                certificateData = byteArrayOf(1, 2, 3),
                passwordEncrypted = "pwd",
                serialNumber = "123456",
                subjectName = "CN=Test Enterprise",
                issuerName = "CN=Certisign",
                cnpj = "11222333000181",
                validFrom = Instant.now().minus(10, ChronoUnit.DAYS),
                validUntil = Instant.now().plus(300, ChronoUnit.DAYS),
            )

        every { companyBranchRepository.findByIdAndTenantId(branchId, tenantId) } returns Optional.of(branch)
        every { branchCertificateRepository.findByBranchIdAndTenantId(branchId, tenantId) } returns Optional.of(cert)

        val meta = service.getCertificateMetadata(branchId)

        assertEquals("A1", meta.certificateType)
        assertEquals("123456", meta.serialNumber)
        assertFalse(meta.isExpired)
        assertTrue(meta.daysUntilExpiration > 0)
    }

    @Test
    fun `should throw CertificateProcessingException for invalid certificate file`() {
        val branch =
            CompanyBranch(
                id = branchId,
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )

        every { companyBranchRepository.findByIdAndTenantId(branchId, tenantId) } returns Optional.of(branch)

        assertThrows<CertificateProcessingException> {
            service.uploadCertificate(branchId, byteArrayOf(0, 1, 2), "wrong-password")
        }
    }

    @Test
    fun `should delete certificate successfully`() {
        val cert =
            BranchCertificate(
                tenantId = tenantId,
                branchId = branchId,
                certificateType = CertificateType.A1,
                certificateData = byteArrayOf(1, 2, 3),
                passwordEncrypted = "pwd",
                validUntil = Instant.now(),
            )

        every { branchCertificateRepository.findByBranchIdAndTenantId(branchId, tenantId) } returns Optional.of(cert)
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        service.deleteCertificate(branchId)

        verify { branchCertificateRepository.delete(cert) }
    }
}
