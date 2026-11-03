package org.opencontrol.erp.crm.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateCustomerCategoryRequest
import org.opencontrol.erp.crm.api.dto.CustomerCategoryResponse
import org.opencontrol.erp.crm.domain.model.CustomerCategory
import org.opencontrol.erp.crm.exception.CustomerCategoryNotFoundException
import org.opencontrol.erp.crm.exception.DuplicateCodeException
import org.opencontrol.erp.crm.repository.CustomerCategoryRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CustomerCategoryService(
    private val repository: CustomerCategoryRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listCategories(activeOnly: Boolean = false): List<CustomerCategoryResponse> {
        val tenantId = getCurrentTenantId()
        val list =
            if (activeOnly) {
                repository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId)
            } else {
                repository.findAllByTenantIdOrderByCodeAsc(tenantId)
            }
        return list.map { CustomerCategoryResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): CustomerCategoryResponse {
        val tenantId = getCurrentTenantId()
        val category =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomerCategoryNotFoundException(id) }
        return CustomerCategoryResponse.fromEntity(category)
    }

    fun createCategory(request: CreateCustomerCategoryRequest): CustomerCategoryResponse {
        val tenantId = getCurrentTenantId()
        val code = request.code.trim().uppercase()
        if (repository.existsByTenantIdAndCode(tenantId, code)) {
            throw DuplicateCodeException(code)
        }

        val category =
            CustomerCategory(
                tenantId = tenantId,
                code = code,
                name = request.name.trim(),
                description = request.description?.trim(),
                colorHex = request.colorHex.trim(),
                defaultDiscountPct = request.defaultDiscountPct,
                isActive = true,
            )
        val saved = repository.save(category)

        auditService.recordAudit(
            entityType = "CustomerCategory",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created customer category ${saved.code} - ${saved.name}",
            stateAfter = saved,
        )

        return CustomerCategoryResponse.fromEntity(saved)
    }

    fun updateCategory(
        id: UUID,
        request: CreateCustomerCategoryRequest,
    ): CustomerCategoryResponse {
        val tenantId = getCurrentTenantId()
        val category =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomerCategoryNotFoundException(id) }

        val beforeState = CustomerCategoryResponse.fromEntity(category)
        category.name = request.name.trim()
        category.description = request.description?.trim()
        category.colorHex = request.colorHex.trim()
        category.defaultDiscountPct = request.defaultDiscountPct
        category.updatedAt = Instant.now()

        val saved = repository.save(category)

        auditService.recordAudit(
            entityType = "CustomerCategory",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated customer category ${saved.code}",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        return CustomerCategoryResponse.fromEntity(saved)
    }

    fun deleteCategory(id: UUID) {
        val tenantId = getCurrentTenantId()
        val category =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomerCategoryNotFoundException(id) }

        category.isActive = false
        category.updatedAt = Instant.now()
        repository.save(category)

        auditService.recordAudit(
            entityType = "CustomerCategory",
            entityId = id.toString(),
            action = "DEACTIVATE",
            details = "Deactivated customer category ${category.code}",
        )
    }
}
