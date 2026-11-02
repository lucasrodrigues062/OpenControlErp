package org.opencontrol.erp.companies.repository

import org.opencontrol.erp.companies.domain.BranchFiscalSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface BranchFiscalSettingsRepository : JpaRepository<BranchFiscalSettings, UUID> {
    fun findByBranchIdAndTenantId(
        branchId: UUID,
        tenantId: UUID,
    ): Optional<BranchFiscalSettings>

    fun findByBranchId(branchId: UUID): Optional<BranchFiscalSettings>

    fun deleteByBranchId(branchId: UUID)
}
