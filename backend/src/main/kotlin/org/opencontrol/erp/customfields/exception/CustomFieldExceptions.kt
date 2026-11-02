package org.opencontrol.erp.customfields.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@ResponseStatus(HttpStatus.NOT_FOUND)
class CustomFieldNotFoundException(
    id: UUID,
) : RuntimeException("Custom field definition not found for ID: $id")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateFieldKeyException(
    targetEntity: String,
    fieldKey: String,
) : RuntimeException("A custom field with key '$fieldKey' already exists for entity '$targetEntity'")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidFieldValueException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class FieldSecurityException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class UnsupportedTargetEntityException(
    targetEntity: String,
) : RuntimeException("Target entity '$targetEntity' is not registered for custom fields")
