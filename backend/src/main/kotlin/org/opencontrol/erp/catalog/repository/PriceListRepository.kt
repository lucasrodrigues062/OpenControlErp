package org.opencontrol.erp.catalog.repository

import org.opencontrol.erp.catalog.domain.model.PriceList
import org.opencontrol.erp.catalog.domain.model.ProductPrice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface PriceListRepository : JpaRepository<PriceList, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<PriceList>

    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<PriceList>

    fun findByTenantIdAndIsDefaultTrue(tenantId: UUID): Optional<PriceList>

    fun existsByTenantIdAndCode(
        tenantId: UUID,
        code: String,
    ): Boolean
}

@Repository
interface ProductPriceRepository : JpaRepository<ProductPrice, UUID> {
    fun findByIdAndTenantId(
        id: UUID,
        tenantId: UUID,
    ): Optional<ProductPrice>

    fun findByPriceListIdAndProductId(
        priceListId: UUID,
        productId: UUID,
    ): Optional<ProductPrice>

    fun findAllByTenantIdAndProductId(
        tenantId: UUID,
        productId: UUID,
    ): List<ProductPrice>

    fun findAllByTenantIdAndPriceListId(
        tenantId: UUID,
        priceListId: UUID,
    ): List<ProductPrice>
}
