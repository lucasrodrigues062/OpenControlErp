package org.opencontrol.erp.inventory.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "storage_locations")
class StorageLocation(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "warehouse_id", nullable = false)
    val warehouseId: UUID,
    @Column(name = "aisle", length = 20, nullable = false)
    var aisle: String,
    @Column(name = "rack", length = 20, nullable = false)
    var rack: String,
    @Column(name = "shelf", length = 20, nullable = false)
    var shelf: String,
    @Column(name = "bin", length = 20, nullable = false)
    var bin: String,
    @Column(name = "formatted_address", length = 100, nullable = false)
    var formattedAddress: String,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
)
