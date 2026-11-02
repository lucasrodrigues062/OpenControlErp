package org.opencontrol.erp.companies.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.companies.api.dto.AccountantDto
import org.opencontrol.erp.companies.api.dto.AddressDto
import org.opencontrol.erp.companies.api.dto.CertificateMetadataDto
import org.opencontrol.erp.companies.api.dto.CompanyBranchDetailResponse
import org.opencontrol.erp.companies.api.dto.CompanyBranchSummaryResponse
import org.opencontrol.erp.companies.api.dto.CreateCompanyBranchRequest
import org.opencontrol.erp.companies.api.dto.FiscalSettingsDto
import org.opencontrol.erp.companies.api.dto.UpdateCompanyBranchRequest
import org.opencontrol.erp.companies.domain.Address
import org.opencontrol.erp.companies.domain.BranchAccountant
import org.opencontrol.erp.companies.domain.BranchCertificate
import org.opencontrol.erp.companies.domain.BranchFiscalSettings
import org.opencontrol.erp.companies.domain.CompanyBranch
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.exception.BranchHierarchyException
import org.opencontrol.erp.companies.exception.CompanyNotFoundException
import org.opencontrol.erp.companies.exception.DuplicateCnpjException
import org.opencontrol.erp.companies.exception.DuplicateCompanyCodeException
import org.opencontrol.erp.companies.exception.InvalidCnpjException
import org.opencontrol.erp.companies.repository.BranchAccountantRepository
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.BranchFiscalSettingsRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CompanyBranchService(
    private val companyBranchRepository: CompanyBranchRepository,
    private val branchFiscalSettingsRepository: BranchFiscalSettingsRepository,
    private val branchAccountantRepository: BranchAccountantRepository,
    private val branchCertificateRepository: BranchCertificateRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID =
        SecurityContextHolder.getContext()?.tenant?.tenantId
            ?: throw IllegalStateException("Tenant context not found")

    @Transactional(readOnly = true)
    fun listCompanies(
        type: EstablishmentType? = null,
        activeOnly: Boolean = false,
    ): List<CompanyBranchSummaryResponse> {
        val tenantId = getCurrentTenantId()
        val list =
            when {
                type != null -> companyBranchRepository.findAllByTenantIdAndTypeOrderByCodeAsc(tenantId, type)
                activeOnly -> companyBranchRepository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId)
                else -> companyBranchRepository.findAllByTenantIdOrderByCodeAsc(tenantId)
            }

        return list.map { branch ->
            val cert = branchCertificateRepository.findByBranchId(branch.id).orElse(null)
            CompanyBranchSummaryResponse(
                id = branch.id,
                type = branch.type,
                parentId = branch.parentId,
                code = branch.code,
                tradeName = branch.tradeName,
                legalName = branch.legalName,
                cnpj = CnpjValidator.format(branch.cnpj),
                cityName = branch.address.cityName,
                stateCode = branch.address.stateCode,
                isActive = branch.isActive,
                logoUrl = branch.logoUrl,
                certificateValidUntil = cert?.validUntil,
                isCertificateExpired = cert?.isExpired(),
            )
        }
    }

    @Transactional(readOnly = true)
    fun getCompany(id: UUID): CompanyBranchDetailResponse {
        val tenantId = getCurrentTenantId()
        val branch =
            companyBranchRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CompanyNotFoundException(id) }
        return toDetailResponse(branch)
    }

    fun createCompany(req: CreateCompanyBranchRequest): CompanyBranchDetailResponse {
        val tenantId = getCurrentTenantId()
        val cleanCnpj = CnpjValidator.clean(req.cnpj)

        if (!CnpjValidator.isValid(cleanCnpj)) {
            throw InvalidCnpjException("Invalid CNPJ: ${req.cnpj}")
        }

        if (companyBranchRepository.existsByTenantIdAndCnpj(tenantId, cleanCnpj)) {
            throw DuplicateCnpjException(CnpjValidator.format(cleanCnpj))
        }

        if (companyBranchRepository.existsByTenantIdAndCode(tenantId, req.code.trim())) {
            throw DuplicateCompanyCodeException(req.code.trim())
        }

        if (req.type == EstablishmentType.BRANCH) {
            val parentId =
                req.parentId
                    ?: throw BranchHierarchyException("A Branch must be linked to a parent Headquarters company")
            val parent =
                companyBranchRepository
                    .findByIdAndTenantId(parentId, tenantId)
                    .orElseThrow { BranchHierarchyException("Parent Headquarters company not found: $parentId") }
            if (parent.type != EstablishmentType.HEADQUARTERS) {
                throw BranchHierarchyException("Parent establishment must be a Headquarters (Matriz)")
            }
        }

        val address =
            Address(
                postalCode = req.address.postalCode?.trim(),
                street = req.address.street?.trim(),
                number = req.address.number?.trim(),
                complement = req.address.complement?.trim(),
                neighborhood = req.address.neighborhood?.trim(),
                cityName = req.address.cityName.trim(),
                cityIbgeCode = req.address.cityIbgeCode,
                stateCode =
                    req.address.stateCode
                        .trim()
                        .uppercase(),
                countryCode =
                    req.address.countryCode
                        .trim()
                        .uppercase(),
            )

        val entity =
            CompanyBranch(
                tenantId = tenantId,
                type = req.type,
                parentId = if (req.type == EstablishmentType.BRANCH) req.parentId else null,
                code = req.code.trim(),
                legalName = req.legalName.trim(),
                tradeName = req.tradeName.trim(),
                cnpj = cleanCnpj,
                stateRegistration = req.stateRegistration?.trim(),
                isStateRegistrationExempt = req.isStateRegistrationExempt,
                municipalRegistration = req.municipalRegistration?.trim(),
                mainCnae = req.mainCnae?.trim(),
                nfseCnae = req.nfseCnae?.trim(),
                phone1 = req.phone1?.trim(),
                phone2 = req.phone2?.trim(),
                email = req.email?.trim(),
                responsiblePerson = req.responsiblePerson?.trim(),
                website = req.website?.trim(),
                logoUrl = req.logoUrl?.trim(),
                address = address,
                isActive = true,
            )

        val saved = companyBranchRepository.save(entity)

        // Initialize default fiscal settings
        val fiscalSettings =
            BranchFiscalSettings(
                tenantId = tenantId,
                branchId = saved.id,
            )
        branchFiscalSettingsRepository.save(fiscalSettings)

        auditService.recordAudit(
            entityType = "CompanyBranch",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created ${saved.type} with code ${saved.code}",
            stateBefore = null,
            stateAfter =
                mapOf(
                    "type" to saved.type.name,
                    "code" to saved.code,
                    "cnpj" to saved.cnpj,
                    "tradeName" to saved.tradeName,
                ),
        )

        return toDetailResponse(saved)
    }

    fun updateCompany(
        id: UUID,
        req: UpdateCompanyBranchRequest,
    ): CompanyBranchDetailResponse {
        val tenantId = getCurrentTenantId()
        val entity =
            companyBranchRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CompanyNotFoundException(id) }

        val oldState =
            mapOf(
                "tradeName" to entity.tradeName,
                "legalName" to entity.legalName,
                "stateRegistration" to (entity.stateRegistration ?: ""),
            )

        entity.tradeName = req.tradeName.trim()
        entity.legalName = req.legalName.trim()
        entity.stateRegistration = req.stateRegistration?.trim()
        entity.isStateRegistrationExempt = req.isStateRegistrationExempt
        entity.municipalRegistration = req.municipalRegistration?.trim()
        entity.mainCnae = req.mainCnae?.trim()
        entity.nfseCnae = req.nfseCnae?.trim()
        entity.phone1 = req.phone1?.trim()
        entity.phone2 = req.phone2?.trim()
        entity.email = req.email?.trim()
        entity.responsiblePerson = req.responsiblePerson?.trim()
        entity.website = req.website?.trim()
        entity.logoUrl = req.logoUrl?.trim()

        entity.address.postalCode = req.address.postalCode?.trim()
        entity.address.street = req.address.street?.trim()
        entity.address.number = req.address.number?.trim()
        entity.address.complement = req.address.complement?.trim()
        entity.address.neighborhood = req.address.neighborhood?.trim()
        entity.address.cityName = req.address.cityName.trim()
        entity.address.cityIbgeCode = req.address.cityIbgeCode
        entity.address.stateCode =
            req.address.stateCode
                .trim()
                .uppercase()
        entity.address.countryCode =
            req.address.countryCode
                .trim()
                .uppercase()
        entity.updatedAt = Instant.now()

        val updated = companyBranchRepository.save(entity)

        auditService.recordAudit(
            entityType = "CompanyBranch",
            entityId = updated.id.toString(),
            action = "UPDATE",
            details = "Updated company details for ${updated.code}",
            stateBefore = oldState,
            stateAfter =
                mapOf(
                    "tradeName" to updated.tradeName,
                    "legalName" to updated.legalName,
                    "stateRegistration" to (updated.stateRegistration ?: ""),
                ),
        )

        return toDetailResponse(updated)
    }

    fun toggleStatus(id: UUID): CompanyBranchDetailResponse {
        val tenantId = getCurrentTenantId()
        val entity =
            companyBranchRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CompanyNotFoundException(id) }

        if (entity.isActive && entity.type == EstablishmentType.HEADQUARTERS) {
            val activeBranchesCount = companyBranchRepository.countByTenantIdAndParentIdAndIsActiveTrue(tenantId, id)
            if (activeBranchesCount > 0) {
                throw BranchHierarchyException(
                    "Cannot deactivate Headquarters while it has $activeBranchesCount active branch(es). Deactivate branches first.",
                )
            }
        }

        val oldStatus = entity.isActive
        entity.isActive = !entity.isActive
        entity.updatedAt = Instant.now()
        val saved = companyBranchRepository.save(entity)

        auditService.recordAudit(
            entityType = "CompanyBranch",
            entityId = saved.id.toString(),
            action = "STATUS_CHANGE",
            details = "Changed active status to ${saved.isActive}",
            stateBefore = mapOf("isActive" to oldStatus),
            stateAfter = mapOf("isActive" to saved.isActive),
        )

        return toDetailResponse(saved)
    }

    fun updateFiscalSettings(
        id: UUID,
        req: FiscalSettingsDto,
    ): FiscalSettingsDto {
        val tenantId = getCurrentTenantId()
        val branch =
            companyBranchRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CompanyNotFoundException(id) }

        val settings =
            branchFiscalSettingsRepository
                .findByBranchIdAndTenantId(branch.id, tenantId)
                .orElseGet {
                    BranchFiscalSettings(tenantId = tenantId, branchId = branch.id)
                }

        settings.taxRegime = req.taxRegime
        settings.pisCofinsRegime = req.pisCofinsRegime
        settings.isTaxSubstitute = req.isTaxSubstitute
        settings.isIpiTaxpayer = req.isIpiTaxpayer
        settings.isCulturalPromoter = req.isCulturalPromoter
        settings.calculateFcp = req.calculateFcp
        settings.deductIcmsFromPiscofinsBasis = req.deductIcmsFromPiscofinsBasis
        settings.icmsSnCreditRate = req.icmsSnCreditRate ?: settings.icmsSnCreditRate
        settings.issRate = req.issRate ?: settings.issRate
        settings.cbsRate = req.cbsRate ?: settings.cbsRate
        settings.nfeEnvironment = req.nfeEnvironment
        settings.nfeSeries = req.nfeSeries ?: settings.nfeSeries
        settings.nfeModel = req.nfeModel ?: settings.nfeModel
        settings.nfseSeries = req.nfseSeries
        settings.mdfeSeries = req.mdfeSeries
        settings.defaultInvoiceMessage = req.defaultInvoiceMessage
        settings.danfeLayout = req.danfeLayout ?: settings.danfeLayout
        settings.nfseUser = req.nfseUser
        if (!req.nfsePassword.isNullOrBlank()) {
            settings.nfsePasswordEncrypted = req.nfsePassword // In production: encrypt with AES
        }
        settings.nfseAccessKey = req.nfseAccessKey
        settings.updatedAt = Instant.now()

        val saved = branchFiscalSettingsRepository.save(settings)

        auditService.recordAudit(
            entityType = "BranchFiscalSettings",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated fiscal settings for branch $id",
            stateBefore = null,
            stateAfter = mapOf("taxRegime" to saved.taxRegime.name, "nfeEnvironment" to saved.nfeEnvironment.name),
        )

        return toFiscalDto(saved)
    }

    fun updateAccountant(
        id: UUID,
        req: AccountantDto,
    ): AccountantDto {
        val tenantId = getCurrentTenantId()
        val branch =
            companyBranchRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CompanyNotFoundException(id) }

        val accountant =
            branchAccountantRepository
                .findByBranchIdAndTenantId(branch.id, tenantId)
                .orElseGet {
                    BranchAccountant(tenantId = tenantId, branchId = branch.id, name = req.name)
                }

        accountant.name = req.name.trim()
        accountant.cpf = req.cpf?.trim()
        accountant.crc = req.crc?.trim()
        accountant.email = req.email?.trim()
        accountant.phone = req.phone?.trim()
        accountant.postalCode = req.postalCode?.trim()
        accountant.street = req.street?.trim()
        accountant.number = req.number?.trim()
        accountant.neighborhood = req.neighborhood?.trim()
        accountant.cityName = req.cityName?.trim()
        accountant.stateCode = req.stateCode?.trim()?.uppercase()
        accountant.updatedAt = Instant.now()

        val saved = branchAccountantRepository.save(accountant)

        auditService.recordAudit(
            entityType = "BranchAccountant",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated accountant for branch $id",
            stateBefore = null,
            stateAfter = mapOf("name" to saved.name, "crc" to (saved.crc ?: "")),
        )

        return toAccountantDto(saved)
    }

    private fun toDetailResponse(branch: CompanyBranch): CompanyBranchDetailResponse {
        val fiscal = branchFiscalSettingsRepository.findByBranchId(branch.id).orElse(null)
        val accountant = branchAccountantRepository.findByBranchId(branch.id).orElse(null)
        val cert = branchCertificateRepository.findByBranchId(branch.id).orElse(null)

        return CompanyBranchDetailResponse(
            id = branch.id,
            type = branch.type,
            parentId = branch.parentId,
            code = branch.code,
            tradeName = branch.tradeName,
            legalName = branch.legalName,
            cnpj = CnpjValidator.format(branch.cnpj),
            stateRegistration = branch.stateRegistration,
            isStateRegistrationExempt = branch.isStateRegistrationExempt,
            municipalRegistration = branch.municipalRegistration,
            mainCnae = branch.mainCnae,
            nfseCnae = branch.nfseCnae,
            phone1 = branch.phone1,
            phone2 = branch.phone2,
            email = branch.email,
            responsiblePerson = branch.responsiblePerson,
            website = branch.website,
            logoUrl = branch.logoUrl,
            address =
                AddressDto(
                    postalCode = branch.address.postalCode,
                    street = branch.address.street,
                    number = branch.address.number,
                    complement = branch.address.complement,
                    neighborhood = branch.address.neighborhood,
                    cityName = branch.address.cityName,
                    cityIbgeCode = branch.address.cityIbgeCode,
                    stateCode = branch.address.stateCode,
                    countryCode = branch.address.countryCode,
                ),
            isActive = branch.isActive,
            fiscalSettings = fiscal?.let { toFiscalDto(it) },
            accountant = accountant?.let { toAccountantDto(it) },
            certificate = cert?.let { toCertificateDto(it) },
            createdAt = branch.createdAt,
            updatedAt = branch.updatedAt,
        )
    }

    private fun toFiscalDto(settings: BranchFiscalSettings): FiscalSettingsDto =
        FiscalSettingsDto(
            taxRegime = settings.taxRegime,
            pisCofinsRegime = settings.pisCofinsRegime,
            isTaxSubstitute = settings.isTaxSubstitute,
            isIpiTaxpayer = settings.isIpiTaxpayer,
            isCulturalPromoter = settings.isCulturalPromoter,
            calculateFcp = settings.calculateFcp,
            deductIcmsFromPiscofinsBasis = settings.deductIcmsFromPiscofinsBasis,
            icmsSnCreditRate = settings.icmsSnCreditRate,
            issRate = settings.issRate,
            cbsRate = settings.cbsRate,
            nfeEnvironment = settings.nfeEnvironment,
            nfeSeries = settings.nfeSeries,
            nfeModel = settings.nfeModel,
            nfseSeries = settings.nfseSeries,
            mdfeSeries = settings.mdfeSeries,
            defaultInvoiceMessage = settings.defaultInvoiceMessage,
            danfeLayout = settings.danfeLayout,
            nfseUser = settings.nfseUser,
            nfsePassword = null, // Do not expose password
            nfseAccessKey = settings.nfseAccessKey,
        )

    private fun toAccountantDto(accountant: BranchAccountant): AccountantDto =
        AccountantDto(
            name = accountant.name,
            cpf = accountant.cpf,
            crc = accountant.crc,
            email = accountant.email,
            phone = accountant.phone,
            postalCode = accountant.postalCode,
            street = accountant.street,
            number = accountant.number,
            neighborhood = accountant.neighborhood,
            cityName = accountant.cityName,
            stateCode = accountant.stateCode,
        )

    private fun toCertificateDto(cert: BranchCertificate): CertificateMetadataDto =
        CertificateMetadataDto(
            certificateType = cert.certificateType.name,
            serialNumber = cert.serialNumber,
            subjectName = cert.subjectName,
            issuerName = cert.issuerName,
            cnpj = cert.cnpj?.let { CnpjValidator.format(it) },
            validFrom = cert.validFrom,
            validUntil = cert.validUntil,
            isExpired = cert.isExpired(),
            daysUntilExpiration = cert.daysUntilExpiration(),
        )
}
