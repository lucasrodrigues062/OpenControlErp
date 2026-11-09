package org.opencontrol.erp.inbound.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.opencontrol.erp.inbound.domain.model.CostFormulaProfile
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CustomVariableDto(
    val name: String,
    val label: String,
    val defaultValue: BigDecimal,
    val scope: String = "PROFILE_DEFAULT",
)

data class CreateCostFormulaProfileRequest(
    @field:NotBlank
    @field:Size(max = 50)
    val code: String,
    @field:NotBlank
    @field:Size(max = 150)
    val name: String,
    @field:NotBlank
    val formulaExpression: String,
    val freightProrationMethod: String = "VALUE_PROPORTIONAL",
    val customVariables: Map<String, CustomVariableDto> = emptyMap(),
    val isDefault: Boolean = false,
)

data class CostFormulaProfileResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val formulaExpression: String,
    val freightProrationMethod: String,
    val customVariables: Map<String, CustomVariableDto>,
    val isDefault: Boolean,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(p: CostFormulaProfile, customVars: Map<String, CustomVariableDto> = emptyMap()) =
            CostFormulaProfileResponse(
                id = p.id,
                code = p.code,
                name = p.name,
                formulaExpression = p.formulaExpression,
                freightProrationMethod = p.freightProrationMethod,
                customVariables = customVars,
                isDefault = p.isDefault,
                isActive = p.isActive,
                createdAt = p.createdAt,
                updatedAt = p.updatedAt,
            )
    }
}

data class EvaluateFormulaTestRequest(
    @field:NotBlank
    val formulaExpression: String,
    val variables: Map<String, BigDecimal> = emptyMap(),
)

data class EvaluateFormulaTestResponse(
    val result: BigDecimal,
    val variablesUsed: Map<String, BigDecimal>,
    val success: Boolean,
    val errorMessage: String? = null,
)
