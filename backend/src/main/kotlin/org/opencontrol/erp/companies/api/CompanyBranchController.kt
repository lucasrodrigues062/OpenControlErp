package org.opencontrol.erp.companies.api

import jakarta.validation.Valid
import org.opencontrol.erp.companies.api.dto.AccountantDto
import org.opencontrol.erp.companies.api.dto.CertificateMetadataDto
import org.opencontrol.erp.companies.api.dto.CompanyBranchDetailResponse
import org.opencontrol.erp.companies.api.dto.CompanyBranchSummaryResponse
import org.opencontrol.erp.companies.api.dto.CreateCompanyBranchRequest
import org.opencontrol.erp.companies.api.dto.FiscalSettingsDto
import org.opencontrol.erp.companies.api.dto.UpdateCompanyBranchRequest
import org.opencontrol.erp.companies.api.dto.UserBranchesResponse
import org.opencontrol.erp.companies.domain.EstablishmentType
import org.opencontrol.erp.companies.service.BranchCertificateService
import org.opencontrol.erp.companies.service.BranchContextService
import org.opencontrol.erp.companies.service.CompanyBranchService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/companies")
class CompanyBranchController(
    private val companyBranchService: CompanyBranchService,
    private val branchCertificateService: BranchCertificateService,
    private val branchContextService: BranchContextService,
) {
    @GetMapping
    fun listCompanies(
        @RequestParam(required = false) type: EstablishmentType?,
        @RequestParam(defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<List<CompanyBranchSummaryResponse>> = ResponseEntity.ok(companyBranchService.listCompanies(type, activeOnly))

    @PostMapping
    fun createCompany(
        @Valid @RequestBody req: CreateCompanyBranchRequest,
    ): ResponseEntity<CompanyBranchDetailResponse> {
        val created = companyBranchService.createCompany(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/user-branches")
    fun getUserBranches(): ResponseEntity<UserBranchesResponse> = ResponseEntity.ok(branchContextService.getUserBranches())

    @GetMapping("/{id}")
    fun getCompanyById(
        @PathVariable id: UUID,
    ): ResponseEntity<CompanyBranchDetailResponse> = ResponseEntity.ok(companyBranchService.getCompany(id))

    @PutMapping("/{id}")
    fun updateCompany(
        @PathVariable id: UUID,
        @Valid @RequestBody req: UpdateCompanyBranchRequest,
    ): ResponseEntity<CompanyBranchDetailResponse> = ResponseEntity.ok(companyBranchService.updateCompany(id, req))

    @PatchMapping("/{id}/status")
    fun toggleCompanyStatus(
        @PathVariable id: UUID,
    ): ResponseEntity<CompanyBranchDetailResponse> = ResponseEntity.ok(companyBranchService.toggleStatus(id))

    @PutMapping("/{id}/fiscal-settings")
    fun updateFiscalSettings(
        @PathVariable id: UUID,
        @Valid @RequestBody req: FiscalSettingsDto,
    ): ResponseEntity<FiscalSettingsDto> = ResponseEntity.ok(companyBranchService.updateFiscalSettings(id, req))

    @PutMapping("/{id}/accountant")
    fun updateAccountant(
        @PathVariable id: UUID,
        @Valid @RequestBody req: AccountantDto,
    ): ResponseEntity<AccountantDto> = ResponseEntity.ok(companyBranchService.updateAccountant(id, req))

    @GetMapping("/{id}/certificate")
    fun getCertificateMetadata(
        @PathVariable id: UUID,
    ): ResponseEntity<CertificateMetadataDto> = ResponseEntity.ok(branchCertificateService.getCertificateMetadata(id))

    @PostMapping("/{id}/certificate")
    fun uploadCertificate(
        @PathVariable id: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam("password") password: String,
    ): ResponseEntity<CertificateMetadataDto> {
        val metadata = branchCertificateService.uploadCertificate(id, file.bytes, password)
        return ResponseEntity.ok(metadata)
    }

    @DeleteMapping("/{id}/certificate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCertificate(
        @PathVariable id: UUID,
    ) {
        branchCertificateService.deleteCertificate(id)
    }
}
