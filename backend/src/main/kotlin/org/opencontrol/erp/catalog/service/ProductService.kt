package org.opencontrol.erp.catalog.service

import jakarta.persistence.criteria.Predicate
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.api.dto.CreateProductRequest
import org.opencontrol.erp.catalog.api.dto.ProductPageResponse
import org.opencontrol.erp.catalog.api.dto.ProductResponse
import org.opencontrol.erp.catalog.api.dto.UnitConversionRequest
import org.opencontrol.erp.catalog.api.dto.UnitConversionResponse
import org.opencontrol.erp.catalog.api.dto.UpdateProductRequest
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.ProductTrackingMode
import org.opencontrol.erp.catalog.domain.model.ProductUnitConversion
import org.opencontrol.erp.catalog.domain.validation.BarcodeValidator
import org.opencontrol.erp.catalog.exception.DuplicateBarcodeException
import org.opencontrol.erp.catalog.exception.DuplicateSkuException
import org.opencontrol.erp.catalog.exception.InvalidFiscalDataException
import org.opencontrol.erp.catalog.exception.ProductBarcodeNotFoundException
import org.opencontrol.erp.catalog.exception.ProductCategoryNotFoundException
import org.opencontrol.erp.catalog.exception.ProductNotFoundException
import org.opencontrol.erp.catalog.repository.ProductCategoryRepository
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.repository.ProductUnitConversionRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: ProductCategoryRepository,
    private val unitConversionRepository: ProductUnitConversionRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun searchProducts(
        query: String? = null,
        categoryId: UUID? = null,
        ncm: String? = null,
        trackingMode: ProductTrackingMode? = null,
        pageable: Pageable,
    ): ProductPageResponse {
        val tenantId = getCurrentTenantId()

        val spec =
            Specification<Product> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<UUID>("tenantId"), tenantId))

                if (!query.isNullOrBlank()) {
                    val searchPattern = "%${query.trim().lowercase()}%"
                    val nameMatch = cb.like(cb.lower(root.get("name")), searchPattern)
                    val skuMatch = cb.like(cb.lower(root.get("sku")), searchPattern)
                    val barcodeMatch = cb.like(cb.lower(root.get("barcode")), searchPattern)
                    predicates.add(cb.or(nameMatch, skuMatch, barcodeMatch))
                }

                if (categoryId != null) {
                    predicates.add(cb.equal(root.get<UUID>("categoryId"), categoryId))
                }

                if (!ncm.isNullOrBlank()) {
                    val cleanNcm = ncm.trim().replace(".", "")
                    predicates.add(cb.equal(root.get<String>("ncm"), cleanNcm))
                }

                if (trackingMode != null) {
                    predicates.add(cb.equal(root.get<ProductTrackingMode>("trackingMode"), trackingMode))
                }

                cb.and(*predicates.toTypedArray())
            }

        val page = productRepository.findAll(spec, pageable)
        val items = page.content.map { toProductResponse(it) }

        return ProductPageResponse(
            items = items,
            totalElements = page.totalElements,
            totalPages = page.totalPages,
            page = page.number,
            size = page.size,
        )
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): ProductResponse {
        val tenantId = getCurrentTenantId()
        val product =
            productRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { ProductNotFoundException(id) }
        return toProductResponse(product)
    }

    @Transactional(readOnly = true)
    fun getByBarcode(barcode: String): ProductResponse {
        val tenantId = getCurrentTenantId()
        val cleanedBarcode = barcode.trim()
        val product =
            productRepository
                .findByTenantIdAndBarcode(tenantId, cleanedBarcode)
                .orElseThrow { ProductBarcodeNotFoundException(cleanedBarcode) }
        return toProductResponse(product)
    }

    fun createProduct(request: CreateProductRequest): ProductResponse {
        val tenantId = getCurrentTenantId()
        val sku = request.sku.trim().uppercase()
        val barcode = request.barcode?.trim()?.ifBlank { null }
        val cleanNcm = request.ncm.trim().replace(".", "")
        val cleanCest =
            request.cest
                ?.trim()
                ?.replace(".", "")
                ?.ifBlank { null }

        validateProductFields(tenantId, null, sku, barcode, cleanNcm, cleanCest, request.origin, request.categoryId)

        val product =
            Product(
                tenantId = tenantId,
                branchId = request.branchId,
                sku = sku,
                barcode = barcode,
                name = request.name.trim(),
                description = request.description?.trim(),
                itemType = request.itemType,
                unitOfMeasure = request.unitOfMeasure.trim().uppercase(),
                categoryId = request.categoryId,
                brand = request.brand?.trim(),
                model = request.model?.trim(),
                ncm = cleanNcm,
                ncmException = request.ncmException?.trim(),
                cest = cleanCest,
                origin = request.origin,
                trackingMode = request.trackingMode,
                grossWeight = request.grossWeight,
                netWeight = request.netWeight,
                width = request.width,
                height = request.height,
                depth = request.depth,
                volume = request.volume,
                minStock = request.minStock,
                maxStock = request.maxStock,
                reorderPoint = request.reorderPoint,
                lastAcquisitionCost = request.lastAcquisitionCost,
                averageCost = request.lastAcquisitionCost,
                replacementCost = request.lastAcquisitionCost,
                isActive = true,
            )

        val saved = productRepository.save(product)

        auditService.recordAudit(
            entityType = "Product",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created product ${saved.sku} - ${saved.name}",
            stateAfter = saved,
        )

        return toProductResponse(saved)
    }

    fun updateProduct(
        id: UUID,
        request: UpdateProductRequest,
    ): ProductResponse {
        val tenantId = getCurrentTenantId()
        val product =
            productRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { ProductNotFoundException(id) }

        val sku = request.sku.trim().uppercase()
        val barcode = request.barcode?.trim()?.ifBlank { null }
        val cleanNcm = request.ncm.trim().replace(".", "")
        val cleanCest =
            request.cest
                ?.trim()
                ?.replace(".", "")
                ?.ifBlank { null }

        validateProductFields(tenantId, id, sku, barcode, cleanNcm, cleanCest, request.origin, request.categoryId)

        val beforeState = toProductResponse(product)

        product.sku = sku
        product.barcode = barcode
        product.name = request.name.trim()
        product.description = request.description?.trim()
        product.itemType = request.itemType
        product.unitOfMeasure = request.unitOfMeasure.trim().uppercase()
        product.categoryId = request.categoryId
        product.brand = request.brand?.trim()
        product.model = request.model?.trim()
        product.ncm = cleanNcm
        product.ncmException = request.ncmException?.trim()
        product.cest = cleanCest
        product.origin = request.origin
        product.trackingMode = request.trackingMode
        product.grossWeight = request.grossWeight
        product.netWeight = request.netWeight
        product.width = request.width
        product.height = request.height
        product.depth = request.depth
        product.volume = request.volume
        product.minStock = request.minStock
        product.maxStock = request.maxStock
        product.reorderPoint = request.reorderPoint
        if (request.lastAcquisitionCost != null) {
            product.lastAcquisitionCost = request.lastAcquisitionCost
        }
        product.isActive = request.isActive
        product.updatedAt = Instant.now()

        val saved = productRepository.save(product)

        auditService.recordAudit(
            entityType = "Product",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated product ${saved.sku}",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        return toProductResponse(saved)
    }

    fun deleteProduct(id: UUID) {
        val tenantId = getCurrentTenantId()
        val product =
            productRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { ProductNotFoundException(id) }

        product.isActive = false
        product.updatedAt = Instant.now()
        productRepository.save(product)

        auditService.recordAudit(
            entityType = "Product",
            entityId = id.toString(),
            action = "DEACTIVATE",
            details = "Deactivated product ${product.sku} - ${product.name}",
        )
    }

    fun addUnitConversion(
        productId: UUID,
        request: UnitConversionRequest,
    ): UnitConversionResponse {
        val tenantId = getCurrentTenantId()
        val product =
            productRepository
                .findByIdAndTenantId(productId, tenantId)
                .orElseThrow { ProductNotFoundException(productId) }

        val unitFrom = request.unitFrom.trim().uppercase()
        val unitTo = request.unitTo.trim().uppercase()

        if (unitFrom == unitTo) {
            throw IllegalArgumentException("Cannot create conversion factor between identical units ($unitFrom).")
        }

        val existing =
            unitConversionRepository.findByTenantIdAndProductIdAndUnitFromAndUnitTo(
                tenantId,
                productId,
                unitFrom,
                unitTo,
            )

        val conversion =
            existing.orElseGet {
                ProductUnitConversion(
                    tenantId = tenantId,
                    productId = product.id,
                    unitFrom = unitFrom,
                    unitTo = unitTo,
                    conversionFactor = request.conversionFactor,
                )
            }
        conversion.conversionFactor = request.conversionFactor
        val saved = unitConversionRepository.save(conversion)

        auditService.recordAudit(
            entityType = "ProductUnitConversion",
            entityId = saved.id.toString(),
            action = "CREATE_OR_UPDATE",
            details = "Configured conversion for product ${product.sku}: 1 $unitFrom = ${request.conversionFactor} $unitTo",
        )

        return UnitConversionResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun listUnitConversions(productId: UUID): List<UnitConversionResponse> {
        val tenantId = getCurrentTenantId()
        return unitConversionRepository
            .findAllByTenantIdAndProductId(tenantId, productId)
            .map { UnitConversionResponse.fromEntity(it) }
    }

    private fun validateProductFields(
        tenantId: UUID,
        currentProductId: UUID?,
        sku: String,
        barcode: String?,
        cleanNcm: String,
        cleanCest: String?,
        origin: Int,
        categoryId: UUID,
    ) {
        // Validate Category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw ProductCategoryNotFoundException(categoryId)
        }

        // Validate SKU uniqueness
        val existingBySku = productRepository.findByTenantIdAndSku(tenantId, sku)
        if (existingBySku.isPresent && existingBySku.get().id != currentProductId) {
            throw DuplicateSkuException(sku)
        }

        // Validate Barcode
        if (barcode != null) {
            if (!BarcodeValidator.isValid(barcode)) {
                throw InvalidFiscalDataException("Invalid barcode format or check digit: '$barcode'")
            }
            val existingByBarcode = productRepository.findByTenantIdAndBarcode(tenantId, barcode)
            if (existingByBarcode.isPresent && existingByBarcode.get().id != currentProductId) {
                throw DuplicateBarcodeException(barcode)
            }
        }

        // Validate Brazilian statutory fiscal data
        if (!BarcodeValidator.isValidNcm(cleanNcm)) {
            throw InvalidFiscalDataException("Invalid NCM '$cleanNcm'. NCM must be exactly 8 numeric digits.")
        }

        if (cleanCest != null && !BarcodeValidator.isValidCest(cleanCest)) {
            throw InvalidFiscalDataException("Invalid CEST '$cleanCest'. CEST must be exactly 7 numeric digits.")
        }

        if (!BarcodeValidator.isValidOrigin(origin)) {
            throw InvalidFiscalDataException("Invalid product origin '$origin'. Must be between 0 and 8.")
        }
    }

    private fun toProductResponse(product: Product): ProductResponse {
        val categoryName =
            categoryRepository
                .findById(product.categoryId)
                .map { it.name }
                .orElse(null)
        val conversions = unitConversionRepository.findAllByTenantIdAndProductId(product.tenantId, product.id)
        return ProductResponse.fromEntity(product, categoryName, conversions)
    }
}
