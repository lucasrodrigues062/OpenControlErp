package org.opencontrol.erp.crm.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateEconomicGroupRequest
import org.opencontrol.erp.crm.domain.model.EconomicGroup
import org.opencontrol.erp.crm.domain.validation.BrazilianDocumentValidator
import org.opencontrol.erp.crm.exception.DuplicateCodeException
import org.opencontrol.erp.crm.exception.EconomicGroupNotFoundException
import org.opencontrol.erp.crm.exception.InvalidDocumentException
import org.opencontrol.erp.crm.repository.EconomicGroupRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class EconomicGroupServiceTest {
    private val repository: EconomicGroupRepository = mockk()
    private val documentValidator = BrazilianDocumentValidator()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: EconomicGroupService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("CRM_ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = EconomicGroupService(repository, documentValidator, auditService)
    }

    @Test
    fun `should list economic groups`() {
        val group =
            EconomicGroup(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                code = "GRP1",
                name = "Group 1",
                consolidatedCreditLimit = BigDecimal("500000.00"),
            )
        every { repository.findAllByTenantIdOrderByCodeAsc(tenantId) } returns listOf(group)

        val result = service.listGroups(false)
        assertEquals(1, result.size)
        assertEquals("GRP1", result[0].code)
    }

    @Test
    fun `should list active economic groups`() {
        val group =
            EconomicGroup(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                code = "GRP1",
                name = "Group 1",
                isActive = true,
            )
        every { repository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId) } returns listOf(group)

        val result = service.listGroups(true)
        assertEquals(1, result.size)
    }

    @Test
    fun `should get group by id`() {
        val id = UUID.randomUUID()
        val group =
            EconomicGroup(
                id = id,
                tenantId = tenantId,
                code = "GRP1",
                name = "Group 1",
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(group)

        val result = service.getById(id)
        assertEquals("GRP1", result.code)
    }

    @Test
    fun `should throw when group not found`() {
        val id = UUID.randomUUID()
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.empty()

        assertThrows<EconomicGroupNotFoundException> {
            service.getById(id)
        }
    }

    @Test
    fun `should create group with valid holding CNPJ`() {
        val req =
            CreateEconomicGroupRequest(
                code = "grp1",
                name = "Group One Holding",
                holdingCnpj = "53066208000170",
                consolidatedCreditLimit = BigDecimal("1000000.00"),
            )
        every { repository.existsByTenantIdAndCode(tenantId, "GRP1") } returns false
        every { repository.save(any()) } answers { firstArg() }

        val created = service.createGroup(req)
        assertNotNull(created)
        assertEquals("GRP1", created.code)
        assertEquals("53066208000170", created.holdingCnpj)
    }

    @Test
    fun `should reject invalid holding CNPJ on create`() {
        val req =
            CreateEconomicGroupRequest(
                code = "GRP1",
                name = "Group Invalid",
                holdingCnpj = "11111111111111",
            )
        every { repository.existsByTenantIdAndCode(tenantId, "GRP1") } returns false

        assertThrows<InvalidDocumentException> {
            service.createGroup(req)
        }
    }

    @Test
    fun `should reject duplicate code on create`() {
        val req = CreateEconomicGroupRequest(code = "GRP1", name = "Group Dup")
        every { repository.existsByTenantIdAndCode(tenantId, "GRP1") } returns true

        assertThrows<DuplicateCodeException> {
            service.createGroup(req)
        }
    }

    @Test
    fun `should update group`() {
        val id = UUID.randomUUID()
        val existing = EconomicGroup(id = id, tenantId = tenantId, code = "GRP1", name = "Old Name")
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        val req =
            CreateEconomicGroupRequest(
                code = "GRP1",
                name = "New Name",
                holdingCnpj = null,
                consolidatedCreditLimit = BigDecimal("2000000.00"),
            )
        val updated = service.updateGroup(id, req)
        assertEquals("New Name", updated.name)
    }

    @Test
    fun `should deactivate group on delete`() {
        val id = UUID.randomUUID()
        val existing = EconomicGroup(id = id, tenantId = tenantId, code = "GRP1", name = "To Deactivate", isActive = true)
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        service.deleteGroup(id)
        assertFalse(existing.isActive)
        verify { repository.save(existing) }
    }
}
