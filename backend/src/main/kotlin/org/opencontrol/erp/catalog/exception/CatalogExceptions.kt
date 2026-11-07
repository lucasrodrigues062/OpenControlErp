package org.opencontrol.erp.catalog.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProductNotFoundException(
    id: UUID,
) : RuntimeException("Product with ID '$id' was not found.")

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProductBarcodeNotFoundException(
    barcode: String,
) : RuntimeException("Product with barcode '$barcode' was not found.")

@ResponseStatus(HttpStatus.NOT_FOUND)
class ProductCategoryNotFoundException(
    id: UUID,
) : RuntimeException("Product category with ID '$id' was not found.")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateSkuException(
    sku: String,
) : RuntimeException("A product with SKU '$sku' already exists.")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateBarcodeException(
    barcode: String,
) : RuntimeException("A product with barcode '$barcode' already exists.")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateCategoryCodeException(
    code: String,
) : RuntimeException("A category with code '$code' already exists.")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidFiscalDataException(
    message: String,
) : RuntimeException(message)
