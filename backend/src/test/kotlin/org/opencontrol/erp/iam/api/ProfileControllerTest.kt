package org.opencontrol.erp.iam.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.opencontrol.erp.iam.api.dto.CreateProfileRequest
import org.opencontrol.erp.iam.api.dto.UpdateProfileRequest
import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.SystemPermission
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.iam.service.ProfileService
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProfileControllerTest {
    private val profileService = mockk<ProfileService>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val controller = ProfileController(profileService, userRepository)

    @Test
    fun `listProfiles should return 200 with mapped DTOs and user counts`() {
        val profile =
            Profile(
                id = UUID.randomUUID(),
                tenantId = UUID.randomUUID(),
                name = "Finance",
                description = "Finance profile",
            )
        profile.setPermissions(setOf("USERS_READ"))

        every { profileService.listProfiles() } returns listOf(profile)
        every { userRepository.countByProfileId(profile.id) } returns 2L

        val response = controller.listProfiles()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
        assertEquals("Finance", response.body?.first()?.name)
        assertEquals(2L, response.body?.first()?.userCount)
    }

    @Test
    fun `createProfile should return 201 with created profile`() {
        val request =
            CreateProfileRequest(
                name = "Auditor",
                description = "Audit profile",
                permissions = listOf("AUDIT_READ"),
            )
        val created =
            Profile(
                id = UUID.randomUUID(),
                tenantId = UUID.randomUUID(),
                name = "Auditor",
                description = "Audit profile",
            )
        created.setPermissions(setOf("AUDIT_READ"))

        every { profileService.createProfile("Auditor", "Audit profile", setOf("AUDIT_READ")) } returns created

        val response = controller.createProfile(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Auditor", response.body?.name)
        assertEquals(listOf("AUDIT_READ"), response.body?.permissions)
    }

    @Test
    fun `updateProfile should return 200 with updated profile`() {
        val profileId = UUID.randomUUID()
        val request =
            UpdateProfileRequest(
                name = "Senior Auditor",
                description = "Updated description",
                permissions = listOf("AUDIT_READ", "USERS_READ"),
            )
        val updated =
            Profile(
                id = profileId,
                tenantId = UUID.randomUUID(),
                name = "Senior Auditor",
                description = "Updated description",
            )
        updated.setPermissions(setOf("AUDIT_READ", "USERS_READ"))

        every {
            profileService.updateProfile(
                profileId,
                "Senior Auditor",
                "Updated description",
                setOf("AUDIT_READ", "USERS_READ"),
            )
        } returns
            updated
        every { userRepository.countByProfileId(profileId) } returns 1L

        val response = controller.updateProfile(profileId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Senior Auditor", response.body?.name)
        assertEquals(1L, response.body?.userCount)
    }

    @Test
    fun `deleteProfile should return 204`() {
        val profileId = UUID.randomUUID()

        val response = controller.deleteProfile(profileId)

        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
    }

    @Test
    fun `getPermissionCatalog should return 200 with catalog items`() {
        every { profileService.getPermissionCatalog() } returns SystemPermission.entries

        val response = controller.getPermissionCatalog()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(SystemPermission.entries.size, response.body?.size)
    }
}
