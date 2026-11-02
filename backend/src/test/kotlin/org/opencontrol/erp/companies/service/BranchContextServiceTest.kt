package org.opencontrol.erp.companies.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.companies.domain.Address
import org.opencontrol.erp.companies.domain.CompanyBranch
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.companies.repository.UserBranchAccessRepository
import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BranchContextServiceTest {
    private val companyBranchRepository: CompanyBranchRepository = mockk(relaxed = true)
    private val userBranchAccessRepository: UserBranchAccessRepository = mockk(relaxed = true)
    private val branchCertificateRepository: BranchCertificateRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)

    private lateinit var service: BranchContextService
    private val tenantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            BranchContextService(
                companyBranchRepository,
                userBranchAccessRepository,
                branchCertificateRepository,
                userRepository,
            )
    }

    @Test
    fun `should return all active branches for admin user`() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = userId.toString(),
                username = "admin",
                roles = setOf("ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT1", jurisdiction = "BR"),
            ),
        )

        val branch1 =
            CompanyBranch(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )
        val branch2 =
            CompanyBranch(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                type = EstablishmentType.BRANCH,
                code = "02",
                legalName = "Filial",
                tradeName = "Filial",
                cnpj = "11222333000262",
                address = Address(cityName = "Campinas", stateCode = "SP"),
            )

        every { companyBranchRepository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId) } returns listOf(branch1, branch2)
        every { branchCertificateRepository.findByBranchId(any()) } returns Optional.empty()

        val response = service.getUserBranches()

        assertNotNull(response)
        assertEquals(2, response.branches.size)
        assertEquals(branch1.id, response.activeBranchId)
    }

    @Test
    fun `should filter branches for restricted user`() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = userId.toString(),
                username = "operator",
                roles = setOf("USERS_READ"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT1", jurisdiction = "BR"),
            ),
        )

        val branch1 =
            CompanyBranch(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                type = EstablishmentType.HEADQUARTERS,
                code = "01",
                legalName = "Matriz",
                tradeName = "Matriz",
                cnpj = "11222333000181",
                address = Address(cityName = "SP", stateCode = "SP"),
            )
        val branch2 =
            CompanyBranch(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                type = EstablishmentType.BRANCH,
                code = "02",
                legalName = "Filial",
                tradeName = "Filial",
                cnpj = "11222333000262",
                address = Address(cityName = "Campinas", stateCode = "SP"),
            )

        val mockUser =
            User(
                id = userId,
                tenantId = tenantId,
                username = "operator",
                email = "op@test.com",
                passwordHash = "hash",
                fullName = "Operator",
                profile = Profile(tenantId = tenantId, name = "Operator"),
            )

        every { companyBranchRepository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId) } returns listOf(branch1, branch2)
        every { userRepository.findByTenantIdAndUsername(tenantId, "operator") } returns mockUser
        every { userBranchAccessRepository.findBranchIdsByUserId(userId) } returns listOf(branch2.id)
        every { branchCertificateRepository.findByBranchId(any()) } returns Optional.empty()

        val response = service.getUserBranches()

        assertEquals(1, response.branches.size)
        assertEquals(branch2.id, response.branches[0].id)
        assertEquals(branch2.id, response.activeBranchId)
    }
}
