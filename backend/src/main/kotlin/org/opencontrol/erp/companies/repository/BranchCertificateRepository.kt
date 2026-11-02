package org.opencontrol.erp.companies.repository

import org.opencontrol.erp.companies.domain.BranchCertificate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface BranchCertificateRepository : JpaRepository<BranchCertificate, UUID> {
    fun findByBranchIdAndTenantId(
        branchId: UUID,
        tenantId: UUID,
    ): Optional<BranchCertificate>

    fun findByBranchId(branchId: UUID): Optional<BranchCertificate>

    fun deleteByBranchId(branchId: UUID)
}
