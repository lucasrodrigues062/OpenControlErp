package org.opencontrol.erp.iam.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.repository.ProfileRepository
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.PluginConflictException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val passwordEncoder: PasswordEncoder,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun createUser(
        username: String,
        email: String,
        passwordRaw: String,
        fullName: String,
        profileId: UUID,
    ): User {
        val tenantId = SecurityContextHolder.getCurrentTenantId()

        if (userRepository.existsByTenantIdAndUsername(tenantId, username)) {
            throw PluginConflictException("Username '$username' is already in use in this tenant")
        }
        if (userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw PluginConflictException("Email '$email' is already registered in this tenant")
        }

        val profile =
            profileRepository.findById(profileId).orElseThrow {
                EntityNotFoundException("Profile not found with ID: $profileId")
            }

        val user =
            User(
                tenantId = tenantId,
                username = username,
                email = email,
                passwordHash = passwordEncoder.encode(passwordRaw) ?: "",
                fullName = fullName,
                status = UserStatus.ACTIVE,
                profile = profile,
            )

        val saved = userRepository.save(user)

        auditService.recordAudit(
            entityType = "User",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created user '${saved.username}' with profile '${profile.name}'",
            stateAfter =
                mapOf(
                    "id" to saved.id.toString(),
                    "username" to saved.username,
                    "email" to saved.email,
                    "fullName" to saved.fullName,
                    "status" to saved.status.name,
                    "profileId" to profile.id.toString(),
                    "profileName" to profile.name,
                ),
        )

        logger.info("Created user '{}' (id: {}) with profile '{}'", saved.username, saved.id, profile.name)
        return saved
    }

    @Transactional
    fun updateUser(
        id: UUID,
        email: String,
        fullName: String,
        status: UserStatus,
        profileId: UUID,
    ): User {
        val user =
            userRepository.findById(id).orElseThrow {
                EntityNotFoundException("User not found with ID: $id")
            }

        val tenantId = SecurityContextHolder.getCurrentTenantId()
        if (user.email != email && userRepository.existsByTenantIdAndEmail(tenantId, email)) {
            throw PluginConflictException("Email '$email' is already registered in this tenant")
        }

        val targetProfile =
            profileRepository.findById(profileId).orElseThrow {
                EntityNotFoundException("Profile not found with ID: $profileId")
            }

        // Last Admin Protection: cannot deactivate or demote last active admin
        if (user.profile.isSystem && user.profile.name.equals("Administrator", ignoreCase = true)) {
            if (status != UserStatus.ACTIVE || !targetProfile.name.equals("Administrator", ignoreCase = true)) {
                val adminCount = userRepository.countByProfileId(user.profile.id)
                if (adminCount <= 1) {
                    throw IllegalStateException("Cannot deactivate or change profile of the last remaining system administrator")
                }
            }
        }

        val stateBefore =
            mapOf(
                "email" to user.email,
                "fullName" to user.fullName,
                "status" to user.status.name,
                "profileId" to user.profile.id.toString(),
                "profileName" to user.profile.name,
            )

        user.email = email
        user.fullName = fullName
        user.status = status
        user.profile = targetProfile
        user.updatedAt = Instant.now()

        val saved = userRepository.save(user)

        auditService.recordAudit(
            entityType = "User",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated user '${saved.username}'",
            stateBefore = stateBefore,
            stateAfter =
                mapOf(
                    "email" to saved.email,
                    "fullName" to saved.fullName,
                    "status" to saved.status.name,
                    "profileId" to targetProfile.id.toString(),
                    "profileName" to targetProfile.name,
                ),
        )

        logger.info("Updated user '{}' (id: {})", saved.username, saved.id)
        return saved
    }

    @Transactional
    fun updateUserPreferences(
        id: UUID,
        preferredLanguage: String,
    ): User {
        if (preferredLanguage != "en-US" && preferredLanguage != "pt-BR") {
            throw IllegalArgumentException("Unsupported language code: $preferredLanguage. Supported codes: en-US, pt-BR")
        }

        val user =
            userRepository.findById(id).orElseThrow {
                EntityNotFoundException("User not found with ID: $id")
            }

        val stateBefore = mapOf("preferredLanguage" to user.preferredLanguage)
        user.preferredLanguage = preferredLanguage
        user.updatedAt = Instant.now()

        val saved = userRepository.save(user)

        auditService.recordAudit(
            entityType = "User",
            entityId = saved.id.toString(),
            action = "UPDATE_PREFERENCES",
            details = "Updated preferred language to '$preferredLanguage'",
            stateBefore = stateBefore,
            stateAfter = mapOf("preferredLanguage" to saved.preferredLanguage),
        )

        logger.info("Updated preferences for user '{}' (language: {})", saved.username, preferredLanguage)
        return saved
    }

    @Transactional(readOnly = true)
    fun searchUsers(
        query: String?,
        status: UserStatus?,
        pageable: Pageable,
    ): Page<User> {
        val tenantId = SecurityContextHolder.getCurrentTenantId()
        return userRepository.searchUsers(tenantId, query, status, pageable)
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): User =
        userRepository.findById(id).orElseThrow {
            EntityNotFoundException("User not found with ID: $id")
        }
}
