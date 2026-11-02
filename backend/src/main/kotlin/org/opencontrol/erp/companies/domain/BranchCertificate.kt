package org.opencontrol.erp.companies.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Entity
@Table(name = "branch_certificates")
class BranchCertificate(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false, unique = true)
    val branchId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type", nullable = false, length = 10)
    var certificateType: CertificateType = CertificateType.A1,
    @Column(name = "certificate_data", nullable = false)
    var certificateData: ByteArray,
    @Column(name = "password_encrypted", nullable = false, length = 255)
    var passwordEncrypted: String,
    @Column(name = "serial_number", length = 100)
    var serialNumber: String? = null,
    @Column(name = "subject_name", length = 255)
    var subjectName: String? = null,
    @Column(name = "issuer_name", length = 255)
    var issuerName: String? = null,
    @Column(length = 14)
    var cnpj: String? = null,
    @Column(name = "valid_from")
    var validFrom: Instant? = null,
    @Column(name = "valid_until", nullable = false)
    var validUntil: Instant,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
) {
    fun isExpired(): Boolean = Instant.now().isAfter(validUntil)

    fun daysUntilExpiration(): Long {
        val now = Instant.now()
        return if (now.isAfter(validUntil)) {
            -ChronoUnit.DAYS.between(validUntil, now)
        } else {
            ChronoUnit.DAYS.between(now, validUntil)
        }
    }
}
