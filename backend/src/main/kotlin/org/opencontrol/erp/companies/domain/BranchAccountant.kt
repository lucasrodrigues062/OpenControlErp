package org.opencontrol.erp.companies.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "branch_accountants")
class BranchAccountant(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false, unique = true)
    val branchId: UUID,
    @Column(nullable = false, length = 150)
    var name: String,
    @Column(length = 14)
    var cpf: String? = null,
    @Column(length = 30)
    var crc: String? = null,
    @Column(length = 100)
    var email: String? = null,
    @Column(length = 30)
    var phone: String? = null,
    @Column(name = "postal_code", length = 10)
    var postalCode: String? = null,
    @Column(length = 150)
    var street: String? = null,
    @Column(length = 30)
    var number: String? = null,
    @Column(length = 100)
    var neighborhood: String? = null,
    @Column(name = "city_name", length = 100)
    var cityName: String? = null,
    @Column(name = "state_code", length = 2)
    var stateCode: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
