package org.opencontrol.erp.catalog.repository

import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ProductCategoryRepository : JpaRepository<ProductCategory, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<ProductCategory>

    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<ProductCategory>

    fun findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId: UUID): List<ProductCategory>

    fun findAllByTenantIdAndParentIdOrderByCodeAsc(
        tenantId: UUID,
        parentId: UUID?,
    ): List<ProductCategory>

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean
}
