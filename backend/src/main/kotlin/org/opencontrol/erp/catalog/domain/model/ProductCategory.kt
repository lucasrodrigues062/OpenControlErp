package org.opencontrol.erp.catalog.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "product_categories")
class ProductCategory(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    @Column(name = "code", length = 50, nullable = false)
    var code: String,
    @Column(name = "name", length = 150, nullable = false)
    var name: String,
    @Column(name = "color_hex", length = 7, nullable = false)
    var colorHex: String = "#10B981",
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
