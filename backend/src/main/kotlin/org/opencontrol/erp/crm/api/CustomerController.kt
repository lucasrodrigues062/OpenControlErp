package org.opencontrol.erp.crm.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.crm.api.dto.CreateCustomerRequest
import org.opencontrol.erp.crm.api.dto.CustomerResponse
import org.opencontrol.erp.crm.api.dto.UpdateCustomerRequest
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.service.CustomerService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
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
@RequestMapping("/api/v1/crm/customers")
@Tag(name = "Customers", description = "Customer master data management (Individual and Legal Entity)")
class CustomerController(
    private val customerService: CustomerService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "List and search customers with pagination")
    fun list(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) type: CustomerType?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) economicGroupId: UUID?,
        @RequestParam(required = false) branchId: UUID?,
        @RequestParam(defaultValue = "false") activeOnly: Boolean,
        @PageableDefault(size = 20, sort = ["legalName"], direction = Sort.Direction.ASC) pageable: Pageable,
    ): ResponseEntity<Page<CustomerResponse>> =
        ResponseEntity.ok(
            customerService.listCustomers(
                query = query,
                type = type,
                categoryId = categoryId,
                economicGroupId = economicGroupId,
                branchId = branchId,
                activeOnly = activeOnly,
                pageable = pageable,
            ),
        )

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_READ') or hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Get customer details by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<CustomerResponse> = ResponseEntity.ok(customerService.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Register a new customer")
    fun create(
        @Valid @RequestBody request: CreateCustomerRequest,
    ): ResponseEntity<CustomerResponse> = ResponseEntity.status(HttpStatus.CREATED).body(customerService.createCustomer(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Update customer details")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateCustomerRequest,
    ): ResponseEntity<CustomerResponse> = ResponseEntity.ok(customerService.updateCustomer(id, request))

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CRM_WRITE') or hasAuthority('CRM_ADMIN')")
    @Operation(summary = "Toggle customer active status")
    fun toggleStatus(
        @PathVariable id: UUID,
        @RequestBody body: Map<String, Boolean>,
    ): ResponseEntity<CustomerResponse> {
        val active = body["active"] ?: true
        return ResponseEntity.ok(customerService.toggleStatus(id, active))
    }
}
