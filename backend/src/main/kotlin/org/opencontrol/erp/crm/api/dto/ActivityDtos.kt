package org.opencontrol.erp.crm.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.opencontrol.erp.crm.domain.model.ActivityEntityType
import org.opencontrol.erp.crm.domain.model.ActivityType
import org.opencontrol.erp.crm.domain.model.CrmActivity
import java.time.Instant
import java.util.UUID

data class CrmActivityResponse(
    val id: UUID,
    val tenantId: UUID,
    val entityType: ActivityEntityType,
    val entityId: UUID,
    val activityType: ActivityType,
    val summary: String,
    val description: String?,
    val performedAt: Instant,
    val dueDate: Instant?,
    @get:JsonProperty("isCompleted")
    val isCompleted: Boolean,
    val authorUserId: UUID?,
    val createdAt: Instant,
) {
    companion object {
        fun fromEntity(entity: CrmActivity) =
            CrmActivityResponse(
                id = entity.id,
                tenantId = entity.tenantId,
                entityType = entity.entityType,
                entityId = entity.entityId,
                activityType = entity.activityType,
                summary = entity.summary,
                description = entity.description,
                performedAt = entity.performedAt,
                dueDate = entity.dueDate,
                isCompleted = entity.isCompleted,
                authorUserId = entity.authorUserId,
                createdAt = entity.createdAt,
            )
    }
}

data class CreateCrmActivityRequest(
    @field:NotNull(message = "Entity type is required")
    val entityType: ActivityEntityType,
    @field:NotNull(message = "Entity ID is required")
    val entityId: UUID,
    @field:NotNull(message = "Activity type is required")
    val activityType: ActivityType,
    @field:NotBlank(message = "Activity summary is required")
    val summary: String,
    val description: String? = null,
    val performedAt: Instant? = null,
    val dueDate: Instant? = null,
    val isCompleted: Boolean = true,
)
