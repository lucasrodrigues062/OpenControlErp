package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.EconomicGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface EconomicGroupRepository : JpaRepository<EconomicGroup, UUID> {
    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<EconomicGroup>

    fun findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId: UUID): List<EconomicGroup>

    fun findByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Optional<EconomicGroup>

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<EconomicGroup>

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean
}
