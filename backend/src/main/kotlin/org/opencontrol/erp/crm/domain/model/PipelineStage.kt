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
import java.util.UUID

@Entity
@Table(name = "pipeline_stages")
class PipelineStage(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "pipeline_id", nullable = false)
    val pipelineId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pipeline_id", insertable = false, updatable = false)
    var pipeline: SalesPipeline? = null,
    @Column(name = "name", length = 100, nullable = false)
    var name: String,
    @Column(name = "order_index", nullable = false)
    var orderIndex: Int = 0,
    @Column(name = "win_probability_pct", nullable = false)
    var winProbabilityPct: Int = 10,
    @Column(name = "color_hex", length = 7, nullable = false)
    var colorHex: String = "#6366F1",
    @Column(name = "max_stagnant_days", nullable = false)
    var maxStagnantDays: Int = 15,
    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", length = 20, nullable = false)
    var stageType: StageType = StageType.IN_PROGRESS,
)
