package org.opencontrol.erp.companies.service

import org.opencontrol.erp.companies.api.dto.CompanyBranchSummaryResponse
import org.opencontrol.erp.companies.api.dto.UserBranchesResponse
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.companies.repository.UserBranchAccessRepository
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BranchContextService(
    private val companyBranchRepository: CompanyBranchRepository,
    private val userBranchAccessRepository: UserBranchAccessRepository,
    private val branchCertificateRepository: BranchCertificateRepository,
    private val userRepository: UserRepository,
) {
    fun getUserBranches(): UserBranchesResponse {
        val context =
            SecurityContextHolder.getContext()
                ?: throw IllegalStateException("Security context not found")
        val tenantId =
            context.tenant?.tenantId
                ?: throw IllegalStateException("Tenant context not found")
        val username = context.username
        val roles = context.roles

        val allTenantBranches = companyBranchRepository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId)

        val hasGlobalAccess =
            roles.contains("ADMIN") ||
                roles.contains("COMPANIES_MANAGE") ||
                roles.contains("COMPANIES_READ")

        val accessibleBranches =
            if (hasGlobalAccess) {
                allTenantBranches
            } else {
                val user = userRepository.findByTenantIdAndUsername(tenantId, username)
                if (user != null) {
                    val branchIds = userBranchAccessRepository.findBranchIdsByUserId(user.id).toSet()
                    allTenantBranches.filter { branchIds.contains(it.id) }
                } else {
                    allTenantBranches
                }
            }

        val summaries =
            accessibleBranches.map { branch ->
                val cert = branchCertificateRepository.findByBranchId(branch.id).orElse(null)
                CompanyBranchSummaryResponse(
                    id = branch.id,
                    type = branch.type,
                    parentId = branch.parentId,
                    code = branch.code,
                    tradeName = branch.tradeName,
                    legalName = branch.legalName,
                    cnpj = CnpjValidator.format(branch.cnpj),
                    cityName = branch.address.cityName,
                    stateCode = branch.address.stateCode,
                    isActive = branch.isActive,
                    certificateValidUntil = cert?.validUntil,
                    isCertificateExpired = cert?.isExpired(),
                )
            }

        // Active branch: first default or first accessible branch
        val defaultBranchId = summaries.firstOrNull()?.id

        return UserBranchesResponse(
            activeBranchId = defaultBranchId,
            branches = summaries,
        )
    }
}
