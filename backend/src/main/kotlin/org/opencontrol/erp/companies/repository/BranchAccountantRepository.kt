package org.opencontrol.erp.companies.repository

import org.opencontrol.erp.companies.domain.BranchAccountant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface BranchAccountantRepository : JpaRepository<BranchAccountant, UUID> {
    fun findByBranchIdAndTenantId(
        branchId: UUID,
        tenantId: UUID,
    ): Optional<BranchAccountant>

    fun findByBranchId(branchId: UUID): Optional<BranchAccountant>

    fun deleteByBranchId(branchId: UUID)
}
