package org.opencontrol.erp.catalog.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.opencontrol.erp.catalog.domain.model.ProductTrackingMode
import org.opencontrol.erp.catalog.domain.model.ProductUnitConversion
import org.opencontrol.erp.catalog.domain.model.SpedItemType
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductCategoryResponse(
    val id: UUID,
    val parentId: UUID?,
    val code: String,
    val name: String,
    val colorHex: String,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: ProductCategory) =
            ProductCategoryResponse(
                id = entity.id,
                parentId = entity.parentId,
                code = entity.code,
                name = entity.name,
                colorHex = entity.colorHex,
                isActive = entity.isActive,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

data class CreateProductCategoryRequest(
    val parentId: UUID? = null,
    @field:NotBlank
    @field:Size(max = 50)
    val code: String,
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,
    val colorHex: String = "#10B981",
)

data class UpdateProductCategoryRequest(
    val parentId: UUID? = null,
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,
    val colorHex: String = "#10B981",
    val isActive: Boolean = true,
)

data class UnitConversionResponse(
    val id: UUID,
    val productId: UUID,
    val unitFrom: String,
    val unitTo: String,
    val conversionFactor: BigDecimal,
) {
    companion object {
        fun fromEntity(entity: ProductUnitConversion) =
            UnitConversionResponse(
                id = entity.id,
                productId = entity.productId,
                unitFrom = entity.unitFrom,
                unitTo = entity.unitTo,
                conversionFactor = entity.conversionFactor,
            )
    }
}

data class UnitConversionRequest(
    @field:NotBlank
    @field:Size(max = 10)
    val unitFrom: String,
    @field:NotBlank
    @field:Size(max = 10)
    val unitTo: String,
    @field:NotNull
    @field:Positive
    val conversionFactor: BigDecimal,
)

data class ProductResponse(
    val id: UUID,
    val sku: String,
    val barcode: String?,
    val name: String,
    val description: String?,
    val itemType: SpedItemType,
    val unitOfMeasure: String,
    val categoryId: UUID,
    val categoryName: String? = null,
    val brand: String?,
    val model: String?,
    val ncm: String,
    val ncmException: String?,
    val cest: String?,
    val origin: Int,
    val trackingMode: ProductTrackingMode,
    val grossWeight: BigDecimal?,
    val netWeight: BigDecimal?,
    val width: BigDecimal?,
    val height: BigDecimal?,
    val depth: BigDecimal?,
    val volume: BigDecimal?,
    val minStock: BigDecimal,
    val maxStock: BigDecimal?,
    val reorderPoint: BigDecimal?,
    val lastAcquisitionCost: BigDecimal,
    val averageCost: BigDecimal,
    val replacementCost: BigDecimal,
    val isActive: Boolean,
    val unitConversions: List<UnitConversionResponse> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(
            product: Product,
            categoryName: String? = null,
            conversions: List<ProductUnitConversion> = emptyList(),
        ) = ProductResponse(
            id = product.id,
            sku = product.sku,
            barcode = product.barcode,
            name = product.name,
            description = product.description,
            itemType = product.itemType,
            unitOfMeasure = product.unitOfMeasure,
            categoryId = product.categoryId,
            categoryName = categoryName,
            brand = product.brand,
            model = product.model,
            ncm = product.ncm,
            ncmException = product.ncmException,
            cest = product.cest,
            origin = product.origin,
            trackingMode = product.trackingMode,
            grossWeight = product.grossWeight,
            netWeight = product.netWeight,
            width = product.width,
            height = product.height,
            depth = product.depth,
            volume = product.volume,
            minStock = product.minStock,
            maxStock = product.maxStock,
            reorderPoint = product.reorderPoint,
            lastAcquisitionCost = product.lastAcquisitionCost,
            averageCost = product.averageCost,
            replacementCost = product.replacementCost,
            isActive = product.isActive,
            unitConversions = conversions.map { UnitConversionResponse.fromEntity(it) },
            createdAt = product.createdAt,
            updatedAt = product.updatedAt,
        )
    }
}

data class CreateProductRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val sku: String,
    val barcode: String? = null,
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    val description: String? = null,
    val itemType: SpedItemType = SpedItemType.REV_00,
    @field:NotBlank
    @field:Size(max = 10)
    val unitOfMeasure: String = "UN",
    @field:NotNull
    val categoryId: UUID,
    val brand: String? = null,
    val model: String? = null,
    @field:NotBlank
    val ncm: String,
    val ncmException: String? = null,
    val cest: String? = null,
    val origin: Int = 0,
    val trackingMode: ProductTrackingMode = ProductTrackingMode.NONE,
    val grossWeight: BigDecimal? = null,
    val netWeight: BigDecimal? = null,
    val width: BigDecimal? = null,
    val height: BigDecimal? = null,
    val depth: BigDecimal? = null,
    val volume: BigDecimal? = null,
    val minStock: BigDecimal = BigDecimal.ZERO,
    val maxStock: BigDecimal? = null,
    val reorderPoint: BigDecimal? = null,
    val lastAcquisitionCost: BigDecimal = BigDecimal.ZERO,
    val branchId: UUID? = null,
)

data class UpdateProductRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val sku: String,
    val barcode: String? = null,
    @field:NotBlank
    @field:Size(max = 200)
    val name: String,
    val description: String? = null,
    val itemType: SpedItemType = SpedItemType.REV_00,
    @field:NotBlank
    @field:Size(max = 10)
    val unitOfMeasure: String = "UN",
    @field:NotNull
    val categoryId: UUID,
    val brand: String? = null,
    val model: String? = null,
    @field:NotBlank
    val ncm: String,
    val ncmException: String? = null,
    val cest: String? = null,
    val origin: Int = 0,
    val trackingMode: ProductTrackingMode = ProductTrackingMode.NONE,
    val grossWeight: BigDecimal? = null,
    val netWeight: BigDecimal? = null,
    val width: BigDecimal? = null,
    val height: BigDecimal? = null,
    val depth: BigDecimal? = null,
    val volume: BigDecimal? = null,
    val minStock: BigDecimal = BigDecimal.ZERO,
    val maxStock: BigDecimal? = null,
    val reorderPoint: BigDecimal? = null,
    val lastAcquisitionCost: BigDecimal? = null,
    val isActive: Boolean = true,
)

data class ProductPageResponse(
    val items: List<ProductResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int,
)
