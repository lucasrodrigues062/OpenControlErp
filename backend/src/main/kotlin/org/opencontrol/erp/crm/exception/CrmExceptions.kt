package org.opencontrol.erp.crm.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import java.util.UUID

@ResponseStatus(HttpStatus.NOT_FOUND)
class CustomerNotFoundException(
    id: UUID,
) : RuntimeException("Customer not found with id: $id")

@ResponseStatus(HttpStatus.NOT_FOUND)
class CustomerCategoryNotFoundException(
    id: UUID,
) : RuntimeException("Customer category not found with id: $id")

@ResponseStatus(HttpStatus.NOT_FOUND)
class EconomicGroupNotFoundException(
    id: UUID,
) : RuntimeException("Economic group not found with id: $id")

@ResponseStatus(HttpStatus.NOT_FOUND)
class LeadNotFoundException(
    id: UUID,
) : RuntimeException("Lead not found with id: $id")

@ResponseStatus(HttpStatus.NOT_FOUND)
class PipelineNotFoundException(
    id: UUID,
) : RuntimeException("Sales pipeline not found with id: $id")

@ResponseStatus(HttpStatus.NOT_FOUND)
class PipelineStageNotFoundException(
    id: UUID,
) : RuntimeException("Pipeline stage not found with id: $id")

@ResponseStatus(HttpStatus.NOT_FOUND)
class DealNotFoundException(
    id: UUID,
) : RuntimeException("Deal not found with id: $id")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateCustomerDocumentException(
    doc: String,
) : RuntimeException("A customer with document $doc already exists in this tenant")

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateCodeException(
    code: String,
) : RuntimeException("An item with code $code already exists in this tenant")

@ResponseStatus(HttpStatus.CONFLICT)
class StageHasActiveDealsException(
    message: String = "Cannot delete stage because it contains active deals",
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidDocumentException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidLeadTransitionException(
    message: String,
) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class DealLossReasonRequiredException(
    message: String = "Loss reason is required when marking a deal as lost",
) : RuntimeException(message)
