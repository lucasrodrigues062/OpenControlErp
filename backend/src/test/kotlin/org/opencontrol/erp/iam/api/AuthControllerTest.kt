package org.opencontrol.erp.iam.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.opencontrol.erp.iam.api.dto.LoginRequest
import org.opencontrol.erp.iam.api.dto.LoginResponse
import org.opencontrol.erp.iam.api.dto.ProfileSummaryDto
import org.opencontrol.erp.iam.api.dto.UserResponse
import org.opencontrol.erp.iam.service.AuthenticationService
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AuthControllerTest {
    private val authService = mockk<AuthenticationService>(relaxed = true)
    private val controller = AuthController(authService)

    @Test
    fun `login should return 200 with jwt and user details`() {
        val request = LoginRequest("admin", "admin123")
        val userResponse =
            UserResponse(
                id = UUID.randomUUID(),
                username = "admin",
                email = "admin@opencontrol.org",
                fullName = "Admin",
                status = "ACTIVE",
                profile = ProfileSummaryDto(UUID.randomUUID(), "Administrator"),
                createdAt = Instant.now(),
            )
        val loginResponse =
            LoginResponse(
                accessToken = "token-123",
                user = userResponse,
            )
        every { authService.login(request) } returns loginResponse

        val response = controller.login(request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("token-123", response.body?.accessToken)
        assertEquals("admin", response.body?.user?.username)
    }
}
