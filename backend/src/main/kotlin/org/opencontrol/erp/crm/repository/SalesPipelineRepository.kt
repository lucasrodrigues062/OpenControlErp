package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.SalesPipeline
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface SalesPipelineRepository : JpaRepository<SalesPipeline, UUID> {
    fun findAllByTenantIdOrderByCreatedAtAsc(tenantId: UUID): List<SalesPipeline>

    fun findByTenantIdAndIsDefaultTrue(tenantId: UUID): Optional<SalesPipeline>

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<SalesPipeline>

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean
}
