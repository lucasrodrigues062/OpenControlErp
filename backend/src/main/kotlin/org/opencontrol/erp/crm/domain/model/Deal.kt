package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "deals")
class Deal(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id")
    var branchId: UUID? = null,
    @Column(name = "pipeline_id", nullable = false)
    var pipelineId: UUID,
    @Column(name = "stage_id", nullable = false)
    var stageId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", insertable = false, updatable = false)
    var stage: PipelineStage? = null,
    @Column(name = "customer_id", nullable = false)
    var customerId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    var customer: Customer? = null,
    @Column(name = "lead_id")
    var leadId: UUID? = null,
    @Column(name = "title", length = 150, nullable = false)
    var title: String,
    @Column(name = "estimated_value", precision = 15, scale = 2, nullable = false)
    var estimatedValue: BigDecimal = BigDecimal.ZERO,
    @Column(name = "currency", length = 3, nullable = false)
    var currency: String = "BRL",
    @Column(name = "expected_close_date")
    var expectedCloseDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    var status: DealStatus = DealStatus.OPEN,
    @Column(name = "loss_reason", length = 255)
    var lossReason: String? = null,
    @Column(name = "assigned_user_id")
    var assignedUserId: UUID? = null,
    @Column(name = "closed_at")
    var closedAt: Instant? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
