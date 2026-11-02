package org.opencontrol.erp.companies.repository

import org.opencontrol.erp.companies.domain.UserBranchAccess
import org.opencontrol.erp.companies.domain.UserBranchId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface UserBranchAccessRepository : JpaRepository<UserBranchAccess, UserBranchId> {
    fun findAllByIdUserId(userId: UUID): List<UserBranchAccess>

    fun findByIdUserIdAndIsDefaultTrue(userId: UUID): Optional<UserBranchAccess>

    fun existsByIdUserIdAndIdBranchId(
        userId: UUID,
        branchId: UUID,
    ): Boolean

    @Query("SELECT uba.id.branchId FROM UserBranchAccess uba WHERE uba.id.userId = :userId")
    fun findBranchIdsByUserId(userId: UUID): List<UUID>

    fun deleteByIdUserId(userId: UUID)
}
