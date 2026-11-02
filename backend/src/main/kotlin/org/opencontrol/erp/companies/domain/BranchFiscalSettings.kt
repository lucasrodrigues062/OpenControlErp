package org.opencontrol.erp.companies.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "branch_fiscal_settings")
class BranchFiscalSettings(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false, unique = true)
    val branchId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_regime", nullable = false, length = 50)
    var taxRegime: TaxRegime = TaxRegime.SIMPLES_NACIONAL,
    @Enumerated(EnumType.STRING)
    @Column(name = "pis_cofins_regime", nullable = false, length = 50)
    var pisCofinsRegime: PisCofinsRegime = PisCofinsRegime.CUMULATIVO,
    @Column(name = "is_tax_substitute", nullable = false)
    var isTaxSubstitute: Boolean = false,
    @Column(name = "is_ipi_taxpayer", nullable = false)
    var isIpiTaxpayer: Boolean = false,
    @Column(name = "is_cultural_promoter", nullable = false)
    var isCulturalPromoter: Boolean = false,
    @Column(name = "calculate_fcp", nullable = false)
    var calculateFcp: Boolean = false,
    @Column(name = "deduct_icms_from_piscofins_basis", nullable = false)
    var deductIcmsFromPiscofinsBasis: Boolean = true,
    @Column(name = "icms_sn_credit_rate", precision = 7, scale = 3)
    var icmsSnCreditRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "iss_rate", precision = 7, scale = 3)
    var issRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "cbs_rate", precision = 7, scale = 3)
    var cbsRate: BigDecimal = BigDecimal.ZERO,
    @Enumerated(EnumType.STRING)
    @Column(name = "nfe_environment", nullable = false, length = 20)
    var nfeEnvironment: NfeEnvironment = NfeEnvironment.HOMOLOGATION,
    @Column(name = "nfe_series", length = 10)
    var nfeSeries: String = "1",
    @Column(name = "nfe_model", length = 5)
    var nfeModel: String = "55",
    @Column(name = "nfse_series", length = 10)
    var nfseSeries: String? = null,
    @Column(name = "mdfe_series", length = 10)
    var mdfeSeries: String? = null,
    @Column(name = "default_invoice_message", length = 1000)
    var defaultInvoiceMessage: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "danfe_layout", length = 20)
    var danfeLayout: DanfeLayout = DanfeLayout.PORTRAIT,
    @Column(name = "nfse_user", length = 100)
    var nfseUser: String? = null,
    @Column(name = "nfse_password_encrypted", length = 255)
    var nfsePasswordEncrypted: String? = null,
    @Column(name = "nfse_access_key", length = 100)
    var nfseAccessKey: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
