package org.opencontrol.erp.crm.domain.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "customers")
class Customer(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id")
    var branchId: UUID? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    var type: CustomerType = CustomerType.LEGAL_ENTITY,
    @Column(name = "document", length = 14, nullable = false)
    var document: String,
    @Column(name = "legal_name", length = 150, nullable = false)
    var legalName: String,
    @Column(name = "trade_name", length = 150)
    var tradeName: String? = null,
    @Column(name = "state_registration", length = 30)
    var stateRegistration: String? = null,
    @Column(name = "is_state_registration_exempt", nullable = false)
    var isStateRegistrationExempt: Boolean = false,
    @Column(name = "municipal_registration", length = 30)
    var municipalRegistration: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "icms_taxpayer_type", length = 30, nullable = false)
    var icmsTaxpayerType: IcmsTaxpayerType = IcmsTaxpayerType.NON_CONTRIBUTOR,
    @Column(name = "main_cnae", length = 20)
    var mainCnae: String? = null,
    @Column(name = "birth_or_founding_date")
    var birthOrFoundingDate: LocalDate? = null,
    @Column(name = "phone_1", length = 30)
    var phone1: String? = null,
    @Column(name = "phone_2", length = 30)
    var phone2: String? = null,
    @Column(name = "email", length = 100)
    var email: String? = null,
    @Column(name = "contact_person", length = 100)
    var contactPerson: String? = null,
    @Column(name = "website", length = 200)
    var website: String? = null,
    @Column(name = "category_id")
    var categoryId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    var category: CustomerCategory? = null,
    @Column(name = "economic_group_id")
    var economicGroupId: UUID? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "economic_group_id", insertable = false, updatable = false)
    var economicGroup: EconomicGroup? = null,
    @Column(name = "credit_limit", precision = 15, scale = 2, nullable = false)
    var creditLimit: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(name = "credit_status", length = 30, nullable = false)
    var creditStatus: CreditStatus = CreditStatus.APPROVED,
    @Column(name = "notes")
    var notes: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    // Primary Statutory Address
    @Column(name = "postal_code", length = 10)
    var postalCode: String? = null,
    @Column(name = "street", length = 150)
    var street: String? = null,
    @Column(name = "number", length = 30)
    var number: String? = null,
    @Column(name = "complement", length = 100)
    var complement: String? = null,
    @Column(name = "neighborhood", length = 100)
    var neighborhood: String? = null,
    @Column(name = "city_name", length = 100)
    var cityName: String? = null,
    @Column(name = "city_ibge_code")
    var cityIbgeCode: Int? = null,
    @Column(name = "state_code", length = 2)
    var stateCode: String? = null,
    @Column(name = "country_code", length = 3, nullable = false)
    var countryCode: String = "BRA",
    // Billing Address (Cobrança)
    @Column(name = "billing_postal_code", length = 10)
    var billingPostalCode: String? = null,
    @Column(name = "billing_street", length = 150)
    var billingStreet: String? = null,
    @Column(name = "billing_number", length = 30)
    var billingNumber: String? = null,
    @Column(name = "billing_complement", length = 100)
    var billingComplement: String? = null,
    @Column(name = "billing_neighborhood", length = 100)
    var billingNeighborhood: String? = null,
    @Column(name = "billing_city_name", length = 100)
    var billingCityName: String? = null,
    @Column(name = "billing_state_code", length = 2)
    var billingStateCode: String? = null,
    // Delivery Address (Entrega)
    @Column(name = "delivery_postal_code", length = 10)
    var deliveryPostalCode: String? = null,
    @Column(name = "delivery_street", length = 150)
    var deliveryStreet: String? = null,
    @Column(name = "delivery_number", length = 30)
    var deliveryNumber: String? = null,
    @Column(name = "delivery_complement", length = 100)
    var deliveryComplement: String? = null,
    @Column(name = "delivery_neighborhood", length = 100)
    var deliveryNeighborhood: String? = null,
    @Column(name = "delivery_city_name", length = 100)
    var deliveryCityName: String? = null,
    @Column(name = "delivery_state_code", length = 2)
    var deliveryStateCode: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
