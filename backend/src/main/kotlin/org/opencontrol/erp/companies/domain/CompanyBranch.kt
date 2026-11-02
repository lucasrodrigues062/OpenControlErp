package org.opencontrol.erp.companies.domain

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Embeddable
data class Address(
    @Column(name = "postal_code", length = 10)
    var postalCode: String? = null,
    @Column(length = 150)
    var street: String? = null,
    @Column(length = 30)
    var number: String? = null,
    @Column(length = 100)
    var complement: String? = null,
    @Column(length = 100)
    var neighborhood: String? = null,
    @Column(name = "city_name", nullable = false, length = 100)
    var cityName: String,
    @Column(name = "city_ibge_code")
    var cityIbgeCode: Int? = null,
    @Column(name = "state_code", nullable = false, length = 2)
    var stateCode: String,
    @Column(name = "country_code", nullable = false, length = 3)
    var countryCode: String = "BRA",
)

@Entity
@Table(name = "company_branches")
class CompanyBranch(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: EstablishmentType = EstablishmentType.HEADQUARTERS,
    @Column(name = "parent_id")
    var parentId: UUID? = null,
    @Column(nullable = false, length = 20)
    var code: String,
    @Column(name = "legal_name", nullable = false, length = 150)
    var legalName: String,
    @Column(name = "trade_name", nullable = false, length = 150)
    var tradeName: String,
    @Column(nullable = false, length = 14)
    var cnpj: String,
    @Column(name = "state_registration", length = 30)
    var stateRegistration: String? = null,
    @Column(name = "is_state_registration_exempt", nullable = false)
    var isStateRegistrationExempt: Boolean = false,
    @Column(name = "municipal_registration", length = 30)
    var municipalRegistration: String? = null,
    @Column(name = "main_cnae", length = 20)
    var mainCnae: String? = null,
    @Column(name = "nfse_cnae", length = 20)
    var nfseCnae: String? = null,
    @Column(name = "phone_1", length = 30)
    var phone1: String? = null,
    @Column(name = "phone_2", length = 30)
    var phone2: String? = null,
    @Column(length = 100)
    var email: String? = null,
    @Column(name = "responsible_person", length = 100)
    var responsiblePerson: String? = null,
    @Column(length = 200)
    var website: String? = null,
    @Column(name = "logo_url", columnDefinition = "TEXT")
    var logoUrl: String? = null,
    @Embedded
    var address: Address,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
