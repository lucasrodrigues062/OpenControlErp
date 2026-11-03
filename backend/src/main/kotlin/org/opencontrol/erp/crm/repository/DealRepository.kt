package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.Deal
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface DealRepository :
    JpaRepository<Deal, UUID>,
    JpaSpecificationExecutor<Deal> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<Deal>

    fun findAllByTenantIdAndPipelineId(
        tenantId: UUID,
        pipelineId: UUID,
    ): List<Deal>

    fun findAllByTenantIdAndStatus(
        tenantId: UUID,
        status: DealStatus,
    ): List<Deal>

    fun countByTenantIdAndStatus(
        tenantId: UUID,
        status: DealStatus,
    ): Long

    fun existsByStageId(stageId: UUID): Boolean
}
