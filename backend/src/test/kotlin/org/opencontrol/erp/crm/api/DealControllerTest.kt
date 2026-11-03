package org.opencontrol.erp.crm.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.crm.api.dto.DealResponse
import org.opencontrol.erp.crm.api.dto.PipelineMetricsResponse
import org.opencontrol.erp.crm.api.dto.UpdateDealStageRequest
import org.opencontrol.erp.crm.domain.model.DealStatus
import org.opencontrol.erp.crm.service.DealService
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class DealControllerTest {
    private val dealService: DealService = mockk()
    private lateinit var controller: DealController

    private val sampleDeal =
        DealResponse(
            id = UUID.randomUUID(),
            tenantId = UUID.randomUUID(),
            branchId = null,
            pipelineId = UUID.randomUUID(),
            stageId = UUID.randomUUID(),
            stageName = "Proposta",
            stageColorHex = "#F59E0B",
            customerId = UUID.randomUUID(),
            customerName = "Empresa Cliente Ltda",
            leadId = null,
            title = "Implantação ERP",
            estimatedValue = BigDecimal("60000.00"),
            currency = "BRL",
            expectedCloseDate = null,
            status = DealStatus.OPEN,
            lossReason = null,
            assignedUserId = null,
            closedAt = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = DealController(dealService)
    }

    @Test
    fun `should list deals`() {
        every { dealService.listDeals(any(), any(), any(), any(), any()) } returns listOf(sampleDeal)

        val response = controller.list(null, null, null, null, null)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
    }

    @Test
    fun `should get pipeline metrics`() {
        val metrics =
            PipelineMetricsResponse(
                totalOpenDeals = 5,
                totalPipelineValue = BigDecimal("250000.00"),
                weightedPipelineValue = BigDecimal("125000.00"),
                winRatePct = BigDecimal("65.00"),
            )
        every { dealService.getPipelineMetrics(any()) } returns metrics

        val response = controller.getMetrics(null)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(5, response.body?.totalOpenDeals)
        assertEquals(BigDecimal("250000.00"), response.body?.totalPipelineValue)
    }

    @Test
    fun `should update deal stage`() {
        val newStageId = UUID.randomUUID()
        val movedDeal = sampleDeal.copy(stageId = newStageId, stageName = "Negociação")
        every { dealService.updateDealStage(sampleDeal.id, newStageId, null) } returns movedDeal

        val response = controller.updateStage(sampleDeal.id, UpdateDealStageRequest(stageId = newStageId))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Negociação", response.body?.stageName)
    }
}
