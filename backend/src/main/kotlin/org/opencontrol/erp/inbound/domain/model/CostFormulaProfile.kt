package org.opencontrol.erp.inbound.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "cost_formula_profiles")
class CostFormulaProfile(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "code", length = 50, nullable = false)
    var code: String,
    @Column(name = "name", length = 150, nullable = false)
    var name: String,
    @Column(name = "formula_expression", columnDefinition = "TEXT", nullable = false)
    var formulaExpression: String,
    @Column(name = "freight_proration_method", length = 30, nullable = false)
    var freightProrationMethod: String = "VALUE_PROPORTIONAL",
    @Column(name = "custom_variables", columnDefinition = "JSONB", nullable = false)
    var customVariablesJson: String = "{}",
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
