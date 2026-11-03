package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "leads")
class Lead(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id")
    var branchId: UUID? = null,
    @Column(name = "name", length = 150, nullable = false)
    var name: String,
    @Column(name = "company_name", length = 150)
    var companyName: String? = null,
    @Column(name = "email", length = 100)
    var email: String? = null,
    @Column(name = "phone", length = 30)
    var phone: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 50, nullable = false)
    var source: LeadSource = LeadSource.WEBSITE,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: LeadStatus = LeadStatus.NEW,
    @Column(name = "estimated_budget", precision = 15, scale = 2, nullable = false)
    var estimatedBudget: BigDecimal = BigDecimal.ZERO,
    @Column(name = "interest_summary")
    var interestSummary: String? = null,
    @Column(name = "assigned_user_id")
    var assignedUserId: UUID? = null,
    @Column(name = "disqualification_reason", length = 255)
    var disqualificationReason: String? = null,
    @Column(name = "converted_customer_id")
    var convertedCustomerId: UUID? = null,
    @Column(name = "converted_deal_id")
    var convertedDealId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
