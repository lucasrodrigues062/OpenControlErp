package org.opencontrol.erp.crm.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.opencontrol.erp.crm.domain.model.CreditStatus
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.domain.model.CustomerCategory
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.model.EconomicGroup
import org.opencontrol.erp.crm.domain.model.IcmsTaxpayerType
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// Category DTOs
data class CustomerCategoryResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val description: String?,
    val colorHex: String,
    val defaultDiscountPct: BigDecimal,
    @get:JsonProperty("isActive")
    val isActive: Boolean,
) {
    companion object {
        fun fromEntity(entity: CustomerCategory) =
            CustomerCategoryResponse(
                id = entity.id,
                code = entity.code,
                name = entity.name,
                description = entity.description,
                colorHex = entity.colorHex,
                defaultDiscountPct = entity.defaultDiscountPct,
                isActive = entity.isActive,
            )
    }
}

data class CreateCustomerCategoryRequest(
    @field:NotBlank(message = "Category code is required")
    val code: String,
    @field:NotBlank(message = "Category name is required")
    val name: String,
    val description: String? = null,
    val colorHex: String = "#3B82F6",
    val defaultDiscountPct: BigDecimal = BigDecimal.ZERO,
)

// Economic Group DTOs
data class EconomicGroupResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val holdingCnpj: String?,
    val consolidatedCreditLimit: BigDecimal,
    @get:JsonProperty("isActive")
    val isActive: Boolean,
) {
    companion object {
        fun fromEntity(entity: EconomicGroup) =
            EconomicGroupResponse(
                id = entity.id,
                code = entity.code,
                name = entity.name,
                holdingCnpj = entity.holdingCnpj,
                consolidatedCreditLimit = entity.consolidatedCreditLimit,
                isActive = entity.isActive,
            )
    }
}

data class CreateEconomicGroupRequest(
    @field:NotBlank(message = "Group code is required")
    val code: String,
    @field:NotBlank(message = "Group name is required")
    val name: String,
    val holdingCnpj: String? = null,
    val consolidatedCreditLimit: BigDecimal = BigDecimal.ZERO,
)

// Customer DTOs
data class CustomerResponse(
    val id: UUID,
    val tenantId: UUID,
    val branchId: UUID?,
    val type: CustomerType,
    val document: String,
    val formattedDocument: String,
    val legalName: String,
    val tradeName: String?,
    val stateRegistration: String?,
    @get:JsonProperty("isStateRegistrationExempt")
    val isStateRegistrationExempt: Boolean,
    val municipalRegistration: String?,
    val icmsTaxpayerType: IcmsTaxpayerType,
    val mainCnae: String?,
    val birthOrFoundingDate: LocalDate?,
    val phone1: String?,
    val phone2: String?,
    val email: String?,
    val contactPerson: String?,
    val website: String?,
    val categoryId: UUID?,
    val categoryName: String?,
    val categoryColorHex: String?,
    val economicGroupId: UUID?,
    val economicGroupName: String?,
    val creditLimit: BigDecimal,
    val creditStatus: CreditStatus,
    val notes: String?,
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    // Address
    val postalCode: String?,
    val street: String?,
    val number: String?,
    val complement: String?,
    val neighborhood: String?,
    val cityName: String?,
    val cityIbgeCode: Int?,
    val stateCode: String?,
    val countryCode: String,
    // Billing
    val billingPostalCode: String?,
    val billingStreet: String?,
    val billingNumber: String?,
    val billingComplement: String?,
    val billingNeighborhood: String?,
    val billingCityName: String?,
    val billingStateCode: String?,
    // Delivery
    val deliveryPostalCode: String?,
    val deliveryStreet: String?,
    val deliveryNumber: String?,
    val deliveryComplement: String?,
    val deliveryNeighborhood: String?,
    val deliveryCityName: String?,
    val deliveryStateCode: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun fromEntity(
            entity: Customer,
            formattedDoc: String,
        ) = CustomerResponse(
            id = entity.id,
            tenantId = entity.tenantId,
            branchId = entity.branchId,
            type = entity.type,
            document = entity.document,
            formattedDocument = formattedDoc,
            legalName = entity.legalName,
            tradeName = entity.tradeName,
            stateRegistration = entity.stateRegistration,
            isStateRegistrationExempt = entity.isStateRegistrationExempt,
            municipalRegistration = entity.municipalRegistration,
            icmsTaxpayerType = entity.icmsTaxpayerType,
            mainCnae = entity.mainCnae,
            birthOrFoundingDate = entity.birthOrFoundingDate,
            phone1 = entity.phone1,
            phone2 = entity.phone2,
            email = entity.email,
            contactPerson = entity.contactPerson,
            website = entity.website,
            categoryId = entity.categoryId,
            categoryName = entity.category?.name,
            categoryColorHex = entity.category?.colorHex,
            economicGroupId = entity.economicGroupId,
            economicGroupName = entity.economicGroup?.name,
            creditLimit = entity.creditLimit,
            creditStatus = entity.creditStatus,
            notes = entity.notes,
            isActive = entity.isActive,
            postalCode = entity.postalCode,
            street = entity.street,
            number = entity.number,
            complement = entity.complement,
            neighborhood = entity.neighborhood,
            cityName = entity.cityName,
            cityIbgeCode = entity.cityIbgeCode,
            stateCode = entity.stateCode,
            countryCode = entity.countryCode,
            billingPostalCode = entity.billingPostalCode,
            billingStreet = entity.billingStreet,
            billingNumber = entity.billingNumber,
            billingComplement = entity.billingComplement,
            billingNeighborhood = entity.billingNeighborhood,
            billingCityName = entity.billingCityName,
            billingStateCode = entity.billingStateCode,
            deliveryPostalCode = entity.deliveryPostalCode,
            deliveryStreet = entity.deliveryStreet,
            deliveryNumber = entity.deliveryNumber,
            deliveryComplement = entity.deliveryComplement,
            deliveryNeighborhood = entity.deliveryNeighborhood,
            deliveryCityName = entity.deliveryCityName,
            deliveryStateCode = entity.deliveryStateCode,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }
}

