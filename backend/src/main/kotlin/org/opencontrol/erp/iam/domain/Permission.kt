package org.opencontrol.erp.iam.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable
import java.util.UUID

enum class SystemPermission(
    val code: String,
    val category: String,
    val label: String,
    val description: String,
) {
    USERS_READ(
        "USERS_READ",
        "User Management",
        "View Users",
        "Allows listing, searching, and viewing user account details",
    ),
    USERS_WRITE(
        "USERS_WRITE",
        "User Management",
        "Manage Users",
        "Allows creating, updating, and toggling user accounts",
    ),
    PROFILES_READ(
        "PROFILES_READ",
        "Security",
        "View Profiles",
        "Allows viewing access profiles and the permission catalog",
    ),
    PROFILES_MANAGE(
        "PROFILES_MANAGE",
        "Security",
        "Manage Profiles",
        "Allows creating, editing, and deleting access profiles",
    ),
    AUDIT_READ(
        "AUDIT_READ",
        "Compliance",
        "View Audit Trail",
        "Allows searching and inspecting chronological audit log entries",
    ),
    RULES_READ(
        "RULES_READ",
        "Business Rules",
        "View Rules",
        "Allows viewing and testing custom business rules and execution traces",
    ),
    RULES_MANAGE(
        "RULES_MANAGE",
        "Business Rules",
        "Manage Rules",
        "Allows creating, editing, and deleting custom business rules",
    ),
}

@Embeddable
data class ProfilePermissionId(
    @Column(name = "profile_id")
    val profileId: UUID = UUID.randomUUID(),
    @Column(name = "permission", length = 100)
    val permission: String = "",
) : Serializable

@Entity
@Table(name = "iam_profile_permission")
class ProfilePermission(
    @EmbeddedId
    val id: ProfilePermissionId = ProfilePermissionId(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", insertable = false, updatable = false)
    val profile: Profile? = null,
)
