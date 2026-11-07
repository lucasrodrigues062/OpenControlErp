package org.opencontrol.erp.catalog.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.catalog.api.dto.CreatePriceListRequest
import org.opencontrol.erp.catalog.api.dto.PriceListResponse
import org.opencontrol.erp.catalog.api.dto.ProductPriceResponse
import org.opencontrol.erp.catalog.api.dto.ResolvePriceResponse
import org.opencontrol.erp.catalog.api.dto.SetProductPriceRequest
import org.opencontrol.erp.catalog.service.PricingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Catalog - Pricing", description = "Price lists, markups, and promotional pricing schedules")
class PriceListController(
    private val service: PricingService,
) {
    @GetMapping("/price-lists")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "List commercial price lists")
    fun listPriceLists(): ResponseEntity<List<PriceListResponse>> = ResponseEntity.ok(service.listPriceLists())

    @PostMapping("/price-lists")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Create a commercial price list")
    fun createPriceList(
        @Valid @RequestBody request: CreatePriceListRequest,
    ): ResponseEntity<PriceListResponse> = ResponseEntity.status(HttpStatus.CREATED).body(service.createPriceList(request))

    @PutMapping("/price-lists/{priceListId}/prices/{productId}")
    @PreAuthorize("hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Set product price within a price list")
    fun setPrice(
        @PathVariable priceListId: UUID,
        @PathVariable productId: UUID,
        @Valid @RequestBody request: SetProductPriceRequest,
    ): ResponseEntity<ProductPriceResponse> = ResponseEntity.ok(service.setProductPrice(priceListId, productId, request))

    @GetMapping("/products/{productId}/prices")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Get all prices configured for a product across price lists")
    fun getProductPrices(
        @PathVariable productId: UUID,
    ): ResponseEntity<List<ProductPriceResponse>> = ResponseEntity.ok(service.getProductPrices(productId))

    @GetMapping("/products/{productId}/resolve-price")
    @PreAuthorize("hasAuthority('CATALOG_READ') or hasAuthority('CATALOG_WRITE')")
    @Operation(summary = "Resolve effective sales price considering promotional validity schedules")
    fun resolvePrice(
        @PathVariable productId: UUID,
        @RequestParam(required = false) priceListId: UUID?,
    ): ResponseEntity<ResolvePriceResponse> = ResponseEntity.ok(service.resolveEffectivePrice(productId, priceListId))
}
