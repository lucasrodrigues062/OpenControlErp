package org.opencontrol.erp.kernel.context

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SecurityContextHolderTest {
    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should return defaults when context is not set`() {
        assertNull(SecurityContextHolder.getContext())
        assertEquals("system", SecurityContextHolder.getCurrentUsername())
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), SecurityContextHolder.getCurrentTenantId())
        assertEquals("BR", SecurityContextHolder.getCurrentJurisdiction())
    }

    @Test
    fun `should return set context properties`() {
        val tenantId = UUID.randomUUID()
        val tenant = TenantContext(tenantId = tenantId, tenantCode = "EMPRESA_01", jurisdiction = "BR")
        val user = UserContext(userId = "user-123", username = "lucas", roles = setOf("ADMIN"), tenant = tenant)

        SecurityContextHolder.setContext(user)

        assertEquals("lucas", SecurityContextHolder.getCurrentUsername())
        assertEquals(tenantId, SecurityContextHolder.getCurrentTenantId())
        assertEquals("BR", SecurityContextHolder.getCurrentJurisdiction())
    }

    @Test
    fun `should clear context properly`() {
        val user = UserContext(userId = "1", username = "admin")
        SecurityContextHolder.setContext(user)
        SecurityContextHolder.clearContext()

        assertNull(SecurityContextHolder.getContext())
        assertEquals("system", SecurityContextHolder.getCurrentUsername())
    }
}
