package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.Customer
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CustomerRepository :
    JpaRepository<Customer, UUID>,
    JpaSpecificationExecutor<Customer> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<Customer>

    fun findByTenantIdAndDocument(
        tenantId: UUID,
        document: String,
    ): Optional<Customer>

    fun existsByTenantIdAndDocument(
        tenantId: UUID,
        document: String,
    ): Boolean

    fun countByTenantId(tenantId: UUID): Long
}
