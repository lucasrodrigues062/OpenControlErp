package org.opencontrol.erp.rules.repository

import org.opencontrol.erp.rules.domain.RuleExecutionTrace
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface RuleExecutionTraceRepository :
    JpaRepository<RuleExecutionTrace, UUID>,
    JpaSpecificationExecutor<RuleExecutionTrace> {
    fun findAllByTenantId(
        tenantId: UUID,
        pageable: Pageable,
    ): Page<RuleExecutionTrace>

    fun findAllByTenantIdAndRuleId(
        tenantId: UUID,
        ruleId: UUID,
        pageable: Pageable,
    ): Page<RuleExecutionTrace>
}
