package org.opencontrol.erp.inventory.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.opencontrol.erp.inventory.domain.model.StorageLocation
import org.opencontrol.erp.inventory.domain.model.Warehouse
import org.opencontrol.erp.inventory.domain.model.WarehouseType
import java.time.Instant
import java.util.UUID

data class WarehouseResponse(
    val id: UUID,
    val branchId: UUID,
    val code: String,
    val name: String,
    val warehouseType: WarehouseType,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(w: Warehouse) =
            WarehouseResponse(
                id = w.id,
                branchId = w.branchId,
                code = w.code,
                name = w.name,
                warehouseType = w.warehouseType,
                isDefault = w.isDefault,
                isActive = w.isActive,
                createdAt = w.createdAt,
                updatedAt = w.updatedAt,
            )
    }
}

data class CreateWarehouseRequest(
    @field:NotNull
    val branchId: UUID,
    @field:NotBlank
    @field:Size(max = 50)
    val code: String,
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,
    val warehouseType: WarehouseType = WarehouseType.COMMERCIAL,
    val isDefault: Boolean = false,
)

data class UpdateWarehouseRequest(
    @field:NotNull
    val branchId: UUID,
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,
    val warehouseType: WarehouseType = WarehouseType.COMMERCIAL,
    val isDefault: Boolean = false,
    val isActive: Boolean = true,
)

data class StorageLocationResponse(
    val id: UUID,
    val warehouseId: UUID,
    val aisle: String,
    val rack: String,
    val shelf: String,
    val bin: String,
    val formattedAddress: String,
    val isActive: Boolean,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(loc: StorageLocation) =
            StorageLocationResponse(
                id = loc.id,
                warehouseId = loc.warehouseId,
                aisle = loc.aisle,
                rack = loc.rack,
                shelf = loc.shelf,
                bin = loc.bin,
                formattedAddress = loc.formattedAddress,
                isActive = loc.isActive,
                createdAt = loc.createdAt,
            )
    }
}

data class CreateStorageLocationRequest(
    @field:NotBlank
    @field:Size(max = 20)
    val aisle: String,
    @field:NotBlank
    @field:Size(max = 20)
    val rack: String,
    @field:NotBlank
    @field:Size(max = 20)
    val shelf: String,
    @field:NotBlank
    @field:Size(max = 20)
    val bin: String,
)
