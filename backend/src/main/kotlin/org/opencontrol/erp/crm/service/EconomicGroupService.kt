package org.opencontrol.erp.crm.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateEconomicGroupRequest
import org.opencontrol.erp.crm.api.dto.EconomicGroupResponse
import org.opencontrol.erp.crm.domain.model.EconomicGroup
import org.opencontrol.erp.crm.domain.validation.BrazilianDocumentValidator
import org.opencontrol.erp.crm.exception.DuplicateCodeException
import org.opencontrol.erp.crm.exception.EconomicGroupNotFoundException
import org.opencontrol.erp.crm.exception.InvalidDocumentException
import org.opencontrol.erp.crm.repository.EconomicGroupRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class EconomicGroupService(
    private val repository: EconomicGroupRepository,
    private val documentValidator: BrazilianDocumentValidator,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listGroups(activeOnly: Boolean = false): List<EconomicGroupResponse> {
        val tenantId = getCurrentTenantId()
        val list =
            if (activeOnly) {
                repository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId)
            } else {
                repository.findAllByTenantIdOrderByCodeAsc(tenantId)
            }
        return list.map { EconomicGroupResponse.fromEntity(it) }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): EconomicGroupResponse {
        val tenantId = getCurrentTenantId()
        val group =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { EconomicGroupNotFoundException(id) }
        return EconomicGroupResponse.fromEntity(group)
    }

    fun createGroup(request: CreateEconomicGroupRequest): EconomicGroupResponse {
        val tenantId = getCurrentTenantId()
        val code = request.code.trim().uppercase()
        if (repository.existsByTenantIdAndCode(tenantId, code)) {
            throw DuplicateCodeException(code)
        }

        val cleanHoldingCnpj =
            request.holdingCnpj?.let {
                val cleaned = documentValidator.clean(it)
                if (cleaned.isNotBlank()) {
                    if (!documentValidator.isValidCnpj(cleaned)) {
                        throw InvalidDocumentException("Invalid CNPJ for holding company: $it")
                    }
                    cleaned
                } else {
                    null
                }
            }

        val group =
            EconomicGroup(
                tenantId = tenantId,
                code = code,
                name = request.name.trim(),
                holdingCnpj = cleanHoldingCnpj,
                consolidatedCreditLimit = request.consolidatedCreditLimit,
                isActive = true,
            )
        val saved = repository.save(group)

        auditService.recordAudit(
            entityType = "EconomicGroup",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created economic group ${saved.code} - ${saved.name}",
            stateAfter = saved,
        )

        return EconomicGroupResponse.fromEntity(saved)
    }

    fun updateGroup(
        id: UUID,
        request: CreateEconomicGroupRequest,
    ): EconomicGroupResponse {
        val tenantId = getCurrentTenantId()
        val group =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { EconomicGroupNotFoundException(id) }

        val beforeState = EconomicGroupResponse.fromEntity(group)

        val cleanHoldingCnpj =
            request.holdingCnpj?.let {
                val cleaned = documentValidator.clean(it)
                if (cleaned.isNotBlank()) {
                    if (!documentValidator.isValidCnpj(cleaned)) {
                        throw InvalidDocumentException("Invalid CNPJ for holding company: $it")
                    }
                    cleaned
                } else {
                    null
                }
            }

        group.name = request.name.trim()
        group.holdingCnpj = cleanHoldingCnpj
        group.consolidatedCreditLimit = request.consolidatedCreditLimit
        group.updatedAt = Instant.now()

        val saved = repository.save(group)

        auditService.recordAudit(
            entityType = "EconomicGroup",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated economic group ${saved.code}",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        return EconomicGroupResponse.fromEntity(saved)
    }

    fun deleteGroup(id: UUID) {
        val tenantId = getCurrentTenantId()
        val group =
            repository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { EconomicGroupNotFoundException(id) }

        group.isActive = false
        group.updatedAt = Instant.now()
        repository.save(group)

        auditService.recordAudit(
            entityType = "EconomicGroup",
            entityId = id.toString(),
            action = "DEACTIVATE",
            details = "Deactivated economic group ${group.code}",
        )
    }
}
