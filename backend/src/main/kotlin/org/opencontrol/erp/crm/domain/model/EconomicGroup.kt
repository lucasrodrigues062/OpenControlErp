package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "economic_groups")
class EconomicGroup(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "code", length = 30, nullable = false)
    var code: String,
    @Column(name = "name", length = 150, nullable = false)
    var name: String,
    @Column(name = "holding_cnpj", length = 14)
    var holdingCnpj: String? = null,
    @Column(name = "consolidated_credit_limit", precision = 15, scale = 2, nullable = false)
    var consolidatedCreditLimit: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
