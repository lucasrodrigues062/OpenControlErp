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
import org.opencontrol.erp.iam.domain.SystemPermission
import org.opencontrol.erp.iam.repository.ProfileRepository
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.opencontrol.erp.kernel.exception.PluginConflictException
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProfileServiceTest {
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var profileService: ProfileService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        profileService = ProfileService(profileRepository, userRepository, auditService)
        SecurityContextHolder.setContext(
            UserContext(
                userId = "test-admin",
                username = "admin",
                roles = setOf("PROFILES_MANAGE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `should create profile with permissions and record audit`() {
        every { profileRepository.existsByTenantIdAndName(tenantId, "Finance") } returns false
        every { profileRepository.save(any()) } answers { firstArg() }
        every { auditService.recordAudit(any(), any(), any(), any(), any(), any()) } returns mockk<AuditLog>()

        val profile =
            profileService.createProfile(
                name = "Finance",
                description = "Finance team",
                permissions = setOf("USERS_READ"),
            )

        assertNotNull(profile)
        assertEquals("Finance", profile.name)
        verify { profileRepository.save(any()) }
        verify { auditService.recordAudit("Profile", any(), "CREATE", any(), any(), any()) }
    }

    @Test
    fun `should throw exception when profile name already exists`() {
        every { profileRepository.existsByTenantIdAndName(tenantId, "Finance") } returns true

        assertThrows<PluginConflictException> {
            profileService.createProfile(
                name = "Finance",
                description = "Finance team",
                permissions = setOf("USERS_READ"),
            )
        }
    }

    @Test
    fun `should throw exception when deleting profile with assigned users`() {
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Finance", isSystem = false)

        every { profileRepository.findById(profileId) } returns Optional.of(profile)
        every { userRepository.countByProfileId(profileId) } returns 3L

        val exception =
            assertThrows<IllegalStateException> {
                profileService.deleteProfile(profileId)
            }
        assertEquals("Cannot delete profile 'Finance' because it has 3 assigned user(s)", exception.message)
    }

    @Test
    fun `should throw exception when deleting system profile`() {
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Administrator", isSystem = true)

        every { profileRepository.findById(profileId) } returns Optional.of(profile)

        val exception =
            assertThrows<IllegalStateException> {
                profileService.deleteProfile(profileId)
            }
        assertEquals("System profiles cannot be deleted", exception.message)
    }

    @Test
    fun `should delete profile successfully when no users assigned`() {
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Temp", isSystem = false)

        every { profileRepository.findById(profileId) } returns Optional.of(profile)
        every { userRepository.countByProfileId(profileId) } returns 0L

        profileService.deleteProfile(profileId)

        verify { profileRepository.delete(profile) }
        verify { auditService.recordAudit("Profile", profileId.toString(), "DELETE", any(), any(), any()) }
    }

    @Test
    fun `should update profile successfully`() {
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Sales", isSystem = false)

        every { profileRepository.findById(profileId) } returns Optional.of(profile)
        every { profileRepository.save(any()) } answers { firstArg() }

        val updated = profileService.updateProfile(profileId, "Sales", "New Desc", setOf("USERS_READ"))

        assertEquals("Sales", updated.name)
        assertEquals("New Desc", updated.description)
        verify { profileRepository.save(any()) }
    }

    @Test
    fun `should list profiles and return catalog`() {
        val profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "Sales", isSystem = false)
        every { profileRepository.findAllByTenantId(tenantId) } returns listOf(profile)
        every { profileRepository.findById(profile.id) } returns Optional.of(profile)

        val list = profileService.listProfiles()
        val fetched = profileService.getById(profile.id)
        val catalog = profileService.getPermissionCatalog()

        assertEquals(1, list.size)
        assertEquals(profile.id, fetched.id)
        assertEquals(SystemPermission.entries.size, catalog.size)
    }
}
