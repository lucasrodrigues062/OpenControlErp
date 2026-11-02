package org.opencontrol.erp.companies.domain

/**
 * Type of establishment: Headquarters (Matriz) or Branch (Filial).
 */
enum class EstablishmentType {
    HEADQUARTERS,
    BRANCH,
}

/**
 * Brazilian Tax Regime (Código de Regime Tributário - CRT).
 */
enum class TaxRegime(
    val code: Int,
    val description: String,
) {
    SIMPLES_NACIONAL(1, "Simples Nacional"),
    SIMPLES_EXCESSO_SUBLIMITE(2, "Simples Nacional - Excesso de Sublimite de Receita"),
    REGIME_NORMAL(3, "Regime Normal (Lucro Presumido / Real)"),
}

/**
 * Brazilian PIS / COFINS calculation regime.
 */
enum class PisCofinsRegime {
    CUMULATIVO,
    NAO_CUMULATIVO,
}

/**
 * Electronic Invoicing Environment for SEFAZ / Municipal tax authorities.
 */
enum class NfeEnvironment(
    val code: Int,
) {
    HOMOLOGATION(2),
    PRODUCTION(1),
}

/**
 * DANFE printable layout.
 */
enum class DanfeLayout {
    PORTRAIT,
    LANDSCAPE,
}

/**
 * Digital certificate format.
 */
enum class CertificateType {
    A1,
}
