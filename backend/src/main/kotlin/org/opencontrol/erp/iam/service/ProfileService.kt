package org.opencontrol.erp.iam.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.SystemPermission
import org.opencontrol.erp.iam.repository.ProfileRepository
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.PluginConflictException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ProfileService(
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createProfile(
        name: String,
        description: String?,
        permissions: Set<String>,
    ): Profile {
        val tenantId = SecurityContextHolder.getCurrentTenantId()

        if (profileRepository.existsByTenantIdAndName(tenantId, name)) {
            throw PluginConflictException("A profile with name '$name' already exists in this tenant")
        }

        val profile =
            Profile(
                tenantId = tenantId,
                name = name,
                description = description,
                isSystem = false,
            )
        profile.setPermissions(permissions)

        val saved = profileRepository.save(profile)

        auditService.recordAudit(
            entityType = "Profile",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created profile '${saved.name}' with ${permissions.size} permissions",
            stateAfter =
                mapOf(
                    "id" to saved.id.toString(),
                    "name" to saved.name,
                    "description" to saved.description,
                    "permissions" to permissions.toList(),
                ),
        )

        logger.info("Created profile '{}' (id: {})", saved.name, saved.id)
        return saved
    }

    @Transactional
    fun updateProfile(
        id: UUID,
        name: String,
        description: String?,
        permissions: Set<String>,
    ): Profile {
        val profile =
            profileRepository.findById(id).orElseThrow {
                EntityNotFoundException("Profile not found with ID: $id")
            }

        val tenantId = SecurityContextHolder.getCurrentTenantId()
        if (profile.name != name && profileRepository.existsByTenantIdAndName(tenantId, name)) {
            throw PluginConflictException("A profile with name '$name' already exists in this tenant")
        }

        val stateBefore =
            mapOf(
                "name" to profile.name,
                "description" to profile.description,
                "permissions" to profile.permissions.map { it.id.permission },
            )

        profile.name = name
        profile.description = description
        profile.setPermissions(permissions)
        profile.updatedAt = Instant.now()

        val saved = profileRepository.save(profile)

        auditService.recordAudit(
            entityType = "Profile",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated profile '${saved.name}'",
            stateBefore = stateBefore,
            stateAfter =
                mapOf(
                    "name" to saved.name,
                    "description" to saved.description,
                    "permissions" to permissions.toList(),
                ),
        )

        logger.info("Updated profile '{}' (id: {})", saved.name, saved.id)
        return saved
    }

    @Transactional
    fun deleteProfile(id: UUID) {
        val profile =
            profileRepository.findById(id).orElseThrow {
                EntityNotFoundException("Profile not found with ID: $id")
            }

        if (profile.isSystem) {
            throw IllegalStateException("System profiles cannot be deleted")
        }

        val assignedUsersCount = userRepository.countByProfileId(id)
        if (assignedUsersCount > 0) {
            throw IllegalStateException("Cannot delete profile '${profile.name}' because it has $assignedUsersCount assigned user(s)")
        }

        auditService.recordAudit(
            entityType = "Profile",
            entityId = profile.id.toString(),
            action = "DELETE",
            details = "Deleted profile '${profile.name}'",
            stateBefore =
                mapOf(
                    "name" to profile.name,
                    "permissions" to profile.permissions.map { it.id.permission },
                ),
        )

        profileRepository.delete(profile)
        logger.info("Deleted profile '{}' (id: {})", profile.name, profile.id)
    }

    @Transactional(readOnly = true)
    fun listProfiles(): List<Profile> {
        val tenantId = SecurityContextHolder.getCurrentTenantId()
        return profileRepository.findAllByTenantId(tenantId)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): Profile =
        profileRepository.findById(id).orElseThrow {
            EntityNotFoundException("Profile not found with ID: $id")
        }

    fun getPermissionCatalog(): List<SystemPermission> = SystemPermission.entries
}
