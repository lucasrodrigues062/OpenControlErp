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
import org.opencontrol.erp.crm.api.dto.CreatePipelineRequest
import org.opencontrol.erp.crm.api.dto.CreatePipelineStageRequest
import org.opencontrol.erp.crm.api.dto.ReorderStagesRequest
import org.opencontrol.erp.crm.api.dto.UpdatePipelineRequest
import org.opencontrol.erp.crm.api.dto.UpdatePipelineStageRequest
import org.opencontrol.erp.crm.domain.model.PipelineStage
import org.opencontrol.erp.crm.domain.model.SalesPipeline
import org.opencontrol.erp.crm.domain.model.StageType
import org.opencontrol.erp.crm.exception.DuplicateCodeException
import org.opencontrol.erp.crm.exception.PipelineNotFoundException
import org.opencontrol.erp.crm.exception.StageHasActiveDealsException
import org.opencontrol.erp.crm.repository.DealRepository
import org.opencontrol.erp.crm.repository.PipelineStageRepository
import org.opencontrol.erp.crm.repository.SalesPipelineRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID

class SalesPipelineServiceTest {
    private val pipelineRepository: SalesPipelineRepository = mockk()
    private val stageRepository: PipelineStageRepository = mockk(relaxed = true)
    private val dealRepository: DealRepository = mockk(relaxed = true)
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: SalesPipelineService
    private val tenantId = UUID.randomUUID()

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
        service = SalesPipelineService(pipelineRepository, stageRepository, dealRepository, auditService)
    }

    @Test
    fun `should create default pipeline when none exists`() {
        every { pipelineRepository.findByTenantIdAndIsDefaultTrue(tenantId) } returns Optional.empty()
        every { pipelineRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId) } returns emptyList()
        every { pipelineRepository.save(any()) } answers { firstArg() }

        val pipeline = service.getOrCreateDefaultPipeline(tenantId)
        assertNotNull(pipeline)
        assertEquals("DEFAULT", pipeline.code)
        verify { stageRepository.saveAll(any<List<PipelineStage>>()) }
    }

    @Test
    fun `should list pipelines and stages`() {
        val p = SalesPipeline(id = UUID.randomUUID(), tenantId = tenantId, code = "STANDARD", name = "Standard Funnel")
        val stage =
            PipelineStage(
                id = UUID.randomUUID(),
                pipelineId = p.id,
                name = "Qualification",
                orderIndex = 0,
                stageType = StageType.IN_PROGRESS,
            )

        every { pipelineRepository.findAllByTenantIdOrderByCreatedAtAsc(tenantId) } returns listOf(p)
        every { stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(p.id) } returns listOf(stage)

        val list = service.listPipelines()
        assertEquals(1, list.size)
        assertEquals("STANDARD", list[0].code)
        assertEquals(1, list[0].stages.size)
    }

    @Test
    fun `should get pipeline by id`() {
        val id = UUID.randomUUID()
        val p = SalesPipeline(id = id, tenantId = tenantId, code = "STANDARD", name = "Standard Funnel")
        every { pipelineRepository.findByIdAndTenantId(id, tenantId) } returns Optional.of(p)
        every { stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(id) } returns emptyList()

        val found = service.getById(id)
        assertEquals("STANDARD", found.code)
    }

    @Test
    fun `should throw when pipeline not found`() {
        val id = UUID.randomUUID()
        every { pipelineRepository.findByIdAndTenantId(id, tenantId) } returns Optional.empty()

        assertThrows<PipelineNotFoundException> {
            service.getById(id)
        }
    }

    @Test
    fun `should create pipeline with stages`() {
        val req =
            CreatePipelineRequest(
                code = "sales_b2b",
                name = "B2B Sales",
                isDefault = false,
            )
        every { pipelineRepository.existsByTenantIdAndCode(tenantId, "SALES_B2B") } returns false
        every { pipelineRepository.save(any()) } answers { firstArg() }

        val created = service.createPipeline(req)
        assertEquals("SALES_B2B", created.code)
        verify { stageRepository.saveAll(any<List<PipelineStage>>()) }
    }

    @Test
    fun `should reject duplicate code on pipeline create`() {
        val req = CreatePipelineRequest(code = "SALES_B2B", name = "B2B")
        every { pipelineRepository.existsByTenantIdAndCode(tenantId, "SALES_B2B") } returns true

        assertThrows<DuplicateCodeException> {
            service.createPipeline(req)
        }
    }

    @Test
    fun `should update pipeline`() {
        val id = UUID.randomUUID()
        val p = SalesPipeline(id = id, tenantId = tenantId, code = "SALES", name = "Old Name")
        every { pipelineRepository.findByIdAndTenantId(id, tenantId) } returns Optional.of(p)
        every { pipelineRepository.save(any()) } answers { firstArg() }
        every { stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(id) } returns emptyList()

        val req = UpdatePipelineRequest(name = "Updated Name", isDefault = true)
        val updated = service.updatePipeline(id, req)
        assertEquals("Updated Name", updated.name)
    }

    @Test
    fun `should add stage to pipeline`() {
        val pipelineId = UUID.randomUUID()
        val p = SalesPipeline(id = pipelineId, tenantId = tenantId, code = "SALES", name = "Sales")
        every { pipelineRepository.findByIdAndTenantId(pipelineId, tenantId) } returns Optional.of(p)
        every { stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipelineId) } returns emptyList()
        every { stageRepository.save(any()) } answers { firstArg() }

        val req =
            CreatePipelineStageRequest(
                name = "Demo Agendada",
                winProbabilityPct = 40,
                colorHex = "#3B82F6",
                maxStagnantDays = 10,
                stageType = StageType.IN_PROGRESS,
            )

        val stage = service.addStage(pipelineId, req)
        assertEquals("Demo Agendada", stage.name)
        assertEquals(40, stage.winProbabilityPct)
        assertEquals(0, stage.orderIndex)
    }

    @Test
    fun `should update existing stage in pipeline`() {
        val pipelineId = UUID.randomUUID()
        val stageId = UUID.randomUUID()
        val p = SalesPipeline(id = pipelineId, tenantId = tenantId, code = "SALES", name = "Sales")
        val stage =
            PipelineStage(
                id = stageId,
                pipelineId = pipelineId,
                name = "Old Stage",
                winProbabilityPct = 10,
            )

        every { pipelineRepository.findByIdAndTenantId(pipelineId, tenantId) } returns Optional.of(p)
        every { stageRepository.findByIdAndPipelineId(stageId, pipelineId) } returns Optional.of(stage)
        every { stageRepository.save(any()) } answers { firstArg() }

        val req =
            UpdatePipelineStageRequest(
                name = "New Stage Name",
                winProbabilityPct = 60,
                colorHex = "#10B981",
                maxStagnantDays = 7,
                stageType = StageType.IN_PROGRESS,
            )

        val updated = service.updateStage(pipelineId, stageId, req)
        assertEquals("New Stage Name", updated.name)
        assertEquals(60, updated.winProbabilityPct)
        assertEquals("#10B981", updated.colorHex)
    }

    @Test
    fun `should delete stage when no active deals`() {
        val pipelineId = UUID.randomUUID()
        val stageId = UUID.randomUUID()
        val p = SalesPipeline(id = pipelineId, tenantId = tenantId, code = "SALES", name = "Sales")
        val stage = PipelineStage(id = stageId, pipelineId = pipelineId, name = "To Delete")

        every { pipelineRepository.findByIdAndTenantId(pipelineId, tenantId) } returns Optional.of(p)
        every { stageRepository.findByIdAndPipelineId(stageId, pipelineId) } returns Optional.of(stage)
        every { dealRepository.existsByStageId(stageId) } returns false

        service.deleteStage(pipelineId, stageId)
        verify { stageRepository.delete(stage) }
    }

    @Test
    fun `should prevent deleting stage when active deals exist`() {
        val pipelineId = UUID.randomUUID()
        val stageId = UUID.randomUUID()
        val p = SalesPipeline(id = pipelineId, tenantId = tenantId, code = "SALES", name = "Sales")
        val stage = PipelineStage(id = stageId, pipelineId = pipelineId, name = "Active Stage")

        every { pipelineRepository.findByIdAndTenantId(pipelineId, tenantId) } returns Optional.of(p)
        every { stageRepository.findByIdAndPipelineId(stageId, pipelineId) } returns Optional.of(stage)
        every { dealRepository.existsByStageId(stageId) } returns true

        assertThrows<StageHasActiveDealsException> {
            service.deleteStage(pipelineId, stageId)
        }
    }

    @Test
    fun `should reorder stages in pipeline`() {
        val pipelineId = UUID.randomUUID()
        val p = SalesPipeline(id = pipelineId, tenantId = tenantId, code = "SALES", name = "Sales")
        val s1 = PipelineStage(id = UUID.randomUUID(), pipelineId = pipelineId, name = "Stage 1", orderIndex = 0)
        val s2 = PipelineStage(id = UUID.randomUUID(), pipelineId = pipelineId, name = "Stage 2", orderIndex = 1)

        every { pipelineRepository.findByIdAndTenantId(pipelineId, tenantId) } returns Optional.of(p)
        every { stageRepository.findAllByPipelineIdOrderByOrderIndexAsc(pipelineId) } returns listOf(s1, s2)
        every { stageRepository.saveAll(any<List<PipelineStage>>()) } returns listOf(s2, s1)

        val reordered = service.reorderStages(pipelineId, ReorderStagesRequest(listOf(s2.id, s1.id)))
        assertNotNull(reordered)
        verify { stageRepository.saveAll(any<List<PipelineStage>>()) }
    }
}
