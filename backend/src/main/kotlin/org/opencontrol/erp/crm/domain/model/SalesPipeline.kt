package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "sales_pipelines")
class SalesPipeline(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "code", length = 30, nullable = false)
    var code: String,
    @Column(name = "name", length = 100, nullable = false)
    var name: String,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @OneToMany(mappedBy = "pipeline", cascade = [CascadeType.ALL], fetch = FetchType.LAZY)
    @OrderBy("orderIndex ASC")
    var stages: MutableList<PipelineStage> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
