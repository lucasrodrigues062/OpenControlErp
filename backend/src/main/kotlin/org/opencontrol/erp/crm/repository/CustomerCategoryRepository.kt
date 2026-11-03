package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.CustomerCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CustomerCategoryRepository : JpaRepository<CustomerCategory, UUID> {
    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<CustomerCategory>

    fun findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId: UUID): List<CustomerCategory>

    fun findByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Optional<CustomerCategory>

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<CustomerCategory>

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean
}
