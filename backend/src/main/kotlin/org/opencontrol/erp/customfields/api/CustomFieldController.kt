package org.opencontrol.erp.customfields.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.customfields.api.dto.CreateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.api.dto.CustomFieldDefinitionResponse
import org.opencontrol.erp.customfields.api.dto.EntityCustomFieldsResponse
import org.opencontrol.erp.customfields.api.dto.FieldOrderDto
import org.opencontrol.erp.customfields.api.dto.SaveEntityCustomFieldsRequest
import org.opencontrol.erp.customfields.api.dto.UpdateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.service.CustomFieldDefinitionService
import org.opencontrol.erp.customfields.service.CustomFieldEntityRegistry
import org.opencontrol.erp.customfields.service.CustomFieldValueService
import org.opencontrol.erp.customfields.service.TargetEntityDescriptor
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/custom-fields")
@Tag(name = "Custom Fields", description = "Dynamic Multi-Tenant Custom Fields Management")
class CustomFieldController(
    private val definitionService: CustomFieldDefinitionService,
    private val valueService: CustomFieldValueService,
    private val entityRegistry: CustomFieldEntityRegistry,
) {
    @GetMapping("/entities")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_READ') or hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "List all registered target entities eligible for custom fields")
    fun listEntities(): ResponseEntity<List<TargetEntityDescriptor>> = ResponseEntity.ok(entityRegistry.listEntities())

    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_READ') or hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "List custom field definitions for a target entity")
    fun listDefinitions(
        @RequestParam targetEntity: String,
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
    ): ResponseEntity<List<CustomFieldDefinitionResponse>> = ResponseEntity.ok(definitionService.listDefinitions(targetEntity, activeOnly))

    @GetMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_READ') or hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Get custom field definition by ID")
    fun getDefinitionById(
        @PathVariable id: UUID,
    ): ResponseEntity<CustomFieldDefinitionResponse> = ResponseEntity.ok(definitionService.getDefinitionById(id))

    @PostMapping("/definitions")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Create a new custom field definition")
    fun createDefinition(
        @Valid @RequestBody request: CreateCustomFieldDefinitionRequest,
    ): ResponseEntity<CustomFieldDefinitionResponse> {
        val created = definitionService.createDefinition(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/definitions/{id}")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Update an existing custom field definition")
    fun updateDefinition(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCustomFieldDefinitionRequest,
    ): ResponseEntity<CustomFieldDefinitionResponse> = ResponseEntity.ok(definitionService.updateDefinition(id, request))

    @PatchMapping("/definitions/{id}/toggle-status")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Toggle active/inactive status of a custom field definition")
    fun toggleStatus(
        @PathVariable id: UUID,
    ): ResponseEntity<CustomFieldDefinitionResponse> = ResponseEntity.ok(definitionService.toggleStatus(id))

    @PutMapping("/definitions/reorder")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Reorder custom field definitions for an entity")
    fun reorderDefinitions(
        @Valid @RequestBody orders: List<FieldOrderDto>,
    ): ResponseEntity<Void> {
        definitionService.reorderDefinitions(orders)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/values/{targetEntity}/{entityId}")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_READ') or hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Retrieve custom field values for an entity instance")
    fun getEntityCustomValues(
        @PathVariable targetEntity: String,
        @PathVariable entityId: UUID,
    ): ResponseEntity<EntityCustomFieldsResponse> = ResponseEntity.ok(valueService.getEntityCustomValues(targetEntity, entityId))

    @PutMapping("/values/{targetEntity}/{entityId}")
    @PreAuthorize("hasAuthority('CUSTOM_FIELDS_READ') or hasAuthority('CUSTOM_FIELDS_MANAGE')")
    @Operation(summary = "Save or update custom field values for an entity instance")
    fun saveEntityCustomValues(
        @PathVariable targetEntity: String,
        @PathVariable entityId: UUID,
        @Valid @RequestBody request: SaveEntityCustomFieldsRequest,
    ): ResponseEntity<EntityCustomFieldsResponse> {
        val saved = valueService.saveEntityCustomValues(targetEntity, entityId, request.values)
        return ResponseEntity.ok(saved)
    }
}
