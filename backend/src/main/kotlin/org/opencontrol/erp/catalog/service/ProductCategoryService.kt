package org.opencontrol.erp.catalog.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.api.dto.CreateProductCategoryRequest
import org.opencontrol.erp.catalog.api.dto.ProductCategoryResponse
import org.opencontrol.erp.catalog.api.dto.UpdateProductCategoryRequest
import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.opencontrol.erp.catalog.exception.DuplicateCategoryCodeException
import org.opencontrol.erp.catalog.exception.ProductCategoryNotFoundException
import org.opencontrol.erp.catalog.repository.ProductCategoryRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ProductCategoryService(
    private val repository: ProductCategoryRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listCategories(
        parentId: UUID? = null,
        activeOnly: Boolean = false,
    ): List<ProductCategoryResponse> {
        val tenantId = getCurrentTenantId()
        val list =
            when {
                parentId != null -> repository.findAllByTenantIdAndParentIdOrderByCodeAsc(tenantId, parentId)
                activeOnly -> repository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId)
                else -> repository.findAllByTenantIdOrderByCodeAsc(tenantId)
            }
        return list.map { ProductCategoryResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): ProductCategoryResponse {
        val tenantId = getCurrentTenantId()
        val category =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { ProductCategoryNotFoundException(id) }
        return ProductCategoryResponse.fromEntity(category)
    }

    fun createCategory(request: CreateProductCategoryRequest): ProductCategoryResponse {
        val tenantId = getCurrentTenantId()
        val code = request.code.trim().uppercase()

        if (repository.existsByTenantIdAndCode(tenantId, code)) {
            throw DuplicateCategoryCodeException(code)
        }

        if (request.parentId != null) {
            repository
                .findByIdAndTenantId(request.parentId, tenantId)
                .orElseThrow { ProductCategoryNotFoundException(request.parentId) }
        }

        val category =
            ProductCategory(
                tenantId = tenantId,
                parentId = request.parentId,
                code = code,
                name = request.name.trim(),
                colorHex = request.colorHex.trim(),
                isActive = true,
            )
        val saved = repository.save(category)

        auditService.recordAudit(
            entityType = "ProductCategory",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created product category ${saved.code} - ${saved.name}",
            stateAfter = saved,
        )

        return ProductCategoryResponse.fromEntity(saved)
    }

    fun updateCategory(
        id: UUID,
        request: UpdateProductCategoryRequest,
    ): ProductCategoryResponse {
        val tenantId = getCurrentTenantId()
        val category =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { ProductCategoryNotFoundException(id) }

        if (request.parentId != null) {
            if (request.parentId == id) {
                throw IllegalArgumentException("A category cannot be its own parent.")
            }
            repository
                .findByIdAndTenantId(request.parentId, tenantId)
                .orElseThrow { ProductCategoryNotFoundException(request.parentId) }
        }

        val beforeState = ProductCategoryResponse.fromEntity(category)
        category.parentId = request.parentId
        category.name = request.name.trim()
        category.colorHex = request.colorHex.trim()
        category.isActive = request.isActive
        category.updatedAt = Instant.now()

        val saved = repository.save(category)

        auditService.recordAudit(
            entityType = "ProductCategory",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated product category ${saved.code}",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        return ProductCategoryResponse.fromEntity(saved)
    }

    fun deleteCategory(id: UUID) {
        val tenantId = getCurrentTenantId()
        val category =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { ProductCategoryNotFoundException(id) }

        category.isActive = false
        category.updatedAt = Instant.now()
        repository.save(category)

        auditService.recordAudit(
            entityType = "ProductCategory",
            entityId = id.toString(),
            action = "DEACTIVATE",
            details = "Deactivated product category ${category.code}",
        )
    }
}
