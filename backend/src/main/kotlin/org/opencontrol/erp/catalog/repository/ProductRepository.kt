package org.opencontrol.erp.catalog.repository

import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.ProductUnitConversion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface ProductRepository :
    JpaRepository<Product, UUID>,
    JpaSpecificationExecutor<Product> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<Product>

    fun findByTenantIdAndBarcode(
        tenantId: UUID,
        barcode: String,
    ): Optional<Product>

    fun findByTenantIdAndSku(
        tenantId: UUID,
        sku: String,
    ): Optional<Product>

    fun existsByTenantIdAndSku(
        tenantId: UUID,
        sku: String,
    ): Boolean

    fun existsByTenantIdAndBarcode(
        tenantId: UUID,
        barcode: String,
    ): Boolean
}

@Repository
interface ProductUnitConversionRepository : JpaRepository<ProductUnitConversion, UUID> {
    fun findAllByTenantIdAndProductId(
        tenantId: UUID,
        productId: UUID,
    ): List<ProductUnitConversion>

    fun findByTenantIdAndProductIdAndUnitFromAndUnitTo(
        tenantId: UUID,
        productId: UUID,
        unitFrom: String,
        unitTo: String,
    ): Optional<ProductUnitConversion>

    fun existsByTenantIdAndProductIdAndUnitFromAndUnitTo(
        tenantId: UUID,
        productId: UUID,
        unitFrom: String,
        unitTo: String,
    ): Boolean
}
