package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.ActivityEntityType
import org.opencontrol.erp.crm.domain.model.CrmActivity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CrmActivityRepository : JpaRepository<CrmActivity, UUID> {
    fun findAllByTenantIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
        tenantId: UUID,
        entityType: ActivityEntityType,
        entityId: UUID,
    ): List<CrmActivity>
}
