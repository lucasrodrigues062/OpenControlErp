package org.opencontrol.erp.iam.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.iam.api.dto.LoginRequest
import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthenticationServiceTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val passwordEncoder = mockk<PasswordEncoder>(relaxed = true)
    private val jwtService = mockk<JwtService>(relaxed = true)
    private val auditService = mockk<AuditService>(relaxed = true)

    private lateinit var authenticationService: AuthenticationService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        authenticationService = AuthenticationService(userRepository, passwordEncoder, jwtService, auditService)
        SecurityContextHolder.setContext(
            UserContext(
                userId = "test",
                username = "test",
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `should login successfully with valid credentials`() {
        val profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "Manager")
        profile.setPermissions(setOf("USERS_READ"))
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "valid.user",
                email = "valid@opencontrol.org",
                passwordHash = "encoded_pass",
                fullName = "Valid User",
                status = UserStatus.ACTIVE,
                profile = profile,
            )

        every { userRepository.findByTenantIdAndUsername(tenantId, "valid.user") } returns user
        every { passwordEncoder.matches("password123", "encoded_pass") } returns true
        every { jwtService.generateToken(any(), any(), any(), any(), any()) } returns "mocked.jwt.token"

        val response = authenticationService.login(LoginRequest("valid.user", "password123"))

        assertNotNull(response)
        assertEquals("mocked.jwt.token", response.accessToken)
        assertEquals("valid.user", response.user.username)
        assertEquals("Manager", response.user.profile.name)
    }

    @Test
    fun `should reject when password does not match`() {
        val profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "Manager")
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "valid.user",
                email = "valid@opencontrol.org",
                passwordHash = "encoded_pass",
                fullName = "Valid User",
                status = UserStatus.ACTIVE,
                profile = profile,
            )

        every { userRepository.findByTenantIdAndUsername(tenantId, "valid.user") } returns user
        every { passwordEncoder.matches("wrong_pass", "encoded_pass") } returns false

        assertThrows<BadCredentialsException> {
            authenticationService.login(LoginRequest("valid.user", "wrong_pass"))
        }
    }

    @Test
    fun `should reject when user does not exist`() {
        every { userRepository.findByTenantIdAndUsername(tenantId, "ghost") } returns null
        every { userRepository.findByTenantIdAndEmail(tenantId, "ghost") } returns null

        assertThrows<BadCredentialsException> {
            authenticationService.login(LoginRequest("ghost", "pass"))
        }
    }

    @Test
    fun `should reject when user account is inactive`() {
        val profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "Manager")
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "inactive.user",
                email = "inactive@opencontrol.org",
                passwordHash = "encoded_pass",
                fullName = "Inactive User",
                status = UserStatus.INACTIVE,
                profile = profile,
            )

        every { userRepository.findByTenantIdAndUsername(tenantId, "inactive.user") } returns user
        every { passwordEncoder.matches("password123", "encoded_pass") } returns true

        val ex =
            assertThrows<IllegalStateException> {
                authenticationService.login(LoginRequest("inactive.user", "password123"))
            }
        assertEquals("User account is inactive or suspended", ex.message)
    }
}
