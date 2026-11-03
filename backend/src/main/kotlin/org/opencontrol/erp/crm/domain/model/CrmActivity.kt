package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "crm_activities")
class CrmActivity(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", length = 30, nullable = false)
    var entityType: ActivityEntityType,
    @Column(name = "entity_id", nullable = false)
    var entityId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", length = 30, nullable = false)
    var activityType: ActivityType,
    @Column(name = "summary", length = 200, nullable = false)
    var summary: String,
    @Column(name = "description")
    var description: String? = null,
    @Column(name = "performed_at", nullable = false)
    var performedAt: Instant = Instant.now(),
    @Column(name = "due_date")
    var dueDate: Instant? = null,
    @Column(name = "is_completed", nullable = false)
    var isCompleted: Boolean = true,
    @Column(name = "author_user_id")
    var authorUserId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
