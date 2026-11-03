package org.opencontrol.erp.crm.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateDealRequest
import org.opencontrol.erp.crm.domain.events.DealLostEvent
import org.opencontrol.erp.crm.domain.events.DealWonEvent
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.domain.model.Deal
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.opencontrol.erp.crm.domain.model.PipelineStage
import org.opencontrol.erp.crm.domain.model.StageType
import org.opencontrol.erp.crm.exception.DealLossReasonRequiredException
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.crm.repository.DealRepository
import org.opencontrol.erp.crm.repository.PipelineStageRepository
import org.opencontrol.erp.crm.repository.SalesPipelineRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class DealServiceTest {
    private val dealRepository: DealRepository = mockk()
    private val pipelineRepository: SalesPipelineRepository = mockk()
    private val stageRepository: PipelineStageRepository = mockk()
    private val customerRepository: CustomerRepository = mockk()
    private val pipelineService: SalesPipelineService = mockk()
    private val auditService: AuditService = mockk(relaxed = true)
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)

    private lateinit var dealService: DealService

    private val tenantId = UUID.randomUUID()
    private val pipelineId = UUID.randomUUID()
    private val stageInProgressId = UUID.randomUUID()
    private val stageWonId = UUID.randomUUID()
    private val stageLostId = UUID.randomUUID()
    private val customerId = UUID.randomUUID()

    private val stageInProgress =
        PipelineStage(
            id = stageInProgressId,
            pipelineId = pipelineId,
            name = "Proposta",
            stageType = StageType.IN_PROGRESS,
            winProbabilityPct = 50,
        )
    private val stageWon =
        PipelineStage(id = stageWonId, pipelineId = pipelineId, name = "Ganho", stageType = StageType.WON, winProbabilityPct = 100)
    private val stageLost =
        PipelineStage(id = stageLostId, pipelineId = pipelineId, name = "Perdido", stageType = StageType.LOST, winProbabilityPct = 0)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("CRM_ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        dealService =
            DealService(
                dealRepository = dealRepository,
                pipelineRepository = pipelineRepository,
                stageRepository = stageRepository,
                customerRepository = customerRepository,
                pipelineService = pipelineService,
                auditService = auditService,
                eventPublisher = eventPublisher,
            )
    }

    @Test
    fun `should create deal with status OPEN`() {
        val request =
            CreateDealRequest(
                title = "Contrato Anual ERP",
                pipelineId = pipelineId,
                stageId = stageInProgressId,
                customerId = customerId,
                estimatedValue = BigDecimal("120000.00"),
            )

        every { pipelineRepository.findByIdAndTenantId(pipelineId, tenantId) } returns Optional.of(mockk())
        every { stageRepository.findByIdAndPipelineId(stageInProgressId, pipelineId) } returns Optional.of(stageInProgress)
        val customer = Customer(id = customerId, tenantId = tenantId, document = "53066208000170", legalName = "Sym Soluções")
        every { customerRepository.findByIdAndTenantId(customerId, tenantId) } returns Optional.of(customer)
        every { dealRepository.save(any()) } answers { firstArg() }

        val response = dealService.createDeal(request)

        assertNotNull(response)
        assertEquals("Contrato Anual ERP", response.title)
        assertEquals(DealStatus.OPEN, response.status)
        assertEquals(BigDecimal("120000.00"), response.estimatedValue)
    }

    @Test
    fun `should move deal to WON stage and trigger DealWonEvent`() {
        val dealId = UUID.randomUUID()
        val deal =
            Deal(
                id = dealId,
                tenantId = tenantId,
                pipelineId = pipelineId,
                stageId = stageInProgressId,
                customerId = customerId,
                title = "Oportunidade Fechamento",
                estimatedValue = BigDecimal("80000.00"),
                status = DealStatus.OPEN,
            )

        every { dealRepository.findByIdAndTenantId(dealId, tenantId) } returns Optional.of(deal)
        every { stageRepository.findByIdAndPipelineId(stageWonId, pipelineId) } returns Optional.of(stageWon)
        every { dealRepository.save(any()) } answers { firstArg() }

        val response = dealService.updateDealStage(dealId, stageWonId)

        assertEquals(DealStatus.WON, response.status)
        assertNotNull(deal.closedAt)
        verify { eventPublisher.publishEvent(any<DealWonEvent>()) }
    }

    @Test
    fun `should require loss reason when moving deal to LOST stage`() {
        val dealId = UUID.randomUUID()
        val deal =
            Deal(
                id = dealId,
                tenantId = tenantId,
                pipelineId = pipelineId,
                stageId = stageInProgressId,
                customerId = customerId,
                title = "Oportunidade Perdida",
                estimatedValue = BigDecimal("45000.00"),
                status = DealStatus.OPEN,
            )

        every { dealRepository.findByIdAndTenantId(dealId, tenantId) } returns Optional.of(deal)
        every { stageRepository.findByIdAndPipelineId(stageLostId, pipelineId) } returns Optional.of(stageLost)

        assertThrows<DealLossReasonRequiredException> {
            dealService.updateDealStage(dealId, stageLostId, lossReason = null)
        }
    }

    @Test
    fun `should move deal to LOST stage with recorded reason`() {
        val dealId = UUID.randomUUID()
        val deal =
            Deal(
                id = dealId,
                tenantId = tenantId,
                pipelineId = pipelineId,
                stageId = stageInProgressId,
                customerId = customerId,
                title = "Oportunidade Perdida",
                estimatedValue = BigDecimal("45000.00"),
                status = DealStatus.OPEN,
            )

        every { dealRepository.findByIdAndTenantId(dealId, tenantId) } returns Optional.of(deal)
        every { stageRepository.findByIdAndPipelineId(stageLostId, pipelineId) } returns Optional.of(stageLost)
        every { dealRepository.save(any()) } answers { firstArg() }

        val response = dealService.updateDealStage(dealId, stageLostId, lossReason = "Preço concorrente menor")

        assertEquals(DealStatus.LOST, response.status)
        assertEquals("Preço concorrente menor", response.lossReason)
        assertNotNull(deal.closedAt)
        verify { eventPublisher.publishEvent(any<DealLostEvent>()) }
    }

    @Test
    fun `should calculate pipeline metrics correctly`() {
        val deal1 =
            Deal(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                pipelineId = pipelineId,
                stageId = stageInProgressId,
                customerId = customerId,
                title = "Deal 1",
                estimatedValue = BigDecimal("100000.00"),
                status = DealStatus.OPEN,
            )
        val deal2 =
            Deal(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                pipelineId = pipelineId,
                stageId = stageWonId,
                customerId = customerId,
                title = "Deal 2",
                estimatedValue = BigDecimal("50000.00"),
                status = DealStatus.WON,
            )
        val deal3 =
            Deal(
                id = UUID.randomUUID(),
                tenantId = tenantId,
                pipelineId = pipelineId,
                stageId = stageLostId,
                customerId = customerId,
                title = "Deal 3",
                estimatedValue = BigDecimal("50000.00"),
                status = DealStatus.LOST,
            )

        every { dealRepository.findAllByTenantIdAndPipelineId(tenantId, pipelineId) } returns listOf(deal1, deal2, deal3)
        every { stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipelineId) } returns listOf(stageInProgress, stageWon, stageLost)

        val metrics = dealService.getPipelineMetrics(pipelineId)

        assertEquals(1, metrics.totalOpenDeals)
        assertEquals(BigDecimal("100000.00"), metrics.totalPipelineValue)
        assertEquals(BigDecimal("50000.00"), metrics.weightedPipelineValue) // 100k * 50%
        assertEquals(BigDecimal("50.00"), metrics.winRatePct) // 1 won out of 2 closed = 50%
    }
}
