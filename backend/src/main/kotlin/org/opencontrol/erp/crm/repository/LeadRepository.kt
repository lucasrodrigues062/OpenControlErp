package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.Lead
import org.opencontrol.erp.crm.domain.model.LeadStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface LeadRepository :
    JpaRepository<Lead, UUID>,
    JpaSpecificationExecutor<Lead> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<Lead>

    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<Lead>

    fun findAllByTenantIdAndStatusOrderByCreatedAtDesc(
        tenantId: UUID,
        status: LeadStatus,
    ): List<Lead>
}
