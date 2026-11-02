package org.opencontrol.erp.audit.repository

import org.opencontrol.erp.audit.domain.AuditLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AuditLogRepository :
    JpaRepository<AuditLog, UUID>,
    JpaSpecificationExecutor<AuditLog>
