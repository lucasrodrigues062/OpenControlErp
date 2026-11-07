package org.opencontrol.erp.inventory.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import org.opencontrol.erp.inventory.domain.model.LotStatus
import org.opencontrol.erp.inventory.domain.model.ProductLot
import org.opencontrol.erp.inventory.domain.model.ProductSerialNumber
import org.opencontrol.erp.inventory.domain.model.SerialNumberStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class ProductLotResponse(
    val id: UUID,
    val warehouseId: UUID,
    val productId: UUID,
    val lotNumber: String,
    val manufacturingDate: LocalDate,
    val expirationDate: LocalDate,
    val quantityOnHand: BigDecimal,
    val quantityReserved: BigDecimal,
    val status: LotStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(lot: ProductLot) =
            ProductLotResponse(
                id = lot.id,
                warehouseId = lot.warehouseId,
                productId = lot.productId,
                lotNumber = lot.lotNumber,
                manufacturingDate = lot.manufacturingDate,
                expirationDate = lot.expirationDate,
                quantityOnHand = lot.quantityOnHand,
                quantityReserved = lot.quantityReserved,
                status = lot.status,
                createdAt = lot.createdAt,
                updatedAt = lot.updatedAt,
            )
    }
}

data class CreateLotRequest(
    @field:NotNull
    val warehouseId: UUID,
    @field:NotNull
    val productId: UUID,
    @field:NotBlank
    @field:Size(max = 100)
    val lotNumber: String,
    @field:NotNull
    val manufacturingDate: LocalDate,
    @field:NotNull
    val expirationDate: LocalDate,
    @field:PositiveOrZero
    val quantityOnHand: BigDecimal = BigDecimal.ZERO,
)

data class ProductSerialNumberResponse(
    val id: UUID,
    val warehouseId: UUID,
    val productId: UUID,
    val lotId: UUID?,
    val serialNumber: String,
    val status: SerialNumberStatus,
    val warrantyUntil: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(s: ProductSerialNumber) =
            ProductSerialNumberResponse(
                id = s.id,
                warehouseId = s.warehouseId,
                productId = s.productId,
                lotId = s.lotId,
                serialNumber = s.serialNumber,
                status = s.status,
                warrantyUntil = s.warrantyUntil,
                createdAt = s.createdAt,
                updatedAt = s.updatedAt,
            )
    }
}

data class CreateSerialNumberRequest(
    @field:NotNull
    val warehouseId: UUID,
    @field:NotNull
    val productId: UUID,
    val lotId: UUID? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val serialNumber: String,
    val warrantyUntil: LocalDate? = null,
)

data class UpdateSerialStatusRequest(
    @field:NotNull
    val status: SerialNumberStatus,
)
