package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.CreateCustomerCategoryRequest
import org.opencontrol.erp.crm.api.dto.CustomerCategoryResponse
import org.opencontrol.erp.crm.service.CustomerCategoryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/customer-categories")
@Tag(name = "Customer Categories", description = "Customer segmentation and commercial category taxonomy")
class CustomerCategoryController(
    private val service: CustomerCategoryService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List all customer categories")
    fun list(
        @RequestParam(defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<List<CustomerCategoryResponse>> = ResponseEntity.ok(service.listCategories(activeOnly))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get customer category by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<CustomerCategoryResponse> = ResponseEntity.ok(service.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Create customer category")
    fun create(
        @Valid @RequestBody request: CreateCustomerCategoryRequest,
    ): ResponseEntity<CustomerCategoryResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createCategory(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update customer category")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreateCustomerCategoryRequest,
    ): ResponseEntity<CustomerCategoryResponse> = ResponseEntity.ok(service.updateCategory(id, request))

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Deactivate customer category")
    fun delete(
        @PathVariable id: UUID,
    ) {
        service.deleteCategory(id)
    }
}
