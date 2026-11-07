package org.opencontrol.erp.inventory.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@ResponseStatus(HttpStatus.NOT_FOUND)
class WarehouseNotFoundException(
    id: UUID,
) : RuntimeException("Warehouse with ID '$id' was not found.")

@ResponseStatus(HttpStatus.NOT_FOUND)
class StorageLocationNotFoundException(
    id: UUID,
) : RuntimeException("Storage location with ID '$id' was not found.")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateWarehouseCodeException(
    code: String,
) : RuntimeException("A warehouse with code '$code' already exists in this tenant.")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateStorageLocationException(
    address: String,
) : RuntimeException("A storage location with address '$address' already exists in this warehouse.")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InsufficientStockException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class LotNotFoundException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class SerialNumberNotFoundException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateSerialNumberException(
    serial: String,
) : RuntimeException("Serial number '$serial' already registered for this product.")
