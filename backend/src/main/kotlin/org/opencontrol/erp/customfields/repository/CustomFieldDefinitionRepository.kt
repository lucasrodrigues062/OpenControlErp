package org.opencontrol.erp.customfields.repository

import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CustomFieldDefinitionRepository : JpaRepository<CustomFieldDefinition, UUID> {
    fun findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(
        tenantId: UUID,
        targetEntity: String,
    ): List<CustomFieldDefinition>

    fun findByTenantIdAndTargetEntityAndIsActiveTrueOrderByDisplayOrderAsc(
        tenantId: UUID,
        targetEntity: String,
    ): List<CustomFieldDefinition>

    fun findByTenantIdAndTargetEntityAndFieldKey(
        tenantId: UUID,
        targetEntity: String,
        fieldKey: String,
    ): Optional<CustomFieldDefinition>

    fun existsByTenantIdAndTargetEntityAndFieldKey(
        tenantId: UUID,
        targetEntity: String,
        fieldKey: String,
    ): Boolean

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<CustomFieldDefinition>
}
