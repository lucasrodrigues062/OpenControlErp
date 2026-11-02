package org.opencontrol.erp.iam.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.domain.AuditLog
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.repository.ProfileRepository
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.PluginConflictException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserServiceTest {
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val passwordEncoder: PasswordEncoder = mockk(relaxed = true)
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var userService: UserService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository, profileRepository, passwordEncoder, auditService)
        SecurityContextHolder.setContext(
            UserContext(
                userId = "admin-id",
                username = "admin",
                roles = setOf("USERS_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `should create user with hashed password and assigned single profile`() {
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Operator")

        every { userRepository.existsByTenantIdAndUsername(tenantId, "joao") } returns false
        every { userRepository.existsByTenantIdAndEmail(tenantId, "joao@opencontrol.org") } returns false
        every { profileRepository.findById(profileId) } returns Optional.of(profile)
        every { passwordEncoder.encode("Secret123") } returns "hashed_Secret123"
        every { userRepository.save(any()) } answers { firstArg() }
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val user =
            userService.createUser(
                username = "joao",
                email = "joao@opencontrol.org",
                passwordRaw = "Secret123",
                fullName = "Joao Silva",
                profileId = profileId,
            )

        assertNotNull(user)
        assertEquals("joao", user.username)
        assertEquals("hashed_Secret123", user.passwordHash)
        assertEquals("Operator", user.profile.name)
        verify { auditService.recordAudit("User", any(), "CREATE", any(), any(), any()) }
    }

    @Test
    fun `should throw EntityNotFoundException when creating user with non-existent profile`() {
        val profileId = UUID.randomUUID()
        every { userRepository.existsByTenantIdAndUsername(tenantId, "joao") } returns false
        every { userRepository.existsByTenantIdAndEmail(tenantId, "joao@opencontrol.org") } returns false
        every { profileRepository.findById(profileId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            userService.createUser("joao", "joao@opencontrol.org", "pass", "Joao", profileId)
        }
    }

    @Test
    fun `should reject duplicate username in same tenant`() {
        every { userRepository.existsByTenantIdAndUsername(tenantId, "joao") } returns true

        assertThrows<PluginConflictException> {
            userService.createUser("joao", "joao@opencontrol.org", "pass", "Joao", UUID.randomUUID())
        }
    }

    @Test
    fun `should reject duplicate email in same tenant`() {
        every { userRepository.existsByTenantIdAndUsername(tenantId, "joao") } returns false
        every { userRepository.existsByTenantIdAndEmail(tenantId, "joao@opencontrol.org") } returns true

        assertThrows<PluginConflictException> {
            userService.createUser("joao", "joao@opencontrol.org", "pass", "Joao", UUID.randomUUID())
        }
    }

    @Test
    fun `should prevent deactivating the last active system administrator`() {
        val adminProfile =
            Profile(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                name = "Administrator",
                isSystem = true,
            )
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "admin",
                email = "admin@opencontrol.org",
                passwordHash = "hash",
                fullName = "System Admin",
                profile = adminProfile,
                status = UserStatus.ACTIVE,
            )

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { profileRepository.findById(adminProfile.id) } returns Optional.of(adminProfile)
        every { userRepository.countByProfileId(adminProfile.id) } returns 1

        assertThrows<IllegalStateException> {
            userService.updateUser(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                status = UserStatus.INACTIVE,
                profileId = adminProfile.id,
            )
        }
    }

    @Test
    fun `should update user attributes and single profile assignment`() {
        val profileA = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "ProfileA")
        val profileB = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "ProfileB")
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "maria",
                email = "maria@opencontrol.org",
                passwordHash = "hash",
                fullName = "Maria Original",
                profile = profileA,
                status = UserStatus.ACTIVE,
            )

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { profileRepository.findById(profileB.id) } returns Optional.of(profileB)
        every { userRepository.save(any()) } answers { firstArg() }

        val updated =
            userService.updateUser(
                id = user.id,
                email = "maria.new@opencontrol.org",
                fullName = "Maria Atualizada",
                status = UserStatus.ACTIVE,
                profileId = profileB.id,
            )

        assertEquals("maria.new@opencontrol.org", updated.email)
        assertEquals("Maria Atualizada", updated.fullName)
        assertEquals("ProfileB", updated.profile.name)
        verify { auditService.recordAudit("User", user.id.toString(), "UPDATE", any(), any(), any()) }
    }

    @Test
    fun `should throw EntityNotFoundException when updating non-existent user`() {
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            userService.updateUser(userId, "new@test.com", "Name", UserStatus.ACTIVE, UUID.randomUUID())
        }
    }

    @Test
    fun `should throw EntityNotFoundException when updating user with non-existent target profile`() {
        val profileId = UUID.randomUUID()
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "u",
                email = "u@test.com",
                passwordHash = "hash",
                fullName = "Name",
                profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "P"),
            )
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { profileRepository.findById(profileId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            userService.updateUser(user.id, "u@test.com", "Name", UserStatus.ACTIVE, profileId)
        }
    }

    @Test
    fun `should update user preferred language when valid`() {
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "lucas",
                email = "lucas@opencontrol.org",
                passwordHash = "hash",
                fullName = "Lucas",
                profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "P"),
                preferredLanguage = "en-US",
            )

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userRepository.save(any()) } answers { firstArg() }

        val updated = userService.updateUserPreferences(user.id, "pt-BR")

        assertEquals("pt-BR", updated.preferredLanguage)
        verify { auditService.recordAudit("User", user.id.toString(), "UPDATE_PREFERENCES", any(), any(), any()) }
    }

    @Test
    fun `should throw EntityNotFoundException when updating preferences of non-existent user`() {
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            userService.updateUserPreferences(userId, "pt-BR")
        }
    }

    @Test
    fun `should reject invalid preferred language code`() {
        val userId = UUID.randomUUID()
        assertThrows<IllegalArgumentException> {
            userService.updateUserPreferences(userId, "fr-FR")
        }
    }

    @Test
    fun `should search and get user by id`() {
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                username = "user1",
                email = "u1@opencontrol.org",
                passwordHash = "hash",
                fullName = "User One",
                profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "P"),
                status = UserStatus.ACTIVE,
            )
        val pageable = PageRequest.of(0, 10)

        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userRepository.searchUsers(tenantId, "user", UserStatus.ACTIVE, pageable) } returns PageImpl(listOf(user))

        val fetched = userService.getById(user.id)
        val searchResult = userService.searchUsers("user", UserStatus.ACTIVE, pageable)

        assertEquals(user.id, fetched.id)
        assertEquals(1, searchResult.totalElements)
    }

    @Test
    fun `should throw EntityNotFoundException when user id not found in getById`() {
        val userId = UUID.randomUUID()
        every { userRepository.findById(userId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            userService.getById(userId)
        }
    }
}
