package org.opencontrol.erp.catalog.service

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
import org.opencontrol.erp.catalog.api.dto.CreateProductCategoryRequest
import org.opencontrol.erp.catalog.api.dto.UpdateProductCategoryRequest
import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.opencontrol.erp.catalog.exception.DuplicateCategoryCodeException
import org.opencontrol.erp.catalog.exception.ProductCategoryNotFoundException
import org.opencontrol.erp.catalog.repository.ProductCategoryRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID

class ProductCategoryServiceTest {
    private val repository: ProductCategoryRepository = mockk()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: ProductCategoryService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("CATALOG_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = ProductCategoryService(repository, auditService)
    }

    @Test
    fun `should list categories successfully`() {
        val cat =
            ProductCategory(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                code = "ELEC",
                name = "Electronics",
                colorHex = "#10B981",
            )
        every { repository.findAllByTenantIdOrderByCodeAsc(tenantId) } returns listOf(cat)

        val result = service.listCategories(parentId = null, activeOnly = false)
        assertEquals(1, result.size)
        assertEquals("ELEC", result[0].code)
        assertEquals("Electronics", result[0].name)
    }

    @Test
    fun `should list child categories by parentId`() {
        val parentId = UUID.randomUUID()
        val cat =
            ProductCategory(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                parentId = parentId,
                code = "SMARTPHONES",
                name = "Smartphones",
            )
        every { repository.findAllByTenantIdAndParentIdOrderByCodeAsc(tenantId, parentId) } returns listOf(cat)

        val result = service.listCategories(parentId = parentId, activeOnly = false)
        assertEquals(1, result.size)
        assertEquals("SMARTPHONES", result[0].code)
    }

    @Test
    fun `should get category by id`() {
        val id = UUID.randomUUID()
        val cat =
            ProductCategory(
                id = id,
                tenantId = tenantId,
                code = "ELEC",
                name = "Electronics",
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(cat)

        val result = service.getById(id)
        assertEquals(id, result.id)
        assertEquals("ELEC", result.code)
    }

    @Test
    fun `should throw when category by id is not found`() {
        val id = UUID.randomUUID()
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.empty()

        assertThrows<ProductCategoryNotFoundException> {
            service.getById(id)
        }
    }

    @Test
    fun `should create category successfully`() {
        val req =
            CreateProductCategoryRequest(
                code = "COMP",
                name = "Computers",
                colorHex = "#3B82F6",
            )
        every { repository.existsByTenantIdAndCode(tenantId, "COMP") } returns false
        every { repository.save(any()) } answers { firstArg() }

        val created = service.createCategory(req)
        assertNotNull(created.id)
        assertEquals("COMP", created.code)
        assertEquals("Computers", created.name)
        verify {
            auditService.recordAudit(
                entityType = "ProductCategory",
                entityId = any(),
                action = "CREATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should throw when creating duplicate category code`() {
        val req =
            CreateProductCategoryRequest(
                code = "COMP",
                name = "Computers",
            )
        every { repository.existsByTenantIdAndCode(tenantId, "COMP") } returns true

        assertThrows<DuplicateCategoryCodeException> {
            service.createCategory(req)
        }
    }

    @Test
    fun `should update category successfully`() {
        val id = UUID.randomUUID()
        val existing =
            ProductCategory(
                id = id,
                tenantId = tenantId,
                code = "COMP",
                name = "Old Name",
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        val req =
            UpdateProductCategoryRequest(
                name = "Updated Name",
                colorHex = "#6366F1",
                isActive = true,
            )

        val updated = service.updateCategory(id, req)
        assertEquals("Updated Name", updated.name)
        assertEquals("#6366F1", updated.colorHex)
        verify {
            auditService.recordAudit(
                entityType = "ProductCategory",
                entityId = any(),
                action = "UPDATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }

    @Test
    fun `should deactivate category on delete`() {
        val id = UUID.randomUUID()
        val existing =
            ProductCategory(
                id = id,
                tenantId = tenantId,
                code = "COMP",
                name = "Computers",
                isActive = true,
            )
        every { repository.findByIdAndTenantId(id, tenantId) } returns Optional.of(existing)
        every { repository.save(any()) } answers { firstArg() }

        service.deleteCategory(id)
        assertFalse(existing.isActive)
        verify {
            auditService.recordAudit(
                entityType = "ProductCategory",
                entityId = any(),
                action = "DEACTIVATE",
                details = any(),
                stateBefore = any(),
                stateAfter = any(),
            )
        }
    }
}
