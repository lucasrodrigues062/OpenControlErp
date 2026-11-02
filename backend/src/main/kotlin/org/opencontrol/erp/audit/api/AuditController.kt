package org.opencontrol.erp.audit.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencontrol.erp.audit.api.dto.AuditLogEntryResponse
import org.opencontrol.erp.audit.api.dto.AuditLogPageResponse
import org.opencontrol.erp.audit.service.AuditService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.UUID

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Immutable System and Business Audit Trail")
class AuditController(
    private val auditService: AuditService,
) {
    @GetMapping("/logs")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Search and filter immutable audit log entries")
    fun searchLogs(
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) entityId: String?,
        @RequestParam(required = false) actorId: UUID?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) fromDate: Instant?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) toDate: Instant?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<AuditLogPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())

        val logsPage =
            auditService.queryLogs(
                entityType = entityType,
                entityId = entityId,
                actorId = actorId,
                action = action,
                fromDate = fromDate,
                toDate = toDate,
                pageable = pageable,
            )

        val items =
            logsPage.content.map { log ->
                AuditLogEntryResponse(
                    id = log.id,
                    tenantId = log.tenantId,
                    actorId = log.actorId,
                    actorUsername = log.actorUsername,
                    action = log.action,
                    entityType = log.entityType,
                    entityId = log.entityId,
                    details = log.details,
                    stateBefore = log.stateBefore,
                    stateAfter = log.stateAfter,
                    clientIp = log.clientIp,
                    userAgent = log.userAgent,
                    createdAt = log.createdAt,
                )
            }

        val response =
            AuditLogPageResponse(
                items = items,
                totalElements = logsPage.totalElements,
                totalPages = logsPage.totalPages,
                currentPage = logsPage.number,
            )
        return ResponseEntity.ok(response)
    }
}
