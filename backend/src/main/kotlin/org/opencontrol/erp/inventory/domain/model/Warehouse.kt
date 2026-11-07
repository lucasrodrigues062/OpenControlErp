package org.opencontrol.erp.inventory.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "warehouses")
class Warehouse(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false)
    var branchId: UUID,
    @Column(name = "code", length = 50, nullable = false)
    var code: String,
    @Column(name = "name", length = 150, nullable = false)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "warehouse_type", length = 30, nullable = false)
    var warehouseType: WarehouseType = WarehouseType.COMMERCIAL,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
