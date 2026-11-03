package org.opencontrol.erp.crm.service

import jakarta.persistence.criteria.Predicate
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.ConvertLeadRequest
import org.opencontrol.erp.crm.api.dto.CreateCustomerRequest
import org.opencontrol.erp.crm.api.dto.CreateDealRequest
import org.opencontrol.erp.crm.api.dto.CreateLeadRequest
import org.opencontrol.erp.crm.api.dto.LeadConversionResponse
import org.opencontrol.erp.crm.api.dto.LeadResponse
import org.opencontrol.erp.crm.domain.events.LeadConvertedEvent
import org.opencontrol.erp.crm.domain.model.Lead
import org.opencontrol.erp.crm.domain.model.LeadSource
import org.opencontrol.erp.crm.domain.model.LeadStatus
import org.opencontrol.erp.crm.exception.InvalidLeadTransitionException
import org.opencontrol.erp.crm.exception.LeadNotFoundException
import org.opencontrol.erp.crm.repository.LeadRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class LeadService(
    private val leadRepository: LeadRepository,
    private val customerService: CustomerService,
    private val dealService: DealService,
    private val auditService: AuditService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listLeads(
        status: LeadStatus? = null,
        source: LeadSource? = null,
        assignedUserId: UUID? = null,
    ): List<LeadResponse> {
        val tenantId = getCurrentTenantId()
        val spec =
            Specification<Lead> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<UUID>("tenantId"), tenantId))

                if (status != null) {
                    predicates.add(cb.equal(root.get<LeadStatus>("status"), status))
                }
                if (source != null) {
                    predicates.add(cb.equal(root.get<LeadSource>("source"), source))
                }
                if (assignedUserId != null) {
                    predicates.add(cb.equal(root.get<UUID>("assignedUserId"), assignedUserId))
                }

                cb.and(*predicates.toTypedArray())
            }

        return leadRepository.findAll(spec).map { LeadResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): LeadResponse {
        val tenantId = getCurrentTenantId()
        val lead =
            leadRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { LeadNotFoundException(id) }
        return LeadResponse.fromEntity(lead)
    }

    fun createLead(request: CreateLeadRequest): LeadResponse {
        val tenantId = getCurrentTenantId()

        val lead =
            Lead(
                tenantId = tenantId,
                branchId = request.branchId,
                name = request.name.trim(),
                companyName = request.companyName?.trim(),
                email = request.email?.trim()?.lowercase(),
                phone = request.phone?.trim(),
                source = request.source,
                status = LeadStatus.NEW,
                estimatedBudget = request.estimatedBudget,
                interestSummary = request.interestSummary?.trim(),
                assignedUserId = request.assignedUserId,
            )
        val saved = leadRepository.save(lead)

        auditService.recordAudit(
            entityType = "Lead",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created prospect lead ${saved.name} (Source: ${saved.source})",
            stateAfter = saved,
        )

        return LeadResponse.fromEntity(saved)
    }

    fun updateLead(
        id: UUID,
        request: CreateLeadRequest,
    ): LeadResponse {
        val tenantId = getCurrentTenantId()
        val lead =
            leadRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { LeadNotFoundException(id) }

        lead.name = request.name.trim()
        lead.companyName = request.companyName?.trim()
        lead.email = request.email?.trim()?.lowercase()
        lead.phone = request.phone?.trim()
        lead.source = request.source
        lead.estimatedBudget = request.estimatedBudget
        lead.interestSummary = request.interestSummary?.trim()
        lead.assignedUserId = request.assignedUserId
        lead.updatedAt = Instant.now()

        val saved = leadRepository.save(lead)
        return LeadResponse.fromEntity(saved)
    }

    fun updateStatus(
        id: UUID,
        newStatus: LeadStatus,
    ): LeadResponse {
        val tenantId = getCurrentTenantId()
        val lead =
            leadRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { LeadNotFoundException(id) }

        if (lead.status == LeadStatus.CONVERTED) {
            throw InvalidLeadTransitionException("Cannot change status of an already converted lead.")
        }

        lead.status = newStatus
        lead.updatedAt = Instant.now()
        val saved = leadRepository.save(lead)
        return LeadResponse.fromEntity(saved)
    }

    fun disqualifyLead(
        id: UUID,
        reason: String,
    ): LeadResponse {
        val tenantId = getCurrentTenantId()
        val lead =
            leadRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { LeadNotFoundException(id) }

        if (reason.isBlank()) {
            throw IllegalArgumentException("Disqualification reason is required.")
        }

        lead.status = LeadStatus.DISQUALIFIED
        lead.disqualificationReason = reason.trim()
        lead.updatedAt = Instant.now()
        val saved = leadRepository.save(lead)

        auditService.recordAudit(
            entityType = "Lead",
            entityId = saved.id.toString(),
            action = "DISQUALIFY",
            details = "Disqualified lead ${saved.name}. Reason: ${saved.disqualificationReason}",
        )

        return LeadResponse.fromEntity(saved)
    }

    fun convertLead(
        id: UUID,
        request: ConvertLeadRequest,
    ): LeadConversionResponse {
        val tenantId = getCurrentTenantId()
        val lead =
            leadRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { LeadNotFoundException(id) }

        if (lead.status == LeadStatus.CONVERTED) {
            throw InvalidLeadTransitionException("Lead ${lead.name} has already been converted.")
        }

        // 1. Create Customer
        val customerName =
            request.customerLegalName?.takeIf { it.isNotBlank() }
                ?: lead.companyName?.takeIf { it.isNotBlank() }
                ?: lead.name

        val customer =
            customerService.createCustomer(
                CreateCustomerRequest(
                    type = request.customerType,
                    document = request.customerDocument,
                    legalName = customerName,
                    tradeName = lead.companyName,
                    phone1 = lead.phone,
                    email = lead.email,
                    contactPerson = lead.name,
                    branchId = lead.branchId,
                    creditLimit = lead.estimatedBudget,
                    notes = lead.interestSummary,
                ),
            )

        // 2. Create Deal
        val deal =
            dealService.createDeal(
                CreateDealRequest(
                    title = request.dealTitle,
                    pipelineId = request.pipelineId,
                    stageId = request.stageId,
                    customerId = customer.id,
                    leadId = lead.id,
                    estimatedValue = request.dealEstimatedValue.takeIf { it > java.math.BigDecimal.ZERO } ?: lead.estimatedBudget,
                    assignedUserId = lead.assignedUserId,
                    branchId = lead.branchId,
                ),
            )

        // 3. Mark Lead as Converted
        lead.status = LeadStatus.CONVERTED
        lead.convertedCustomerId = customer.id
        lead.convertedDealId = deal.id
        lead.updatedAt = Instant.now()
        leadRepository.save(lead)

        auditService.recordAudit(
            entityType = "Lead",
            entityId = lead.id.toString(),
            action = "CONVERT",
            details = "Converted lead ${lead.name} into Customer ${customer.legalName} and Deal ${deal.title}",
        )

        eventPublisher.publishEvent(
            LeadConvertedEvent(
                leadId = lead.id,
                tenantId = tenantId,
                customerId = customer.id,
                dealId = deal.id,
            ),
        )

        return LeadConversionResponse(
            leadId = lead.id,
            customerId = customer.id,
            dealId = deal.id,
        )
    }
}
