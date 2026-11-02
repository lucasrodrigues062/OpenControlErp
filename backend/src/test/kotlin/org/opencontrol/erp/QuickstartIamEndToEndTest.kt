package org.opencontrol.erp

import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.iam.api.dto.LoginRequest
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.service.AuthenticationService
import org.opencontrol.erp.iam.service.ProfileService
import org.opencontrol.erp.iam.service.UserService
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuickstartIamEndToEndTest {
    @Autowired
    private lateinit var profileService: ProfileService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var authenticationService: AuthenticationService

    @Autowired
    private lateinit var auditService: AuditService

    @Test
    fun `execute end-to-end IAM quickstart scenarios`() {
        val tenantId = UUID.randomUUID()
        val suffix = UUID.randomUUID().toString().take(8)

        SecurityContextHolder.setContext(
            UserContext(
                userId = "admin-id-$suffix",
                username = "admin.$suffix",
                roles = setOf("PROFILES_MANAGE", "USERS_WRITE", "AUDIT_READ"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "QS_TENANT", jurisdiction = "BR"),
            ),
        )

        // Scenario 1: Create Custom Profile ("Sales Operator")
        val profile =
            profileService.createProfile(
                name = "Sales Operator $suffix",
                description = "Sales team operator",
                permissions = setOf("USERS_READ"),
            )
        assertNotNull(profile.id)
        assertEquals("Sales Operator $suffix", profile.name)

        // Scenario 2: Create User with Single-Profile Association
        val username = "carlos.$suffix"
        val email = "carlos.$suffix@opencontrol.org"
        val user =
            userService.createUser(
                username = username,
                email = email,
                passwordRaw = "Password123!",
                fullName = "Carlos Silva",
                profileId = profile.id,
            )
        assertNotNull(user.id)
        assertEquals(username, user.username)
        assertEquals(profile.id, user.profile.id)
        assertEquals(UserStatus.ACTIVE, user.status)
        assertEquals("en-US", user.preferredLanguage)

        // Scenario 3: Authenticate and Generate JWT
        val loginResponse =
            authenticationService.login(
                LoginRequest(username = username, password = "Password123!"),
            )
        assertNotNull(loginResponse.accessToken)
        assertEquals("Bearer", loginResponse.tokenType)
        assertEquals(username, loginResponse.user.username)
        assertEquals("Sales Operator $suffix", loginResponse.user.profile.name)
        assertEquals("en-US", loginResponse.user.preferredLanguage)
        assertTrue(loginResponse.user.permissions?.contains("USERS_READ") == true)

        // Scenario 4: Automated Audit Trail Inspection
        val userAuditLogs =
            auditService.queryLogs(
                entityType = "User",
                entityId = user.id.toString(),
                actorId = null,
                action = "CREATE",
                fromDate = null,
                toDate = null,
                pageable = PageRequest.of(0, 10),
            )
        assertEquals(1, userAuditLogs.totalElements)
        val userAudit = userAuditLogs.content.first()
        assertEquals("CREATE", userAudit.action)
        assertEquals("admin.$suffix", userAudit.actorUsername)
        assertNotNull(userAudit.stateAfter)

        // Scenario 5: User Language Preference Update and Persistence
        val updatedUser = userService.updateUserPreferences(user.id, "pt-BR")
        assertEquals("pt-BR", updatedUser.preferredLanguage)

        val prefLoginResponse =
            authenticationService.login(
                LoginRequest(username = username, password = "Password123!"),
            )
        assertEquals("pt-BR", prefLoginResponse.user.preferredLanguage)
    }
}
