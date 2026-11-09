package org.opencontrol.erp.inbound.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.inbound.api.dto.CostFormulaProfileResponse
import org.opencontrol.erp.inbound.api.dto.CreateCostFormulaProfileRequest
import org.opencontrol.erp.inbound.api.dto.CustomVariableDto
import org.opencontrol.erp.inbound.api.dto.EvaluateFormulaTestRequest
import org.opencontrol.erp.inbound.api.dto.EvaluateFormulaTestResponse
import org.opencontrol.erp.inbound.domain.CostFormulaEngine
import org.opencontrol.erp.inbound.domain.model.CostFormulaProfile
import org.opencontrol.erp.inbound.repository.CostFormulaProfileRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CostFormulaService(
    private val profileRepository: CostFormulaProfileRepository,
    private val formulaEngine: CostFormulaEngine,
    private val objectMapper: ObjectMapper,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listProfiles(): List<CostFormulaProfileResponse> {
        val tenantId = getCurrentTenantId()
        return profileRepository.findAllByTenantIdOrderByCodeAsc(tenantId).map { toResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getProfile(id: UUID): CostFormulaProfileResponse {
        val tenantId = getCurrentTenantId()
        val profile = profileRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { NoSuchElementException("Perfil de fórmula com ID $id não encontrado") }
        return toResponse(profile)
    }

    fun createProfile(request: CreateCostFormulaProfileRequest): CostFormulaProfileResponse {
        val tenantId = getCurrentTenantId()

        if (profileRepository.existsByTenantIdAndCode(tenantId, request.code.trim())) {
            throw IllegalArgumentException("Código de perfil '${request.code}' já está em uso")
        }

        // Test evaluate formula expression syntax with empty/sample variables
        try {
            val testVars = request.customVariables.mapValues { it.value.defaultValue }.toMutableMap()
            testVars["INVOICE_UNIT_PRICE"] = BigDecimal("10.00")
            testVars["FREIGHT_ALLOCATED"] = BigDecimal("1.00")
            testVars["INSURANCE_ALLOCATED"] = BigDecimal("0.50")
            testVars["OTHER_EXPENSES_ALLOCATED"] = BigDecimal("0.00")
            testVars["QUANTITY"] = BigDecimal("1.00")
            testVars["ICMS_CREDIT"] = BigDecimal("1.20")
            testVars["PIS_CREDIT"] = BigDecimal("0.165")
            testVars["COFINS_CREDIT"] = BigDecimal("0.76")
            testVars["IPI_COST"] = BigDecimal("0.50")
            testVars["ICMS_ST_COST"] = BigDecimal("0.00")
            formulaEngine.evaluate(request.formulaExpression, testVars)
        } catch (ex: Exception) {
            throw IllegalArgumentException("Expressão da fórmula inválida: ${ex.message}", ex)
        }

        if (request.isDefault) {
            profileRepository.findByTenantIdAndIsDefaultTrue(tenantId).ifPresent { existingDefault ->
                existingDefault.isDefault = false
                existingDefault.updatedAt = Instant.now()
                profileRepository.save(existingDefault)
            }
        }

        val customVarsJson = objectMapper.writeValueAsString(request.customVariables)

        val profile = CostFormulaProfile(
            tenantId = tenantId,
            code = request.code.trim(),
            name = request.name.trim(),
            formulaExpression = request.formulaExpression.trim(),
            freightProrationMethod = request.freightProrationMethod,
            customVariablesJson = customVarsJson,
            isDefault = request.isDefault,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

        val saved = profileRepository.save(profile)
        auditService.recordAudit(
            action = "CREATE",
            entityType = "CostFormulaProfile",
            entityId = saved.id.toString(),
            details = "Perfil de fórmula de custo criado: ${saved.code} - ${saved.name}"
        )

        return toResponse(saved)
    }

    fun setDefault(id: UUID): CostFormulaProfileResponse {
        val tenantId = getCurrentTenantId()
        val profile = profileRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { NoSuchElementException("Perfil de fórmula com ID $id não encontrado") }

        profileRepository.findByTenantIdAndIsDefaultTrue(tenantId).ifPresent { existingDefault ->
            if (existingDefault.id != profile.id) {
                existingDefault.isDefault = false
                existingDefault.updatedAt = Instant.now()
                profileRepository.save(existingDefault)
            }
        }

        profile.isDefault = true
        profile.updatedAt = Instant.now()
        val saved = profileRepository.save(profile)

        auditService.recordAudit(
            action = "UPDATE",
            entityType = "CostFormulaProfile",
            entityId = saved.id.toString(),
            details = "Perfil ${saved.code} definido como padrão"
        )

        return toResponse(saved)
    }

    fun testEvaluate(request: EvaluateFormulaTestRequest): EvaluateFormulaTestResponse {
        return try {
            val result = formulaEngine.evaluate(request.formulaExpression, request.variables)
            EvaluateFormulaTestResponse(
                result = result,
                variablesUsed = request.variables,
                success = true,
                errorMessage = null,
            )
        } catch (ex: Exception) {
            EvaluateFormulaTestResponse(
                result = BigDecimal.ZERO,
                variablesUsed = request.variables,
                success = false,
                errorMessage = ex.message ?: "Erro desconhecido na avaliação da fórmula",
            )
        }
    }

    private fun toResponse(profile: CostFormulaProfile): CostFormulaProfileResponse {
        val customVars: Map<String, CustomVariableDto> = try {
            if (profile.customVariablesJson.isNotBlank()) {
                objectMapper.readValue(
                    profile.customVariablesJson,
                    object : TypeReference<Map<String, CustomVariableDto>>() {}
                )
            } else {
                emptyMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }

        return CostFormulaProfileResponse.fromEntity(profile, customVars)
    }
}
