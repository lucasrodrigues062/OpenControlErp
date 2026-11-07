package org.opencontrol.erp.catalog.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.catalog.api.dto.CreateProductCategoryRequest
import org.opencontrol.erp.catalog.api.dto.ProductCategoryResponse
import org.opencontrol.erp.catalog.api.dto.UpdateProductCategoryRequest
import org.opencontrol.erp.catalog.service.ProductCategoryService
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
@RequestMapping("/api/v1/catalog/categories")
@Tag(name = "Catalog - Categories", description = "Hierarchical product taxonomy categories")
class ProductCategoryController(
    private val service: ProductCategoryService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "List hierarchical product categories")
    fun list(
        @RequestParam(required = false) parentId: UUID?,
        @RequestParam(defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<List<ProductCategoryResponse>> = ResponseEntity.ok(service.listCategories(parentId, activeOnly))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Get product category by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<ProductCategoryResponse> = ResponseEntity.ok(service.getById(id))

    @PostMapping
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Create a product category")
    fun create(
        @Valid @RequestBody request: CreateProductCategoryRequest,
    ): ResponseEntity<ProductCategoryResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createCategory(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Update a product category")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProductCategoryRequest,
    ): ResponseEntity<ProductCategoryResponse> = ResponseEntity.ok(service.updateCategory(id, request))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a product category")
    fun delete(
        @PathVariable id: UUID,
    ) {
        service.deleteCategory(id)
    }
}
