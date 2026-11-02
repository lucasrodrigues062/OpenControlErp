package org.opencontrol.erp.iam.repository

import org.opencontrol.erp.iam.domain.Profile
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ProfileRepository : JpaRepository<Profile, UUID> {
    fun findByTenantIdAndName(
        tenantId: UUID,
        name: String,
    ): Profile?

    fun existsByTenantIdAndName(
        tenantId: UUID,
        name: String,
    ): Boolean

    fun findAllByTenantId(tenantId: UUID): List<Profile>
}
