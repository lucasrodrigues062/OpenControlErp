package org.opencontrol.erp.inventory.service

import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.inventory.api.dto.StockBalanceResponse
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.exception.InsufficientStockException
import org.opencontrol.erp.inventory.repository.StockBalanceRepository
import org.opencontrol.erp.inventory.repository.StorageLocationRepository
import org.opencontrol.erp.inventory.repository.WarehouseRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class StockBalanceService(
    private val balanceRepository: StockBalanceRepository,
    private val warehouseRepository: WarehouseRepository,
    private val productRepository: ProductRepository,
    private val locationRepository: StorageLocationRepository,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listBalances(
        warehouseId: UUID? = null,
        productId: UUID? = null,
    ): List<StockBalanceResponse> {
        val tenantId = getCurrentTenantId()
        val list =
            when {
                warehouseId != null && productId != null -> {
                    balanceRepository
                        .findAllByTenantIdAndWarehouseId(tenantId, warehouseId)
                        .filter { it.productId == productId }
                }
                warehouseId != null -> balanceRepository.findAllByTenantIdAndWarehouseId(tenantId, warehouseId)
                productId != null -> balanceRepository.findAllByTenantIdAndProductId(tenantId, productId)
                else -> balanceRepository.findAllByTenantId(tenantId)
            }

        return list.map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getProductBalances(productId: UUID): List<StockBalanceResponse> {
        val tenantId = getCurrentTenantId()
        return balanceRepository
            .findAllByTenantIdAndProductId(tenantId, productId)
            .map { toResponse(it) }
    }

    fun getOrCreateBalance(
        warehouseId: UUID,
        productId: UUID,
        locationId: UUID?,
    ): StockBalance {
        val tenantId = getCurrentTenantId()
        val existing =
            if (locationId != null) {
                balanceRepository.findByWarehouseIdAndProductIdAndLocationId(warehouseId, productId, locationId)
            } else {
                balanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(warehouseId, productId)
            }

        return existing.orElseGet {
            val newBalance =
                StockBalance(
                    tenantId = tenantId,
                    warehouseId = warehouseId,
                    productId = productId,
                    locationId = locationId,
                    quantityOnHand = BigDecimal.ZERO,
                    quantityReserved = BigDecimal.ZERO,
                    quantityAvailable = BigDecimal.ZERO,
                )
            balanceRepository.save(newBalance)
        }
    }

    fun adjustStock(
        warehouseId: UUID,
        productId: UUID,
        locationId: UUID?,
        deltaQuantity: BigDecimal,
    ): StockBalance {
        val balance = getOrCreateBalance(warehouseId, productId, locationId)
        val newOnHand = balance.quantityOnHand.add(deltaQuantity)

        if (newOnHand < BigDecimal.ZERO) {
            throw InsufficientStockException(
                "Saldo insuficiente no depósito $warehouseId para o produto $productId. Saldo atual: ${balance.quantityOnHand}, solicitado: $deltaQuantity",
            )
        }

        balance.quantityOnHand = newOnHand
        balance.quantityAvailable = balance.quantityOnHand.subtract(balance.quantityReserved)
        balance.updatedAt = Instant.now()

        return balanceRepository.save(balance)
    }

    fun reserveStock(
        warehouseId: UUID,
        productId: UUID,
        locationId: UUID?,
        quantity: BigDecimal,
    ): StockBalance {
        val balance = getOrCreateBalance(warehouseId, productId, locationId)
        val available = balance.quantityOnHand.subtract(balance.quantityReserved)

        if (available < quantity) {
            throw InsufficientStockException(
                "Estoque disponível insuficiente para reserva. Disponível: $available, solicitado: $quantity",
            )
        }

        balance.quantityReserved = balance.quantityReserved.add(quantity)
        balance.quantityAvailable = balance.quantityOnHand.subtract(balance.quantityReserved)
        balance.updatedAt = Instant.now()

        return balanceRepository.save(balance)
    }

    fun releaseReservedStock(
        warehouseId: UUID,
        productId: UUID,
        locationId: UUID?,
        quantity: BigDecimal,
    ): StockBalance {
        val balance = getOrCreateBalance(warehouseId, productId, locationId)
        val newReserved = balance.quantityReserved.subtract(quantity).coerceAtLeast(BigDecimal.ZERO)

        balance.quantityReserved = newReserved
        balance.quantityAvailable = balance.quantityOnHand.subtract(balance.quantityReserved)
        balance.updatedAt = Instant.now()

        return balanceRepository.save(balance)
    }

    private fun toResponse(balance: StockBalance): StockBalanceResponse {
        val warehouse = warehouseRepository.findById(balance.warehouseId).orElse(null)
        val product = productRepository.findById(balance.productId).orElse(null)
        val location = balance.locationId?.let { locationRepository.findById(it).orElse(null) }

        return StockBalanceResponse.fromEntity(
            b = balance,
            warehouseName = warehouse?.name,
            productSku = product?.sku,
            productName = product?.name,
            locationAddress = location?.formattedAddress,
        )
    }
}
