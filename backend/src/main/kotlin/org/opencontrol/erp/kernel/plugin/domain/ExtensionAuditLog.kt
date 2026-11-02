package org.opencontrol.erp.kernel.plugin.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "extension_audit_log")
class ExtensionAuditLog(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "plugin_fk")
    val pluginFk: UUID?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    val action: PluginAction,

    @Column(name = "performed_by", nullable = false, length = 100)
    val performedBy: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    val fromStatus: PluginStatus?,

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    val toStatus: PluginStatus,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val details: String? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
)
