package org.opencontrol.erp.kernel.plugin.domain

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "extension_plugin")
class ExtensionPlugin(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "plugin_id", nullable = false, unique = true, length = 120)
    val pluginId: String,

    @Column(nullable = false, length = 150)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(nullable = false, length = 32)
    var version: String,

    @Column(nullable = false, length = 120)
    var vendor: String,

    @Column(name = "min_kernel_version", nullable = false, length = 32)
    var minKernelVersion: String,

    @Column(name = "max_kernel_version", length = 32)
    var maxKernelVersion: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: PluginStatus = PluginStatus.INSTALLED,

    @Column(name = "archive_checksum", length = 64)
    var archiveChecksum: String? = null,

    @Column(name = "file_path", length = 500)
    var filePath: String? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @OneToMany(mappedBy = "plugin", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var dependencies: MutableList<ExtensionDependency> = mutableListOf(),

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
