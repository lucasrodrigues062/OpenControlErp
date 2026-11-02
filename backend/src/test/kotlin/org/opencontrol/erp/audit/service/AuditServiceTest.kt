package org.opencontrol.erp.audit.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Expression
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.domain.AuditLog
import org.opencontrol.erp.audit.repository.AuditLogRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuditServiceTest {
    private val auditLogRepository: AuditLogRepository = mockk(relaxed = true)
    private val objectMapper = ObjectMapper()

    private lateinit var auditService: AuditService
    private val tenantId = UUID.randomUUID()
    private val actorId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
        auditService = AuditService(auditLogRepository, objectMapper)
        SecurityContextHolder.setContext(
            UserContext(
                userId = actorId.toString(),
                username = "auditor",
                roles = setOf("AUDIT_READ"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )
    }

    @Test
    fun `should capture actor context and serialize before and after state diff`() {
        val before = mapOf("status" to "INACTIVE")
        val after = mapOf("status" to "ACTIVE")

        every { auditLogRepository.save(any()) } answers { firstArg() }

        val log =
            auditService.recordAudit(
                entityType = "User",
                entityId = "user-123",
                action = "UPDATE",
                details = "Activated user",
                stateBefore = before,
                stateAfter = after,
            )

        assertNotNull(log)
        assertEquals("auditor", log.actorUsername)
        assertEquals(actorId, log.actorId)
        assertEquals("UPDATE", log.action)
        assertEquals("User", log.entityType)
        assertEquals("{\"status\":\"INACTIVE\"}", log.stateBefore)
        assertEquals("{\"status\":\"ACTIVE\"}", log.stateAfter)

        verify { auditLogRepository.save(any()) }
    }

    @Test
    fun `should handle invalid UUID actorId gracefully`() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "non-uuid-user",
                username = "system_user",
                roles = emptySet(),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TEST", jurisdiction = "BR"),
            ),
        )

        every { auditLogRepository.save(any()) } answers { firstArg() }

        val log =
            auditService.recordAudit(
                entityType = "Profile",
                entityId = "prof-1",
                action = "CREATE",
            )

        assertNotNull(log)
        assertEquals("system_user", log.actorUsername)
        assertNull(log.actorId)
        assertNull(log.stateBefore)
        assertNull(log.stateAfter)
    }

    @Test
    fun `should handle missing SecurityContext with default fallbacks`() {
        SecurityContextHolder.clearContext()
        every { auditLogRepository.save(any()) } answers { firstArg() }

        val log =
            auditService.recordAudit(
                entityType = "System",
                entityId = "init",
                action = "BOOTSTRAP",
            )

        assertNotNull(log)
        assertEquals("system", log.actorUsername)
        assertNull(log.actorId)
    }

    @Test
    fun `should query logs with all specification filters applied`() {
        val pageable = PageRequest.of(0, 10)
        val expectedPage = PageImpl(emptyList<AuditLog>(), pageable, 0)
        val specSlot = slot<Specification<AuditLog>>()

        every { auditLogRepository.findAll(capture(specSlot), pageable) } returns expectedPage

        val result =
            auditService.queryLogs(
                entityType = "User",
                entityId = "u-1",
                actorId = actorId,
                action = "LOGIN",
                fromDate = Instant.now().minusSeconds(3600),
                toDate = Instant.now(),
                pageable = pageable,
            )

        assertNotNull(result)
        verify { auditLogRepository.findAll(any<Specification<AuditLog>>(), pageable) }

        // Evaluate captured specification to test CriteriaBuilder predicate building
        val root = mockk<Root<AuditLog>>(relaxed = true)
        val query = mockk<CriteriaQuery<*>>(relaxed = true)
        val cb = mockk<CriteriaBuilder>(relaxed = true)
        val dummyPredicate = mockk<Predicate>(relaxed = true)

        every { cb.equal(any<Expression<*>>(), any()) } returns dummyPredicate
        every { cb.greaterThanOrEqualTo(any<Expression<Instant>>(), any<Instant>()) } returns dummyPredicate
        every { cb.lessThanOrEqualTo(any<Expression<Instant>>(), any<Instant>()) } returns dummyPredicate
        every { cb.and(*anyVararg()) } returns dummyPredicate

        val predicate = specSlot.captured.toPredicate(root, query, cb)
        assertNotNull(predicate)
    }

    @Test
    fun `should query logs with null filters`() {
        val pageable = PageRequest.of(0, 10)
        val expectedPage = PageImpl(emptyList<AuditLog>(), pageable, 0)
        val specSlot = slot<Specification<AuditLog>>()

        every { auditLogRepository.findAll(capture(specSlot), pageable) } returns expectedPage

        val result =
            auditService.queryLogs(
                entityType = null,
                entityId = null,
                actorId = null,
                action = null,
                fromDate = null,
                toDate = null,
                pageable = pageable,
            )

        assertNotNull(result)
        verify { auditLogRepository.findAll(any<Specification<AuditLog>>(), pageable) }

        // Evaluate captured specification for null/empty paths
        val root = mockk<Root<AuditLog>>(relaxed = true)
        val query = mockk<CriteriaQuery<*>>(relaxed = true)
        val cb = mockk<CriteriaBuilder>(relaxed = true)
        val dummyPredicate = mockk<Predicate>(relaxed = true)

        every { cb.equal(any<Expression<*>>(), any()) } returns dummyPredicate
        every { cb.and(*anyVararg()) } returns dummyPredicate

        val predicate = specSlot.captured.toPredicate(root, query, cb)
        assertNotNull(predicate)
    }
}
