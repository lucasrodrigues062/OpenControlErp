package org.opencontrol.erp.crm.service

import jakarta.persistence.criteria.Predicate
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateDealRequest
import org.opencontrol.erp.crm.api.dto.DealResponse
import org.opencontrol.erp.crm.api.dto.PipelineMetricsResponse
import org.opencontrol.erp.crm.domain.events.DealLostEvent
import org.opencontrol.erp.crm.domain.events.DealWonEvent
import org.opencontrol.erp.crm.domain.model.Deal
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.opencontrol.erp.crm.domain.model.StageType
import org.opencontrol.erp.crm.exception.CustomerNotFoundException
import org.opencontrol.erp.crm.exception.DealLossReasonRequiredException
import org.opencontrol.erp.crm.exception.DealNotFoundException
import org.opencontrol.erp.crm.exception.PipelineNotFoundException
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.crm.repository.DealRepository
import org.opencontrol.erp.crm.repository.PipelineStageRepository
import org.opencontrol.erp.crm.repository.SalesPipelineRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class DealService(
    private val dealRepository: DealRepository,
    private val pipelineRepository: SalesPipelineRepository,
    private val stageRepository: PipelineStageRepository,
    private val customerRepository: CustomerRepository,
    private val pipelineService: SalesPipelineService,
    private val auditService: AuditService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listDeals(
        pipelineId: UUID? = null,
        stageId: UUID? = null,
        status: DealStatus? = null,
        assignedUserId: UUID? = null,
        branchId: UUID? = null,
    ): List<DealResponse> {
        val tenantId = getCurrentTenantId()
        val targetPipelineId = pipelineId ?: pipelineService.getOrCreateDefaultPipeline(tenantId).id

        val spec =
            Specification<Deal> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<UUID>("tenantId"), tenantId))
                predicates.add(cb.equal(root.get<UUID>("pipelineId"), targetPipelineId))

                if (stageId != null) {
                    predicates.add(cb.equal(root.get<UUID>("stageId"), stageId))
                }
                if (status != null) {
                    predicates.add(cb.equal(root.get<DealStatus>("status"), status))
                }
                if (assignedUserId != null) {
                    predicates.add(cb.equal(root.get<UUID>("assignedUserId"), assignedUserId))
                }
                if (branchId != null) {
                    predicates.add(
                        cb.or(
                            cb.isNull(root.get<UUID>("branchId")),
                            cb.equal(root.get<UUID>("branchId"), branchId),
                        ),
                    )
                }

                cb.and(*predicates.toTypedArray())
            }

        return dealRepository.findAll(spec).map { DealResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): DealResponse {
        val tenantId = getCurrentTenantId()
        val deal =
            dealRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { DealNotFoundException(id) }
        return DealResponse.fromEntity(deal)
    }

    fun createDeal(request: CreateDealRequest): DealResponse {
        val tenantId = getCurrentTenantId()

        pipelineRepository
            .findByIdAndTenantId(request.pipelineId, tenantId)
            .orElseThrow { PipelineNotFoundException(request.pipelineId) }

        val stage =
            stageRepository
                .findByIdAndPipelineId(request.stageId, request.pipelineId)
                .orElseThrow { IllegalArgumentException("Stage not found with id: ${request.stageId}") }

        val customer =
            customerRepository
                .findByIdAndTenantId(request.customerId, tenantId)
                .orElseThrow { CustomerNotFoundException(request.customerId) }

        val initialStatus =
            when (stage.stageType) {
                StageType.WON -> DealStatus.WON
                StageType.LOST -> DealStatus.LOST
                StageType.IN_PROGRESS -> DealStatus.OPEN
            }

        val deal =
            Deal(
                tenantId = tenantId,
                branchId = request.branchId,
                pipelineId = request.pipelineId,
                stageId = request.stageId,
                customerId = request.customerId,
                leadId = request.leadId,
                title = request.title.trim(),
                estimatedValue = request.estimatedValue ?: BigDecimal.ZERO,
                currency = request.currency?.trim()?.uppercase() ?: "BRL",
                expectedCloseDate = request.expectedCloseDate,
                status = initialStatus,
                assignedUserId = request.assignedUserId,
            )
        val saved = dealRepository.save(deal)

        auditService.recordAudit(
            entityType = "Deal",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created deal ${saved.title} (Value: ${saved.estimatedValue} ${saved.currency}) for ${customer.legalName}",
            stateAfter = saved,
        )

        return DealResponse.fromEntity(saved)
    }

    fun updateDeal(
        id: UUID,
        request: CreateDealRequest,
    ): DealResponse {
        val tenantId = getCurrentTenantId()
        val deal =
            dealRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { DealNotFoundException(id) }

        deal.title = request.title.trim()
        deal.estimatedValue = request.estimatedValue ?: BigDecimal.ZERO
        deal.currency = request.currency?.trim()?.uppercase() ?: "BRL"
        deal.expectedCloseDate = request.expectedCloseDate
        deal.assignedUserId = request.assignedUserId
        deal.updatedAt = Instant.now()

        val saved = dealRepository.save(deal)
        return DealResponse.fromEntity(saved)
    }

    fun updateDealStage(
        id: UUID,
        newStageId: UUID,
        lossReason: String? = null,
    ): DealResponse {
        val tenantId = getCurrentTenantId()
        val deal =
            dealRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { DealNotFoundException(id) }

        val targetStage =
            stageRepository
                .findByIdAndPipelineId(newStageId, deal.pipelineId)
                .orElseThrow { IllegalArgumentException("Target stage not found with id: $newStageId") }

        val previousStatus = deal.status
        deal.stageId = newStageId
        deal.stage = targetStage

        when (targetStage.stageType) {
            StageType.LOST -> {
                if (lossReason.isNullOrBlank()) {
                    throw DealLossReasonRequiredException()
                }
                deal.status = DealStatus.LOST
                deal.lossReason = lossReason.trim()
                deal.closedAt = Instant.now()
                eventPublisher.publishEvent(
                    DealLostEvent(
                        dealId = deal.id,
                        tenantId = deal.tenantId,
                        customerId = deal.customerId,
                        lossReason = deal.lossReason!!,
                        closedAt = deal.closedAt!!,
                    ),
                )
            }
            StageType.WON -> {
                deal.status = DealStatus.WON
                deal.lossReason = null
                deal.closedAt = Instant.now()
                eventPublisher.publishEvent(
                    DealWonEvent(
                        dealId = deal.id,
                        tenantId = deal.tenantId,
                        customerId = deal.customerId,
                        amount = deal.estimatedValue,
                        closedAt = deal.closedAt!!,
                    ),
                )
            }
            StageType.IN_PROGRESS -> {
                deal.status = DealStatus.OPEN
                deal.lossReason = null
                deal.closedAt = null
            }
        }
        deal.updatedAt = Instant.now()

        val saved = dealRepository.save(deal)

        auditService.recordAudit(
            entityType = "Deal",
            entityId = saved.id.toString(),
            action = "STAGE_CHANGE",
            details = "Changed deal ${saved.title} stage from $previousStatus to ${saved.status} (Stage: ${targetStage.name})",
            stateAfter = saved,
        )

        return DealResponse.fromEntity(saved)
    }

    @Transactional(readOnly = true)
    fun getPipelineMetrics(pipelineId: UUID? = null): PipelineMetricsResponse {
        val tenantId = getCurrentTenantId()
        val targetPipelineId = pipelineId ?: pipelineService.getOrCreateDefaultPipeline(tenantId).id

        val deals = dealRepository.findAllByTenantIdAndPipelineId(tenantId, targetPipelineId)
        val stages = stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(targetPipelineId).associateBy { it.id }

        val openDeals = deals.filter { it.status == DealStatus.OPEN }
        val wonDeals = deals.filter { it.status == DealStatus.WON }
        val lostDeals = deals.filter { it.status == DealStatus.LOST }

        val totalOpenCount = openDeals.size
        val totalPipelineValue = openDeals.fold(BigDecimal.ZERO) { acc, d -> acc.add(d.estimatedValue) }

        val weightedValue =
            openDeals.fold(BigDecimal.ZERO) { acc, d ->
                val stageProb = stages[d.stageId]?.winProbabilityPct ?: 0
                val weight = BigDecimal(stageProb).divide(BigDecimal(100), 4, RoundingMode.HALF_UP)
                acc.add(d.estimatedValue.multiply(weight))
            }

        val totalDecided = wonDeals.size + lostDeals.size
        val winRate =
            if (totalDecided > 0) {
                BigDecimal(wonDeals.size)
                    .multiply(BigDecimal(100))
                    .divide(BigDecimal(totalDecided), 2, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

        return PipelineMetricsResponse(
            totalOpenDeals = totalOpenCount,
            totalPipelineValue = totalPipelineValue.setScale(2, RoundingMode.HALF_UP),
            weightedPipelineValue = weightedValue.setScale(2, RoundingMode.HALF_UP),
            winRatePct = winRate,
        )
    }
}
