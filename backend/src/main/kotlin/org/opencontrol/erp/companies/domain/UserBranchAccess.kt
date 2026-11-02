package org.opencontrol.erp.companies.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Embeddable
data class UserBranchId(
    @Column(name = "user_id")
    val userId: UUID = UUID.randomUUID(),
    @Column(name = "branch_id")
    val branchId: UUID = UUID.randomUUID(),
) : Serializable

@Entity
@Table(name = "iam_user_branch")
class UserBranchAccess(
    @EmbeddedId
    val id: UserBranchId = UserBranchId(),
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "assigned_at", nullable = false)
    val assignedAt: Instant = Instant.now(),
)
