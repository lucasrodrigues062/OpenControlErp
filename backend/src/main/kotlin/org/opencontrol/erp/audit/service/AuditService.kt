package org.opencontrol.erp.audit.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.criteria.Predicate
import org.opencontrol.erp.audit.domain.AuditLog
import org.opencontrol.erp.audit.repository.AuditLogRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.MANDATORY)
    fun recordAudit(
        entityType: String,
        entityId: String,
        action: String,
        details: String? = null,
        stateBefore: Any? = null,
        stateAfter: Any? = null,
    ): AuditLog {
        val userContext = SecurityContextHolder.getContext()
        val tenantId = userContext?.tenant?.tenantId ?: SecurityContextHolder.getCurrentTenantId()
        val actorUsername = userContext?.username ?: SecurityContextHolder.getCurrentUsername()
        val actorId =
            userContext?.userId?.let {
                try {
                    UUID.fromString(it)
                } catch (_: Exception) {
                    null
                }
            }

        val jsonBefore = stateBefore?.let { objectMapper.writeValueAsString(it) }
        val jsonAfter = stateAfter?.let { objectMapper.writeValueAsString(it) }

        val auditLog =
            AuditLog(
                tenantId = tenantId,
                actorId = actorId,
                actorUsername = actorUsername,
                action = action,
                entityType = entityType,
                entityId = entityId,
                details = details,
                stateBefore = jsonBefore,
                stateAfter = jsonAfter,
            )

        logger.debug("Recording audit log for entity '{}' id '{}' action '{}'", entityType, entityId, action)
        return auditLogRepository.save(auditLog)
    }

    @Transactional(readOnly = true)
    fun queryLogs(
        entityType: String?,
        entityId: String?,
        actorId: UUID?,
        action: String?,
        fromDate: Instant?,
        toDate: Instant?,
        pageable: Pageable,
    ): Page<AuditLog> {
        val tenantId = SecurityContextHolder.getCurrentTenantId()

        val spec =
            Specification<AuditLog> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<UUID>("tenantId"), tenantId))

                if (!entityType.isNullOrBlank()) {
                    predicates.add(cb.equal(root.get<String>("entityType"), entityType))
                }
                if (!entityId.isNullOrBlank()) {
                    predicates.add(cb.equal(root.get<String>("entityId"), entityId))
                }
                if (actorId != null) {
                    predicates.add(cb.equal(root.get<UUID>("actorId"), actorId))
                }
                if (!action.isNullOrBlank()) {
                    predicates.add(cb.equal(root.get<String>("action"), action))
                }
                if (fromDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate))
                }
                if (toDate != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate))
                }

                cb.and(*predicates.toTypedArray())
            }

        return auditLogRepository.findAll(spec, pageable)
    }
}
