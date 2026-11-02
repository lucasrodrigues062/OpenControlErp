package org.opencontrol.erp.customfields.repository

import org.opencontrol.erp.customfields.domain.EntityCustomFields
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface EntityCustomFieldsRepository : JpaRepository<EntityCustomFields, UUID> {
    fun findByTenantIdAndTargetEntityAndEntityId(
        tenantId: UUID,
        targetEntity: String,
        entityId: UUID,
    ): Optional<EntityCustomFields>

    fun existsByTenantIdAndTargetEntityAndEntityId(
        tenantId: UUID,
        targetEntity: String,
        entityId: UUID,
    ): Boolean
}
