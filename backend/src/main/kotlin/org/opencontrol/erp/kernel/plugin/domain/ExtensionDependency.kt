package org.opencontrol.erp.kernel.plugin.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "extension_dependency")
class ExtensionDependency(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plugin_fk", nullable = false)
    val plugin: ExtensionPlugin,
    @Column(name = "depends_on_plugin_id", nullable = false, length = 120)
    val dependsOnPluginId: String,
    @Column(name = "version_range", nullable = false, length = 64)
    val versionRange: String,
    @Column(nullable = false)
    val optional: Boolean = false,
)
