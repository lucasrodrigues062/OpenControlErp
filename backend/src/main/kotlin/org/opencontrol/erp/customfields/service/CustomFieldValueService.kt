package org.opencontrol.erp.customfields.service

import org.opencontrol.erp.customfields.api.dto.EntityCustomFieldsResponse
import org.opencontrol.erp.customfields.domain.EntityCustomFields
import org.opencontrol.erp.customfields.exception.FieldSecurityException
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import org.opencontrol.erp.customfields.repository.CustomFieldDefinitionRepository
import org.opencontrol.erp.customfields.repository.EntityCustomFieldsRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CustomFieldValueService(
    private val entityCustomFieldsRepository: EntityCustomFieldsRepository,
    private val definitionRepository: CustomFieldDefinitionRepository,
    private val entityRegistry: CustomFieldEntityRegistry,
    private val validator: CustomFieldValidator,
) {
    private fun currentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    private fun isCurrentUserAdmin(): Boolean {
        val roles = SecurityContextHolder.getContext()?.roles ?: emptySet()
        return roles.any { it.contains("ADMIN", ignoreCase = true) }
    }

    @Transactional(readOnly = true)
    fun getEntityCustomValues(
        targetEntity: String,
        entityId: UUID,
    ): EntityCustomFieldsResponse {
        val tenantId = currentTenantId()
        val normalizedEntity = targetEntity.trim().uppercase()
        entityRegistry.validateRegistered(normalizedEntity)

        val definitions =
            definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(
                tenantId,
                normalizedEntity,
            )
        val adminDefs = definitions.filter { it.adminOnly }.map { it.fieldKey }.toSet()
        val isAdmin = isCurrentUserAdmin()

        val record =
            entityCustomFieldsRepository.findByTenantIdAndTargetEntityAndEntityId(
                tenantId,
                normalizedEntity,
                entityId,
            )

        val rawValues = record.map { it.fieldValues }.orElse(mutableMapOf())

        val filteredValues =
            if (isAdmin) {
                rawValues.toMap()
            } else {
                rawValues.filterKeys { it !in adminDefs }
            }

        return EntityCustomFieldsResponse(
            targetEntity = normalizedEntity,
            entityId = entityId,
            values = filteredValues,
            updatedAt = record.map { it.updatedAt }.orElse(null),
        )
    }

    fun saveEntityCustomValues(
        targetEntity: String,
        entityId: UUID,
        incomingValues: Map<String, Any?>,
    ): EntityCustomFieldsResponse {
        val tenantId = currentTenantId()
        val normalizedEntity = targetEntity.trim().uppercase()
        entityRegistry.validateRegistered(normalizedEntity)

        val definitions =
            definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(
                tenantId,
                normalizedEntity,
            )
        val activeDefs = definitions.filter { it.isActive }
        val defsByKey = definitions.associateBy { it.fieldKey }

        val isAdmin = isCurrentUserAdmin()

        // 1. Check existing record for read-only fields & admin-only enforcement
        val existingRecordOpt =
            entityCustomFieldsRepository.findByTenantIdAndTargetEntityAndEntityId(
                tenantId,
                normalizedEntity,
                entityId,
            )
        val isExistingRecord = existingRecordOpt.isPresent
        val existingValues = existingRecordOpt.map { it.fieldValues }.orElse(mutableMapOf())

        for ((key, newVal) in incomingValues) {
            val def = defsByKey[key] ?: continue

            // Admin-only check
            if (def.adminOnly && !isAdmin) {
                val oldVal = existingValues[key]
                if (oldVal != newVal) {
                    throw FieldSecurityException("Field '${def.label}' requires administrative privileges to modify")
                }
            }

            // Read-only immutability check on existing entity
            if (def.readOnly && isExistingRecord) {
                val oldVal = existingValues[key]
                if (oldVal != null && oldVal != newVal) {
                    throw InvalidFieldValueException("Field '${def.label}' is read-only and cannot be modified after initial creation")
                }
            }
        }

        // 2. Validate and sanitize active field values
        val sanitized = validator.validateAndSanitize(activeDefs, incomingValues)

        // 3. Persist entity custom fields record
        val record =
            existingRecordOpt.orElseGet {
                EntityCustomFields(
                    tenantId = tenantId,
                    targetEntity = normalizedEntity,
                    entityId = entityId,
                    fieldValues = mutableMapOf(),
                )
            }

        record.fieldValues.putAll(sanitized)
        record.updatedAt = Instant.now()
        val saved = entityCustomFieldsRepository.save(record)

        val filteredValues =
            if (isAdmin) {
                saved.fieldValues.toMap()
            } else {
                val adminDefs = definitions.filter { it.adminOnly }.map { it.fieldKey }.toSet()
                saved.fieldValues.filterKeys { it !in adminDefs }
            }

        return EntityCustomFieldsResponse(
            targetEntity = normalizedEntity,
            entityId = entityId,
            values = filteredValues,
            updatedAt = saved.updatedAt,
        )
    }
}
