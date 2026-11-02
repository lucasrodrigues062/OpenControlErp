package org.opencontrol.erp.companies.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.companies.api.dto.CertificateMetadataDto
import org.opencontrol.erp.companies.domain.BranchCertificate
import org.opencontrol.erp.companies.domain.CertificateType
import org.opencontrol.erp.companies.exception.CertificateProcessingException
import org.opencontrol.erp.companies.exception.CompanyNotFoundException
import org.opencontrol.erp.companies.repository.BranchCertificateRepository
import org.opencontrol.erp.companies.repository.CompanyBranchRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayInputStream
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class BranchCertificateService(
    private val companyBranchRepository: CompanyBranchRepository,
    private val branchCertificateRepository: BranchCertificateRepository,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID =
        SecurityContextHolder.getContext()?.tenant?.tenantId
            ?: throw IllegalStateException("Tenant context not found")

    @Transactional(readOnly = true)
    fun getCertificateMetadata(branchId: UUID): CertificateMetadataDto {
        val tenantId = getCurrentTenantId()
        companyBranchRepository
            .findByIdAndTenantId(branchId, tenantId)
            .orElseThrow { CompanyNotFoundException(branchId) }

        val cert =
            branchCertificateRepository
                .findByBranchIdAndTenantId(branchId, tenantId)
                .orElseThrow { CompanyNotFoundException("Certificate not found for branch: $branchId") }

        return toDto(cert)
    }

    fun uploadCertificate(
        branchId: UUID,
        fileBytes: ByteArray,
        password: String,
    ): CertificateMetadataDto {
        val tenantId = getCurrentTenantId()
        val branch =
            companyBranchRepository
                .findByIdAndTenantId(branchId, tenantId)
                .orElseThrow { CompanyNotFoundException(branchId) }

        val parsedCert =
            try {
                val keyStore = KeyStore.getInstance("PKCS12")
                keyStore.load(ByteArrayInputStream(fileBytes), password.toCharArray())
                val alias =
                    keyStore.aliases().asSequence().firstOrNull { keyStore.isKeyEntry(it) }
                        ?: throw CertificateProcessingException("No private key entry found in certificate keystore")
                keyStore.getCertificate(alias) as? X509Certificate
                    ?: throw CertificateProcessingException("Failed to extract X509Certificate from keystore")
            } catch (e: Exception) {
                throw CertificateProcessingException("Invalid PKCS#12 certificate file or incorrect password: ${e.message}", e)
            }

        val subject = parsedCert.subjectX500Principal.name
        val issuer = parsedCert.issuerX500Principal.name
        val serialNumber = parsedCert.serialNumber.toString(16)
        val validFrom = parsedCert.notBefore.toInstant()
        val validUntil = parsedCert.notAfter.toInstant()

        // Extract CNPJ from subject string (e.g. CN=COMPANY NAME:11222333000181) or fallback to branch's CNPJ
        val cnpjPattern = "\\b\\d{14}\\b".toRegex()
        val extractedCnpj = cnpjPattern.find(subject)?.value ?: branch.cnpj

        val existing = branchCertificateRepository.findByBranchIdAndTenantId(branchId, tenantId).orElse(null)

        val entity =
            if (existing != null) {
                existing.certificateData = fileBytes
                existing.passwordEncrypted = password
                existing.serialNumber = serialNumber
                existing.subjectName = subject
                existing.issuerName = issuer
                existing.cnpj = extractedCnpj
                existing.validFrom = validFrom
                existing.validUntil = validUntil
                existing.updatedAt = Instant.now()
                existing
            } else {
                BranchCertificate(
                    tenantId = tenantId,
                    branchId = branch.id,
                    certificateType = CertificateType.A1,
                    certificateData = fileBytes,
                    passwordEncrypted = password,
                    serialNumber = serialNumber,
                    subjectName = subject,
                    issuerName = issuer,
                    cnpj = extractedCnpj,
                    validFrom = validFrom,
                    validUntil = validUntil,
                )
            }

        val saved = branchCertificateRepository.save(entity)

        auditService.recordAudit(
            entityType = "BranchCertificate",
            entityId = saved.id.toString(),
            action = "CERTIFICATE_UPLOAD",
            details = "Certificate uploaded for branch $branchId",
            stateBefore = null,
            stateAfter =
                mapOf(
                    "branchId" to branchId.toString(),
                    "subject" to subject,
                    "validUntil" to validUntil.toString(),
                ),
        )

        return toDto(saved)
    }

    fun deleteCertificate(branchId: UUID) {
        val tenantId = getCurrentTenantId()
        val cert =
            branchCertificateRepository
                .findByBranchIdAndTenantId(branchId, tenantId)
                .orElseThrow { CompanyNotFoundException("Certificate not found for branch: $branchId") }

        branchCertificateRepository.delete(cert)

        auditService.recordAudit(
            entityType = "BranchCertificate",
            entityId = cert.id.toString(),
            action = "CERTIFICATE_DELETE",
            details = "Certificate deleted for branch $branchId",
            stateBefore = mapOf("branchId" to branchId.toString()),
            stateAfter = null,
        )
    }

    private fun toDto(cert: BranchCertificate): CertificateMetadataDto =
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
