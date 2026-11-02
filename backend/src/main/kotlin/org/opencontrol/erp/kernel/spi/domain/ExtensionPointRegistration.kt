package org.opencontrol.erp.kernel.spi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import java.util.UUID

@Entity
@Table(name = "extension_point_registration")
class ExtensionPointRegistration(
    @Id
    val id: UUID = UUID.randomUUID(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plugin_fk", nullable = false)
    val plugin: ExtensionPlugin,
    @Column(name = "extension_point_interface", nullable = false, length = 255)
    val extensionPointInterface: String,
    @Column(name = "implementation_class", nullable = false, length = 255)
    val implementationClass: String,
    @Column(nullable = false)
    val priority: Int = 100,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context_constraints", columnDefinition = "jsonb")
    val contextConstraints: String? = null,
    @Column(nullable = false)
    var active: Boolean = true,
)
