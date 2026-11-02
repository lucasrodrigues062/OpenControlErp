package org.opencontrol.erp.rules.repository

import org.opencontrol.erp.rules.domain.CustomRule
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CustomRuleRepository :
    JpaRepository<CustomRule, UUID>,
    JpaSpecificationExecutor<CustomRule> {
    fun findAllByTenantId(
        tenantId: UUID,
        pageable: Pageable,
    ): Page<CustomRule>

    fun findByTenantIdAndId(
        tenantId: UUID,
        id: UUID,
    ): Optional<CustomRule>

    fun findAllByTenantIdAndTriggerEventAndIsActiveOrderByPriorityAsc(
        tenantId: UUID,
        triggerEvent: String,
        isActive: Boolean = true,
    ): List<CustomRule>

    fun existsByTenantIdAndName(
        tenantId: UUID,
        name: String,
    ): Boolean

    fun existsByTenantIdAndNameAndIdNot(
        tenantId: UUID,
        name: String,
        id: UUID,
    ): Boolean
}
