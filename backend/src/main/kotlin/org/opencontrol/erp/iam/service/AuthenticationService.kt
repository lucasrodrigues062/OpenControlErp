package org.opencontrol.erp.iam.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.iam.api.dto.LoginRequest
import org.opencontrol.erp.iam.api.dto.LoginResponse
import org.opencontrol.erp.iam.api.dto.ProfileSummaryDto
import org.opencontrol.erp.iam.api.dto.UserResponse
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthenticationService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val auditService: AuditService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val tenantId = SecurityContextHolder.getCurrentTenantId()

        val user =
            userRepository.findByTenantIdAndUsername(tenantId, request.username)
                ?: userRepository.findByTenantIdAndEmail(tenantId, request.username)
                ?: throw BadCredentialsException("Invalid username or password")

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            logger.warn("Authentication failed for user '{}': password mismatch", request.username)
            throw BadCredentialsException("Invalid username or password")
        }

        if (user.status != UserStatus.ACTIVE) {
            logger.warn("Authentication blocked for user '{}': status is {}", request.username, user.status)
            throw IllegalStateException("User account is inactive or suspended")
        }

        val permissions = user.profile.permissions.map { it.id.permission }

        val token =
            jwtService.generateToken(
                userId = user.id,
                username = user.username,
                tenantId = user.tenantId,
                profileName = user.profile.name,
                permissions = permissions,
            )

        auditService.recordAudit(
            entityType = "User",
            entityId = user.id.toString(),
            action = "LOGIN",
            details = "User '${user.username}' successfully authenticated",
        )

        val userResponse =
            UserResponse(
                id = user.id,
                username = user.username,
                email = user.email,
                fullName = user.fullName,
                status = user.status.name,
                profile =
                    ProfileSummaryDto(
                        id = user.profile.id,
                        name = user.profile.name,
                    ),
                permissions = permissions,
                createdAt = user.createdAt,
            )

        return LoginResponse(
            accessToken = token,
            tokenType = "Bearer",
            expiresIn = 86400,
            user = userResponse,
        )
    }
}
