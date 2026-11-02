package org.opencontrol.erp.iam.repository

import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepository : JpaRepository<User, UUID> {
    fun findByTenantIdAndUsername(
        tenantId: UUID,
        username: String,
    ): User?

    fun findByTenantIdAndEmail(
        tenantId: UUID,
        email: String,
    ): User?

    fun existsByTenantIdAndUsername(
        tenantId: UUID,
        username: String,
    ): Boolean

    fun existsByTenantIdAndEmail(
        tenantId: UUID,
        email: String,
    ): Boolean

    fun countByProfileId(profileId: UUID): Long

    @Query(
        "SELECT u FROM User u WHERE u.tenantId = :tenantId AND (:status IS NULL OR u.status = :status) AND (:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))",
    )
    fun searchUsers(
        @Param("tenantId") tenantId: UUID,
        @Param("query") query: String?,
        @Param("status") status: UserStatus?,
        pageable: Pageable,
    ): Page<User>
}
