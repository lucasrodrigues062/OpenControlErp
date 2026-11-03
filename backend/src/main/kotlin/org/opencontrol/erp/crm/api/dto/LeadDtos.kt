package org.opencontrol.erp.crm.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.model.Lead
import org.opencontrol.erp.crm.domain.model.LeadSource
import org.opencontrol.erp.crm.domain.model.LeadStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class LeadResponse(
    val id: UUID,
    val tenantId: UUID,
    val branchId: UUID?,
    val name: String,
    val companyName: String?,
    val email: String?,
    val phone: String?,
    val source: LeadSource,
    val status: LeadStatus,
    val estimatedBudget: BigDecimal,
    val interestSummary: String?,
    val assignedUserId: UUID?,
    val disqualificationReason: String?,
    val convertedCustomerId: UUID?,
    val convertedDealId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: Lead) =
            LeadResponse(
                id = entity.id,
                tenantId = entity.tenantId,
                branchId = entity.branchId,
                name = entity.name,
                companyName = entity.companyName,
                email = entity.email,
                phone = entity.phone,
                source = entity.source,
                status = entity.status,
                estimatedBudget = entity.estimatedBudget,
                interestSummary = entity.interestSummary,
                assignedUserId = entity.assignedUserId,
                disqualificationReason = entity.disqualificationReason,
                convertedCustomerId = entity.convertedCustomerId,
                convertedDealId = entity.convertedDealId,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
            )
    }
}

data class CreateLeadRequest(
    @field:NotBlank(message = "Lead name is required")
    val name: String,
    val companyName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val source: LeadSource = LeadSource.WEBSITE,
    val estimatedBudget: BigDecimal = BigDecimal.ZERO,
    val interestSummary: String? = null,
    val assignedUserId: UUID? = null,
    val branchId: UUID? = null,
)

data class DisqualifyLeadRequest(
    @field:NotBlank(message = "Disqualification reason is required")
    val reason: String,
)

data class ConvertLeadRequest(
    @field:NotNull(message = "Customer type is required")
    val customerType: CustomerType,
    @field:NotBlank(message = "Customer document (CPF/CNPJ) is required")
    val customerDocument: String,
    val customerLegalName: String? = null,
    @field:NotBlank(message = "Deal title is required")
    val dealTitle: String,
    val dealEstimatedValue: BigDecimal = BigDecimal.ZERO,
    @field:NotNull(message = "Pipeline ID is required")
    val pipelineId: UUID,
    @field:NotNull(message = "Stage ID is required")
    val stageId: UUID,
)

data class LeadConversionResponse(
    val leadId: UUID,
    val customerId: UUID,
    val dealId: UUID,
)
