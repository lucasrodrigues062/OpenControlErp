package org.opencontrol.erp.crm.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateCrmActivityRequest
import org.opencontrol.erp.crm.api.dto.CrmActivityResponse
import org.opencontrol.erp.crm.domain.model.ActivityEntityType
import org.opencontrol.erp.crm.domain.model.CrmActivity
import org.opencontrol.erp.crm.repository.CrmActivityRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CrmActivityService(
    private val repository: CrmActivityRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listActivities(
        entityType: ActivityEntityType,
        entityId: UUID,
    ): List<CrmActivityResponse> {
        val tenantId = getCurrentTenantId()
        return repository
            .findAllByTenantIdAndEntityTypeAndEntityIdOrderByPerformedAtDesc(
                tenantId,
                entityType,
                entityId,
            ).map { CrmActivityResponse.fromEntity(it) }
    }

    fun createActivity(request: CreateCrmActivityRequest): CrmActivityResponse {
        val tenantId = getCurrentTenantId()
        val userContext = SecurityContextHolder.getContext()
        val authorId =
            userContext?.userId?.let {
                try {
                    UUID.fromString(it)
                } catch (_: Exception) {
                    null
                }
            }

        val activity =
            CrmActivity(
                tenantId = tenantId,
                entityType = request.entityType,
                entityId = request.entityId,
                activityType = request.activityType,
                summary = request.summary.trim(),
                description = request.description?.trim(),
                performedAt = request.performedAt ?: Instant.now(),
                dueDate = request.dueDate,
                isCompleted = request.isCompleted,
                authorUserId = authorId,
            )
        val saved = repository.save(activity)

        auditService.recordAudit(
            entityType = "CrmActivity",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Logged ${saved.activityType} on ${saved.entityType} (${saved.summary})",
            stateAfter = saved,
        )

        return CrmActivityResponse.fromEntity(saved)
    }
}
