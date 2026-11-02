package org.opencontrol.erp.companies.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@ResponseStatus(HttpStatus.NOT_FOUND)
class CompanyNotFoundException(
    message: String,
) : RuntimeException(message) {
    constructor(id: UUID) : this("Company or branch not found with id: $id")
}

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateCnpjException(
    cnpj: String,
) : RuntimeException("A company or branch with CNPJ $cnpj already exists in this tenant")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateCompanyCodeException(
    code: String,
) : RuntimeException("A company or branch with code $code already exists in this tenant")

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidCnpjException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class BranchHierarchyException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class CertificateProcessingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
