package org.opencontrol.erp.kernel.plugin.repository

import org.opencontrol.erp.kernel.plugin.domain.ExtensionAuditLog
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ExtensionPluginRepository : JpaRepository<ExtensionPlugin, UUID> {
    fun findByPluginId(pluginId: String): ExtensionPlugin?
    fun findAllByStatus(status: PluginStatus): List<ExtensionPlugin>
    fun existsByPluginId(pluginId: String): Boolean
}

@Repository
interface ExtensionAuditLogRepository : JpaRepository<ExtensionAuditLog, UUID> {
    fun findAllByPluginFkOrderByCreatedAtDesc(pluginFk: UUID): List<ExtensionAuditLog>
}
