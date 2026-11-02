package org.opencontrol.erp.customfields.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.opencontrol.erp.customfields.domain.CustomFieldValidationRules
import org.opencontrol.erp.customfields.domain.FieldType
import java.time.Instant
import java.util.UUID

data class CreateCustomFieldDefinitionRequest(
    @field:NotBlank(message = "Target entity cannot be blank")
    val targetEntity: String,
    @field:NotBlank(message = "Field key cannot be blank")
    @field:Size(min = 2, max = 64, message = "Field key must be between 2 and 64 characters")
    @field:Pattern(
        regexp = "^[a-z0-9_]+$",
        message = "Field key must contain only lowercase alphanumeric characters and underscores",
    )
    val fieldKey: String,
    @field:NotBlank(message = "Label cannot be blank")
    @field:Size(max = 128, message = "Label cannot exceed 128 characters")
    val label: String,
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String? = null,
    @field:NotNull(message = "Field type is required")
    val fieldType: FieldType,
    val required: Boolean = false,
    val adminOnly: Boolean = false,
    val readOnly: Boolean = false,
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val validationRules: CustomFieldValidationRules? = null,
    val displayOrder: Int = 0,
)

data class UpdateCustomFieldDefinitionRequest(
    @field:NotBlank(message = "Label cannot be blank")
    @field:Size(max = 128, message = "Label cannot exceed 128 characters")
    val label: String,
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String? = null,
    val required: Boolean? = null,
    val adminOnly: Boolean? = null,
    val readOnly: Boolean? = null,
    val defaultValue: String? = null,
    val options: List<String>? = null,
    val validationRules: CustomFieldValidationRules? = null,
    val displayOrder: Int? = null,
)

data class FieldOrderDto(
    @field:NotNull
    val id: UUID,
    @field:NotNull
    val displayOrder: Int,
)

data class CustomFieldDefinitionResponse(
    val id: UUID,
    val tenantId: UUID,
    val targetEntity: String,
    val fieldKey: String,
    val label: String,
    val description: String?,
    val fieldType: FieldType,
    val required: Boolean,
    val adminOnly: Boolean,
    val readOnly: Boolean,
    val defaultValue: String?,
    val options: List<String>?,
    val validationRules: CustomFieldValidationRules?,
    val displayOrder: Int,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: CustomFieldDefinition) =
            CustomFieldDefinitionResponse(
                id = entity.id,
                tenantId = entity.tenantId,
                targetEntity = entity.targetEntity,
                fieldKey = entity.fieldKey,
                label = entity.label,
                description = entity.description,
                fieldType = entity.fieldType,
                required = entity.required,
                adminOnly = entity.adminOnly,
                readOnly = entity.readOnly,
                defaultValue = entity.defaultValue,
                options = entity.options,
                validationRules = entity.validationRules,
                displayOrder = entity.displayOrder,
                isActive = entity.isActive,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

data class SaveEntityCustomFieldsRequest(
    @field:NotNull(message = "Field values map cannot be null")
    val values: Map<String, Any?>,
)

data class EntityCustomFieldsResponse(
    val targetEntity: String,
    val entityId: UUID,
    val values: Map<String, Any?>,
    val updatedAt: Instant? = null,
)
