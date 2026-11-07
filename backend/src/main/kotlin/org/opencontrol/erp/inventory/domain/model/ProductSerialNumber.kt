package org.opencontrol.erp.inventory.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "product_serial_numbers")
class ProductSerialNumber(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "warehouse_id", nullable = false)
    var warehouseId: UUID,
    @Column(name = "product_id", nullable = false)
    val productId: UUID,
    @Column(name = "lot_id")
    var lotId: UUID? = null,
    @Column(name = "serial_number", length = 100, nullable = false)
    var serialNumber: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: SerialNumberStatus = SerialNumberStatus.IN_STOCK,
    @Column(name = "warranty_until")
    var warrantyUntil: LocalDate? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
