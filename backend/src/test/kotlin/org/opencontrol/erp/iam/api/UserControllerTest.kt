package org.opencontrol.erp.iam.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.iam.api.dto.CreateUserRequest
import org.opencontrol.erp.iam.api.dto.UpdateUserRequest
import org.opencontrol.erp.iam.api.dto.UserPreferencesRequest
import org.opencontrol.erp.iam.domain.Profile
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.service.UserService
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.data.domain.PageImpl
import org.springframework.http.HttpStatus
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserControllerTest {
    private val userService = mockk<UserService>(relaxed = true)
    private val controller = UserController(userService)
    private val tenantId = UUID.randomUUID()
    private val actorId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        SecurityContextHolder.setContext(
            UserContext(
                userId = actorId.toString(),
                username = "admin",
                roles = setOf("USERS_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `listUsers should return 200 with paginated items`() {
        val profile =
            Profile(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                name = "Staff",
            )
        val user =
            User(
                id = UUID.randomUUID(),
                tenantId = profile.tenantId,
                username = "alice",
                email = "alice@opencontrol.org",
                passwordHash = "hash",
                fullName = "Alice Smith",
                status = UserStatus.ACTIVE,
                profile = profile,
            )
        every { userService.searchUsers(null, null, any()) } returns PageImpl(listOf(user))

        val response = controller.listUsers(null, null, 0, 20)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.items?.size)
        assertEquals(
            "alice",
            response.body
                ?.items
                ?.first()
                ?.username,
        )
        assertEquals(
            "Staff",
            response.body
                ?.items
                ?.first()
                ?.profile
                ?.name,
        )
    }

    @Test
    fun `createUser should return 201 with created user`() {
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Staff")
        val request =
            CreateUserRequest(
                username = "bob",
                email = "bob@opencontrol.org",
                password = "Password123!",
                fullName = "Bob Jones",
                profileId = profileId,
            )
        val created =
            User(
                id = UUID.randomUUID(),
                tenantId = profile.tenantId,
                username = "bob",
                email = "bob@opencontrol.org",
                passwordHash = "hash",
                fullName = "Bob Jones",
                status = UserStatus.ACTIVE,
                profile = profile,
            )
        every { userService.createUser("bob", "bob@opencontrol.org", "Password123!", "Bob Jones", profileId) } returns created

        val response = controller.createUser(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("bob", response.body?.username)
        assertEquals("Staff", response.body?.profile?.name)
    }

    @Test
    fun `getUser should return 200 with user details`() {
        val userId = UUID.randomUUID()
        val profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "Staff")
        val user =
            User(
                id = userId,
                tenantId = profile.tenantId,
                username = "charlie",
                email = "charlie@opencontrol.org",
                passwordHash = "hash",
                fullName = "Charlie Brown",
                status = UserStatus.ACTIVE,
                profile = profile,
            )
        every { userService.getById(userId) } returns user

        val response = controller.getUser(userId)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("charlie", response.body?.username)
    }

    @Test
    fun `updateUser should return 200 with updated details`() {
        val userId = UUID.randomUUID()
        val profileId = UUID.randomUUID()
        val profile = Profile(id = profileId, tenantId = tenantId, name = "Supervisor")
        val request =
            UpdateUserRequest(
                email = "charlie.new@opencontrol.org",
                fullName = "Charlie Brown Senior",
                status = "ACTIVE",
                profileId = profileId,
            )
        val updated =
            User(
                id = userId,
                tenantId = profile.tenantId,
                username = "charlie",
                email = "charlie.new@opencontrol.org",
                passwordHash = "hash",
                fullName = "Charlie Brown Senior",
                status = UserStatus.ACTIVE,
                profile = profile,
            )
        every {
            userService.updateUser(
                userId,
                "charlie.new@opencontrol.org",
                "Charlie Brown Senior",
                UserStatus.ACTIVE,
                profileId,
            )
        } returns
            updated

        val response = controller.updateUser(userId, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("charlie.new@opencontrol.org", response.body?.email)
        assertEquals("Supervisor", response.body?.profile?.name)
    }

    @Test
    fun `updatePreferences should return 200 when authenticated`() {
        val user =
            User(
                id = actorId,
                tenantId = tenantId,
                username = "admin",
                email = "admin@opencontrol.org",
                passwordHash = "hash",
                fullName = "Admin User",
                profile = Profile(id = UUID.randomUUID(), tenantId = tenantId, name = "Admin"),
                preferredLanguage = "pt-BR",
            )
        every { userService.updateUserPreferences(actorId, "pt-BR") } returns user

        val response = controller.updatePreferences(UserPreferencesRequest("pt-BR"))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("pt-BR", response.body?.preferredLanguage)
    }

    @Test
    fun `updatePreferences should throw IllegalStateException when no context`() {
        SecurityContextHolder.clearContext()

        assertThrows<IllegalStateException> {
            controller.updatePreferences(UserPreferencesRequest("pt-BR"))
        }
    }
}
