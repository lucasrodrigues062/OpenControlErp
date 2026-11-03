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
import org.opencontrol.erp.crm.api.dto.CreateCustomerCategoryRequest
import org.opencontrol.erp.crm.domain.model.CustomerCategory
import org.opencontrol.erp.crm.exception.CustomerCategoryNotFoundException
import org.opencontrol.erp.crm.exception.DuplicateCodeException
import org.opencontrol.erp.crm.repository.CustomerCategoryRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class CustomerCategoryServiceTest {
    private val repository: CustomerCategoryRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: CustomerCategoryService
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
        service = CustomerCategoryService(repository, auditService)
    }

    @Test
    fun `should list categories successfully`() {
        val cat =
            CustomerCategory(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                code = "VIP",
                name = "VIP Clients",
                colorHex = "#FF0000",
                defaultDiscountPct = BigDecimal("10.00"),
            )
        every { repository.findAllByTenantIdOrderByCodeAsc(tenantId) } returns listOf(cat)

        val result = service.listCategories(false)
        assertEquals(1, result.size)
        assertEquals("VIP", result[0].code)
    }

    @Test
    fun `should list only active categories when activeOnly is true`() {
        val cat =
            CustomerCategory(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                code = "VIP",
                name = "VIP Clients",
                isActive = true,
            )
        every { repository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId) } returns listOf(cat)

        val result = service.listCategories(true)
        assertEquals(1, result.size)
        assertEquals("VIP", result[0].code)
    }

    @Test
    fun `should get category by id`() {
        val id = UUID.randomUUID()
        val cat =
            CustomerCategory(
                id = id,
                tenantId = tenantId,
                code = "GOLD",
                name = "Gold Tier",
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(cat)

        val result = service.getById(id)
        assertEquals("GOLD", result.code)
    }

    @Test
    fun `should throw when category not found by id`() {
        val id = UUID.randomUUID()
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.empty()

        assertThrows<CustomerCategoryNotFoundException> {
            service.getById(id)
        }
    }

    @Test
    fun `should create category successfully`() {
        val req =
            CreateCustomerCategoryRequest(
                code = "vip",
                name = "VIP Tier",
                description = "High value clients",
                colorHex = "#10B981",
                defaultDiscountPct = BigDecimal("15.00"),
            )
        every { repository.existsByTenantIdAndCode(tenantId, "VIP") } returns false
        every { repository.save(any()) } answers { firstArg() }

        val created = service.createCategory(req)
        assertNotNull(created)
        assertEquals("VIP", created.code)
        assertEquals("VIP Tier", created.name)
        verify { repository.save(any()) }
    }

    @Test
    fun `should reject duplicate code on create`() {
        val req =
            CreateCustomerCategoryRequest(
                code = "VIP",
                name = "VIP",
            )
        every { repository.existsByTenantIdAndCode(tenantId, "VIP") } returns true

        assertThrows<DuplicateCodeException> {
            service.createCategory(req)
        }
    }

    @Test
    fun `should update category`() {
        val id = UUID.randomUUID()
        val existing =
            CustomerCategory(
                id = id,
                tenantId = tenantId,
                code = "OLD",
                name = "Old Name",
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        val req =
            CreateCustomerCategoryRequest(
                code = "OLD",
                name = "New Name",
                description = "Updated desc",
                colorHex = "#0000FF",
                defaultDiscountPct = BigDecimal("5.00"),
            )
        val updated = service.updateCategory(id, req)
        assertEquals("New Name", updated.name)
    }

    @Test
    fun `should deactivate category on delete`() {
        val id = UUID.randomUUID()
        val existing =
            CustomerCategory(
                id = id,
                tenantId = tenantId,
                code = "OLD",
                name = "To Deactivate",
                isActive = true,
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        service.deleteCategory(id)
        assertFalse(existing.isActive)
        verify { repository.save(existing) }
    }
}
