package org.opencontrol.erp.inbound.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.inbound.api.dto.CreateCostFormulaProfileRequest
import org.opencontrol.erp.inbound.api.dto.CustomVariableDto
import org.opencontrol.erp.inbound.api.dto.EvaluateFormulaTestRequest
import org.opencontrol.erp.inbound.domain.CostFormulaEngine
import org.opencontrol.erp.inbound.domain.model.CostFormulaProfile
import org.opencontrol.erp.inbound.repository.CostFormulaProfileRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class CostFormulaServiceTest {
    private val profileRepository: CostFormulaProfileRepository = mockk()
    private val formulaEngine = CostFormulaEngine()
    private val objectMapper = jacksonObjectMapper()
    private val auditService: AuditService = mockk(relaxed = true)

    private lateinit var service: CostFormulaService

    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "cost_admin",
                roles = setOf("INBOUND_WRITE"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        service = CostFormulaService(
            profileRepository = profileRepository,
            formulaEngine = formulaEngine,
            objectMapper = objectMapper,
            auditService = auditService,
        )
    }

    @Test
    fun `should create cost formula profile with custom variables`() {
        val request = CreateCostFormulaProfileRequest(
            code = "PADRAO_COMERCIO",
            name = "Custo Padrão Comércio",
            formulaExpression = "= (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA)) + (FREIGHT_ALLOCATED / QUANTITY) - ICMS_CREDIT",
            customVariables = mapOf(
                "FATOR_QUEBRA" to CustomVariableDto(
                    name = "FATOR_QUEBRA",
                    label = "Fator de Quebra / Perda",
                    defaultValue = BigDecimal("0.05")
                )
            ),
            isDefault = true,
        )

        every { profileRepository.existsByTenantIdAndCode(tenantId, "PADRAO_COMERCIO") } returns false
        every { profileRepository.findByTenantIdAndIsDefaultTrue(tenantId) } returns Optional.empty()
        every { profileRepository.save(any()) } answers { firstArg() }

        val response = service.createProfile(request)

        assertNotNull(response)
        assertEquals("PADRAO_COMERCIO", response.code)
        assertTrue(response.isDefault)
        assertTrue(response.customVariables.containsKey("FATOR_QUEBRA"))
        verify(exactly = 1) { profileRepository.save(any()) }
    }

    @Test
    fun `should reject profile with invalid formula expression`() {
        val request = CreateCostFormulaProfileRequest(
            code = "FORMULA_INVALIDA",
            name = "Fórmula com Erro",
            formulaExpression = "= INVOICE_UNIT_PRICE + + * (",
            isDefault = false,
        )

        every { profileRepository.existsByTenantIdAndCode(tenantId, "FORMULA_INVALIDA") } returns false

        assertThrows(IllegalArgumentException::class.java) {
            service.createProfile(request)
        }
    }

    @Test
    fun `should evaluate formula test successfully`() {
        val request = EvaluateFormulaTestRequest(
            formulaExpression = "= (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA)) + (FREIGHT_ALLOCATED / QUANTITY)",
            variables = mapOf(
                "INVOICE_UNIT_PRICE" to BigDecimal("100.00"),
                "FATOR_QUEBRA" to BigDecimal("0.10"),
                "FREIGHT_ALLOCATED" to BigDecimal("20.00"),
                "QUANTITY" to BigDecimal("2.00"),
            ),
        )

        val response = service.testEvaluate(request)

        assertTrue(response.success)
        assertEquals(0, BigDecimal("120.0000").compareTo(response.result))
    }

    @Test
    fun `should set profile as default and unmark previous default`() {
        val profileId = UUID.randomUUID()
        val targetProfile = CostFormulaProfile(
            id = profileId,
            tenantId = tenantId,
            code = "NOVO_PADRAO",
            name = "Novo Padrão",
            formulaExpression = "= INVOICE_UNIT_PRICE",
            isDefault = false,
        )

        val currentDefault = CostFormulaProfile(
            id = UUID.randomUUID(),
            tenantId = tenantId,
            code = "ANTIGO_PADRAO",
            name = "Antigo Padrão",
            formulaExpression = "= INVOICE_UNIT_PRICE",
            isDefault = true,
        )

        every { profileRepository.findByIdAndTenantId(profileId, tenantId) } returns Optional.of(targetProfile)
        every { profileRepository.findByTenantIdAndIsDefaultTrue(tenantId) } returns Optional.of(currentDefault)
        every { profileRepository.save(any()) } answers { firstArg() }

        val response = service.setDefault(profileId)

        assertTrue(response.isDefault)
        assertFalse(currentDefault.isDefault)
        verify(exactly = 2) { profileRepository.save(any()) }
    }
}
