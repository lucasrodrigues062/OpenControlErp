package org.opencontrol.erp.audit.api.dto

import java.time.Instant
import java.util.UUID

data class AuditLogEntryResponse(
    val id: UUID,
    val tenantId: UUID,
    val actorId: UUID?,
    val actorUsername: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val details: String?,
    val stateBefore: String?,
    val stateAfter: String?,
    val clientIp: String?,
    val userAgent: String?,
    val createdAt: Instant,
)

data class AuditLogPageResponse(
    val items: List<AuditLogEntryResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
)
