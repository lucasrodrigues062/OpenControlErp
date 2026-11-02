package org.opencontrol.erp.iam.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "iam_profile")
class Profile(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(length = 255)
    var description: String? = null,
    @Column(name = "is_system", nullable = false)
    val isSystem: Boolean = false,
    @OneToMany(mappedBy = "profile", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val permissions: MutableSet<ProfilePermission> = mutableSetOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun setPermissions(newPermissions: Set<String>) {
        permissions.clear()
        newPermissions.forEach { perm ->
            permissions.add(
                ProfilePermission(
                    id = ProfilePermissionId(profileId = id, permission = perm),
                    profile = this,
                ),
            )
        }
        updatedAt = Instant.now()
    }

    fun hasPermission(permission: String): Boolean = isSystem || permissions.any { it.id.permission == permission }
}
