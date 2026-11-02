package org.opencontrol.erp.companies.repository

import org.opencontrol.erp.companies.domain.CompanyBranch
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CompanyBranchRepository : JpaRepository<CompanyBranch, UUID> {
    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<CompanyBranch>

    fun findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId: UUID): List<CompanyBranch>

    fun findAllByTenantIdAndTypeOrderByCodeAsc(
        tenantId: UUID,
        type: EstablishmentType,
    ): List<CompanyBranch>

    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<CompanyBranch>

    fun findByTenantIdAndCnpj(
        tenantId: UUID,
        cnpj: String,
    ): Optional<CompanyBranch>

    fun existsByTenantIdAndCnpj(
        tenantId: UUID,
        cnpj: String,
    ): Boolean

    fun existsByTenantIdAndCnpjAndIdNot(
        tenantId: UUID,
        cnpj: String,
        id: UUID,
    ): Boolean

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean

    fun existsByTenantIdAndCodeAndIdNot(
        tenantId: UUID,
        code: String,
        id: UUID,
    ): Boolean

    fun findAllByTenantIdAndParentIdOrderByCodeAsc(
        tenantId: UUID,
        parentId: UUID,
    ): List<CompanyBranch>

    fun countByTenantIdAndParentIdAndIsActiveTrue(
        tenantId: UUID,
        parentId: UUID,
    ): Long
}
