package org.opencontrol.erp.kernel.api.dto

import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import java.time.Instant
import java.util.UUID

data class ExtensionDependencyDto(
    val dependsOnPluginId: String,
    val versionRange: String,
    val optional: Boolean,
)

data class ExtensionPluginDto(
    val id: UUID,
    val pluginId: String,
    val name: String,
    val description: String?,
    val version: String,
    val vendor: String,
    val minKernelVersion: String,
    val status: PluginStatus,
    val errorMessage: String?,
    val dependencies: List<ExtensionDependencyDto>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(entity: ExtensionPlugin): ExtensionPluginDto = ExtensionPluginDto(
            id = entity.id,
            pluginId = entity.pluginId,
            name = entity.name,
            description = entity.description,
            version = entity.version,
            vendor = entity.vendor,
            minKernelVersion = entity.minKernelVersion,
            status = entity.status,
            errorMessage = entity.errorMessage,
            dependencies = entity.dependencies.map {
                ExtensionDependencyDto(
                    dependsOnPluginId = it.dependsOnPluginId,
                    versionRange = it.versionRange,
                    optional = it.optional,
                )
            },
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}
