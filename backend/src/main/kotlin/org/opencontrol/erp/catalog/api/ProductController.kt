package org.opencontrol.erp.catalog.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.catalog.api.dto.CreateProductRequest
import org.opencontrol.erp.catalog.api.dto.ProductPageResponse
import org.opencontrol.erp.catalog.api.dto.ProductResponse
import org.opencontrol.erp.catalog.api.dto.UnitConversionRequest
import org.opencontrol.erp.catalog.api.dto.UnitConversionResponse
import org.opencontrol.erp.catalog.api.dto.UpdateProductRequest
import org.opencontrol.erp.catalog.domain.model.ProductTrackingMode
import org.opencontrol.erp.catalog.service.ProductService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
@RequestMapping("/api/v1/catalog/products")
@Tag(name = "Catalog - Products", description = "Product master data and Brazilian taxation parameters")
class ProductController(
    private val service: ProductService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Search and list products")
    fun search(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) ncm: String?,
        @RequestParam(required = false) trackingMode: ProductTrackingMode?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<ProductPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"))
        return ResponseEntity.ok(service.searchProducts(query, categoryId, ncm, trackingMode, pageable))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Get product by ID")
    fun getById(
        @PathVariable id: UUID,
    ): ResponseEntity<ProductResponse> = ResponseEntity.ok(service.getById(id))

    @GetMapping("/by-barcode/{barcode}")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Look up product by barcode")
    fun getByBarcode(
        @PathVariable barcode: String,
    ): ResponseEntity<ProductResponse> = ResponseEntity.ok(service.getByBarcode(barcode))

    @PostMapping
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Create a new product")
    fun create(
        @Valid @RequestBody request: CreateProductRequest,
    ): ResponseEntity<ProductResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createProduct(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Update an existing product")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProductRequest,
    ): ResponseEntity<ProductResponse> = ResponseEntity.ok(service.updateProduct(id, request))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a product")
    fun delete(
        @PathVariable id: UUID,
    ) {
        service.deleteProduct(id)
    }

    @PostMapping("/{id}/unit-conversions")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Configure a unit of measure conversion")
    fun addUnitConversion(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UnitConversionRequest,
    ): ResponseEntity<UnitConversionResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.addUnitConversion(id, request))

    @GetMapping("/{id}/unit-conversions")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "List packaging conversions for product")
    fun listUnitConversions(
        @PathVariable id: UUID,
    ): ResponseEntity<List<UnitConversionResponse>> = ResponseEntity.ok(service.listUnitConversions(id))
}