data class CreateCustomerRequest(
    @field:NotNull(message = "Customer type is required")
    val type: CustomerType,
    @field:NotBlank(message = "Document (CPF/CNPJ) is required")
    val document: String,
    @field:NotBlank(message = "Legal name is required")
    val legalName: String,
    val tradeName: String? = null,
    val stateRegistration: String? = null,
    val isStateRegistrationExempt: Boolean = false,
    val municipalRegistration: String? = null,
    val icmsTaxpayerType: IcmsTaxpayerType = IcmsTaxpayerType.NON_CONTRIBUTOR,
    val mainCnae: String? = null,
    val birthOrFoundingDate: LocalDate? = null,
    val phone1: String? = null,
    val phone2: String? = null,
    val email: String? = null,
    val contactPerson: String? = null,
    val website: String? = null,
    val categoryId: UUID? = null,
    val economicGroupId: UUID? = null,
    val branchId: UUID? = null,
    val creditLimit: BigDecimal = BigDecimal.ZERO,
    val creditStatus: CreditStatus = CreditStatus.APPROVED,
    val notes: String? = null,
    // Primary address
    val postalCode: String? = null,
    val street: String? = null,
    val number: String? = null,
    val complement: String? = null,
    val neighborhood: String? = null,
    val cityName: String? = null,
    val cityIbgeCode: Int? = null,
    val stateCode: String? = null,
    // Billing
    val billingPostalCode: String? = null,
    val billingStreet: String? = null,
    val billingNumber: String? = null,
    val billingComplement: String? = null,
    val billingNeighborhood: String? = null,
    val billingCityName: String? = null,
    val billingStateCode: String? = null,
    // Delivery
    val deliveryPostalCode: String? = null,
    val deliveryStreet: String? = null,
    val deliveryNumber: String? = null,
    val deliveryComplement: String? = null,
    val deliveryNeighborhood: String? = null,
    val deliveryCityName: String? = null,
    val deliveryStateCode: String? = null,
)

data class UpdateCustomerRequest(
    @field:NotBlank(message = "Legal name is required")
    val legalName: String,
    val tradeName: String? = null,
    val stateRegistration: String? = null,
    val isStateRegistrationExempt: Boolean = false,
    val municipalRegistration: String? = null,
    val icmsTaxpayerType: IcmsTaxpayerType = IcmsTaxpayerType.NON_CONTRIBUTOR,
    val mainCnae: String? = null,
    val birthOrFoundingDate: LocalDate? = null,
    val phone1: String? = null,
    val phone2: String? = null,
    val email: String? = null,
    val contactPerson: String? = null,
    val website: String? = null,
    val categoryId: UUID? = null,
    val economicGroupId: UUID? = null,
    val creditLimit: BigDecimal? = null,
    val creditStatus: CreditStatus? = null,
    val notes: String? = null,
    val isActive: Boolean? = null,
    // Primary address
    val postalCode: String? = null,
    val street: String? = null,
    val number: String? = null,
    val complement: String? = null,
    val neighborhood: String? = null,
    val cityName: String? = null,
    val cityIbgeCode: Int? = null,
    val stateCode: String? = null,
    // Billing
    val billingPostalCode: String? = null,
    val billingStreet: String? = null,
    val billingNumber: String? = null,
    val billingComplement: String? = null,
    val billingNeighborhood: String? = null,
    val billingCityName: String? = null,
    val billingStateCode: String? = null,
    // Delivery
    val deliveryPostalCode: String? = null,
    val deliveryStreet: String? = null,
    val deliveryNumber: String? = null,
    val deliveryComplement: String? = null,
    val deliveryNeighborhood: String? = null,
    val deliveryCityName: String? = null,
    val deliveryStateCode: String? = null,
)
