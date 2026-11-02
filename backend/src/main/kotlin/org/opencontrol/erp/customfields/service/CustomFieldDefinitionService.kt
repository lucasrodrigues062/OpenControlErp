package org.opencontrol.erp.customfields.service

import org.opencontrol.erp.customfields.api.dto.CreateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.api.dto.CustomFieldDefinitionResponse
import org.opencontrol.erp.customfields.api.dto.FieldOrderDto
import org.opencontrol.erp.customfields.api.dto.UpdateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.exception.CustomFieldNotFoundException
import org.opencontrol.erp.customfields.exception.DuplicateFieldKeyException
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import org.opencontrol.erp.customfields.repository.CustomFieldDefinitionRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CustomFieldDefinitionService(
    private val definitionRepository: CustomFieldDefinitionRepository,
    private val entityRegistry: CustomFieldEntityRegistry,
) {
    private fun currentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    fun createDefinition(request: CreateCustomFieldDefinitionRequest): CustomFieldDefinitionResponse {
        val tenantId = currentTenantId()
        val normalizedEntity = request.targetEntity.trim().uppercase()
        val normalizedKey = request.fieldKey.trim().lowercase()

        // 1. Validate entity is registered in SPI
        entityRegistry.validateRegistered(normalizedEntity)

        // 2. Validate SELECT options
        if (request.fieldType == FieldType.SELECT) {
            if (request.options.isNullOrEmpty() || request.options.all { it.isBlank() }) {
                throw InvalidFieldValueException("Options list cannot be empty for SELECT field type")
            }
        }

        // 3. Prevent duplicate keys for the same entity in tenant
        if (definitionRepository.existsByTenantIdAndTargetEntityAndFieldKey(tenantId, normalizedEntity, normalizedKey)) {
            throw DuplicateFieldKeyException(normalizedEntity, normalizedKey)
        }

        val entity =
            CustomFieldDefinition(
                tenantId = tenantId,
                targetEntity = normalizedEntity,
                fieldKey = normalizedKey,
                label = request.label.trim(),
                description = request.description?.trim(),
                fieldType = request.fieldType,
                required = request.required,
                adminOnly = request.adminOnly,
                readOnly = request.readOnly,
                defaultValue = request.defaultValue?.trim(),
                options = request.options?.map { it.trim() }?.filter { it.isNotBlank() },
                validationRules = request.validationRules,
                displayOrder = request.displayOrder,
                isActive = true,
            )

        val saved = definitionRepository.save(entity)
        return CustomFieldDefinitionResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun listDefinitions(
        targetEntity: String,
        activeOnly: Boolean = true,
    ): List<CustomFieldDefinitionResponse> {
        val tenantId = currentTenantId()
        val normalizedEntity = targetEntity.trim().uppercase()
        entityRegistry.validateRegistered(normalizedEntity)

        val list =
            if (activeOnly) {
                definitionRepository.findByTenantIdAndTargetEntityAndIsActiveTrueOrderByDisplayOrderAsc(tenantId, normalizedEntity)
            } else {
                definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, normalizedEntity)
            }

        return list.map { CustomFieldDefinitionResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getDefinitionById(id: UUID): CustomFieldDefinitionResponse {
        val tenantId = currentTenantId()
        val entity =
            definitionRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomFieldNotFoundException(id) }
        return CustomFieldDefinitionResponse.fromEntity(entity)
    }

    fun updateDefinition(
        id: UUID,
        request: UpdateCustomFieldDefinitionRequest,
    ): CustomFieldDefinitionResponse {
        val tenantId = currentTenantId()
        val entity =
            definitionRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomFieldNotFoundException(id) }

        entity.label = request.label.trim()
        request.description?.let { entity.description = it.trim() }
        request.required?.let { entity.required = it }
        request.adminOnly?.let { entity.adminOnly = it }
        request.readOnly?.let { entity.readOnly = it }
        request.defaultValue?.let { entity.defaultValue = it.trim() }
        request.options?.let {
            if (entity.fieldType == FieldType.SELECT && (it.isEmpty() || it.all { opt -> opt.isBlank() })) {
                throw InvalidFieldValueException("Options list cannot be empty for SELECT field type")
            }
            entity.options = it.map { opt -> opt.trim() }.filter { opt -> opt.isNotBlank() }
        }
        request.validationRules?.let { entity.validationRules = it }
        request.displayOrder?.let { entity.displayOrder = it }
        entity.updatedAt = Instant.now()

        val saved = definitionRepository.save(entity)
        return CustomFieldDefinitionResponse.fromEntity(saved)
    }

    fun toggleStatus(id: UUID): CustomFieldDefinitionResponse {
        val tenantId = currentTenantId()
        val entity =
            definitionRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomFieldNotFoundException(id) }

        entity.isActive = !entity.isActive
        entity.updatedAt = Instant.now()
        val saved = definitionRepository.save(entity)
        return CustomFieldDefinitionResponse.fromEntity(saved)
    }

    fun reorderDefinitions(orders: List<FieldOrderDto>) {
        val tenantId = currentTenantId()
        val now = Instant.now()
        for (item in orders) {
            definitionRepository.findByIdAndTenantId(item.id, tenantId).ifPresent { def ->
                def.displayOrder = item.displayOrder
                def.updatedAt = now
                definitionRepository.save(def)
            }
        }
    }
}
