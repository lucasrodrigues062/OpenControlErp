package org.opencontrol.erp.crm.repository

import org.opencontrol.erp.crm.domain.model.PipelineStage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface PipelineStageRepository : JpaRepository<PipelineStage, UUID> {
    fun findAllByPipelineIdOrderByOrderIndexAsc(pipelineId: UUID): List<PipelineStage>

    fun findByIdAndPipelineId(
        id: UUID,
        pipelineId: UUID,
    ): Optional<PipelineStage>
}
