package org.opencontrol.erp.companies.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import org.opencontrol.erp.companies.domain.DanfeLayout
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.domain.NfeEnvironment
import org.opencontrol.erp.companies.domain.PisCofinsRegime
import org.opencontrol.erp.companies.domain.TaxRegime
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class AddressDto(
    @field:Size(max = 10)
    val postalCode: String? = null,
    @field:Size(max = 150)
    val street: String? = null,
    @field:Size(max = 30)
    val number: String? = null,
    @field:Size(max = 100)
    val complement: String? = null,
    @field:Size(max = 100)
    val neighborhood: String? = null,
    @field:NotBlank
    @field:Size(max = 100)
    val cityName: String,
    val cityIbgeCode: Int? = null,
    @field:NotBlank
    @field:Size(min = 2, max = 2)
    val stateCode: String,
    @field:Size(min = 3, max = 3)
    val countryCode: String = "BRA",
)

data class CreateCompanyBranchRequest(
    @field:NotNull
    val type: EstablishmentType = EstablishmentType.HEADQUARTERS,
    val parentId: UUID? = null,
    @field:NotBlank
    @field:Size(max = 20)
    val code: String,
    @field:NotBlank
    @field:Size(max = 150)
    val tradeName: String,
    @field:NotBlank
    @field:Size(max = 150)
    val legalName: String,
    @field:NotBlank
    val cnpj: String,
    @field:Size(max = 30)
    val stateRegistration: String? = null,
    @get:JsonProperty("isStateRegistrationExempt")
    val isStateRegistrationExempt: Boolean = false,
    @field:Size(max = 30)
    val municipalRegistration: String? = null,
    @field:Size(max = 20)
    val mainCnae: String? = null,
    @field:Size(max = 20)
    val nfseCnae: String? = null,
    @field:Size(max = 30)
    val phone1: String? = null,
    @field:Size(max = 30)
    val phone2: String? = null,
    @field:Size(max = 100)
    val email: String? = null,
    @field:Size(max = 100)
    val responsiblePerson: String? = null,
    @field:Size(max = 200)
    val website: String? = null,
    val logoUrl: String? = null,
    val address: AddressDto,
)

data class UpdateCompanyBranchRequest(
    @field:NotBlank
    @field:Size(max = 150)
    val tradeName: String,
    @field:NotBlank
    @field:Size(max = 150)
    val legalName: String,
    @field:Size(max = 30)
    val stateRegistration: String? = null,
    @get:JsonProperty("isStateRegistrationExempt")
    val isStateRegistrationExempt: Boolean = false,
    @field:Size(max = 30)
    val municipalRegistration: String? = null,
    @field:Size(max = 20)
    val mainCnae: String? = null,
    @field:Size(max = 20)
    val nfseCnae: String? = null,
    @field:Size(max = 30)
    val phone1: String? = null,
    @field:Size(max = 30)
    val phone2: String? = null,
    @field:Size(max = 100)
    val email: String? = null,
    @field:Size(max = 100)
    val responsiblePerson: String? = null,
    @field:Size(max = 200)
    val website: String? = null,
    val logoUrl: String? = null,
    val address: AddressDto,
)

data class FiscalSettingsDto(
    val taxRegime: TaxRegime = TaxRegime.SIMPLES_NACIONAL,
    val pisCofinsRegime: PisCofinsRegime = PisCofinsRegime.CUMULATIVO,
    val isTaxSubstitute: Boolean = false,
    val isIpiTaxpayer: Boolean = false,
    val isCulturalPromoter: Boolean = false,
    val calculateFcp: Boolean = false,
    val deductIcmsFromPiscofinsBasis: Boolean = true,
    val icmsSnCreditRate: BigDecimal = BigDecimal.ZERO,
    val issRate: BigDecimal = BigDecimal.ZERO,
    val cbsRate: BigDecimal = BigDecimal.ZERO,
    val nfeEnvironment: NfeEnvironment = NfeEnvironment.HOMOLOGATION,
    val nfeSeries: String = "1",
    val nfeModel: String = "55",
    val nfseSeries: String? = null,
    val mdfeSeries: String? = null,
    val defaultInvoiceMessage: String? = null,
    val danfeLayout: DanfeLayout = DanfeLayout.PORTRAIT,
    val nfseUser: String? = null,
    val nfsePassword: String? = null,
    val nfseAccessKey: String? = null,
)

data class AccountantDto(
    val name: String,
    val cpf: String? = null,
    val crc: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val postalCode: String? = null,
    val street: String? = null,
    val number: String? = null,
    val neighborhood: String? = null,
    val cityName: String? = null,
    val stateCode: String? = null,
)

data class CertificateMetadataDto(
    val certificateType: String,
    val serialNumber: String?,
    val subjectName: String?,
    val issuerName: String?,
    val cnpj: String?,
    val validFrom: Instant?,
    val validUntil: Instant,
    val isExpired: Boolean,
    val daysUntilExpiration: Long,
)

data class CompanyBranchSummaryResponse(
    val id: UUID,
    val type: EstablishmentType,
    val parentId: UUID?,
    val code: String,
    val tradeName: String,
    val legalName: String,
    val cnpj: String,
    val cityName: String,
    val stateCode: String,
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val logoUrl: String? = null,
    val certificateValidUntil: Instant? = null,
    @get:JsonProperty("isCertificateExpired")
    val isCertificateExpired: Boolean? = null,
)

data class CompanyBranchDetailResponse(
    val id: UUID,
    val type: EstablishmentType,
    val parentId: UUID?,
    val code: String,
    val tradeName: String,
    val legalName: String,
    val cnpj: String,
    val stateRegistration: String?,
    @get:JsonProperty("isStateRegistrationExempt")
    val isStateRegistrationExempt: Boolean,
    val municipalRegistration: String?,
    val mainCnae: String?,
    val nfseCnae: String?,
    val phone1: String?,
    val phone2: String?,
    val email: String?,
    val responsiblePerson: String?,
    val website: String?,
    val logoUrl: String? = null,
    val address: AddressDto,
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    val fiscalSettings: FiscalSettingsDto?,
    val accountant: AccountantDto?,
    val certificate: CertificateMetadataDto?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class UserBranchesResponse(
    val activeBranchId: UUID?,
    val branches: List<CompanyBranchSummaryResponse>,
)
