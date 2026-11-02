package org.opencontrol.erp.iam.service

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JwtServiceTest {
    private val jwtService =
        JwtService(
            secret = "opencontrol-test-secure-secret-key-for-development-must-be-at-least-256-bits!",
            expirationSeconds = 3600,
        )

    @Test
    fun `should generate and validate JWT token`() {
        val userId = UUID.randomUUID()
        val tenantId = UUID.randomUUID()

        val token =
            jwtService.generateToken(
                userId = userId,
                username = "lucas.dev",
                tenantId = tenantId,
                profileName = "Developer",
                permissions = listOf("USERS_READ", "PROFILES_READ"),
            )

        assertNotNull(token)
        assertTrue(jwtService.isTokenValid(token))

        val claims = jwtService.parseAndValidateToken(token)
        assertEquals("lucas.dev", claims.subject)
        assertEquals(userId.toString(), claims["userId"])
        assertEquals(tenantId.toString(), claims["tenantId"])
        assertEquals("Developer", claims["profile"])
    }

    @Test
    fun `should return false for invalid token`() {
        assertFalse(jwtService.isTokenValid("invalid.jwt.token"))
    }
}
