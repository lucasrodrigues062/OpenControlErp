package org.opencontrol.erp.iam.config

import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.SystemPermission
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.repository.ProfileRepository
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class IamBootstrapRunner(
    private val profileRepository: ProfileRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val defaultTenantId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @Transactional
    override fun run(vararg args: String) {
        try {
            SecurityContextHolder.setContext(
                UserContext(
                    userId = "system",
                    username = "system",
                    roles = setOf("SYSTEM"),
                    tenant =
                        TenantContext(
                            tenantId = defaultTenantId,
                            tenantCode = "DEFAULT",
                            jurisdiction = "BR",
                        ),
                ),
            )

            // 1. Ensure System Administrator Profile exists
            var adminProfile = profileRepository.findByTenantIdAndName(defaultTenantId, "Administrator")
            if (adminProfile == null) {
                logger.info("Bootstrapping default 'Administrator' profile...")
                val profile =
                    Profile(
                        tenantId = defaultTenantId,
                        name = "Administrator",
                        description = "Default root system administrator with unrestricted privileges",
                        isSystem = true,
                    )
                profile.setPermissions(SystemPermission.entries.map { it.code }.toSet())
                adminProfile = profileRepository.save(profile)
                logger.info("Created 'Administrator' profile with ID: {}", adminProfile.id)
            }

            // 2. Ensure initial 'admin' user exists
            if (!userRepository.existsByTenantIdAndUsername(defaultTenantId, "admin")) {
                logger.info("Bootstrapping default 'admin' user account...")
                val adminUser =
                    User(
                        tenantId = defaultTenantId,
                        username = "admin",
                        email = "admin@opencontrol.org",
                        passwordHash = passwordEncoder.encode("admin123") ?: "",
                        fullName = "Root Administrator",
                        status = UserStatus.ACTIVE,
                        profile = adminProfile,
                    )
                userRepository.save(adminUser)
                logger.info("Default 'admin' user created successfully (username: admin, email: admin@opencontrol.org)")
            }
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
