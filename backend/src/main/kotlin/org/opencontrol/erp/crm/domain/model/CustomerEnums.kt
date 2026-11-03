package org.opencontrol.erp.crm.domain.model

enum class CustomerType {
    INDIVIDUAL,
    LEGAL_ENTITY,
}

enum class IcmsTaxpayerType {
    CONTRIBUTOR,
    NON_CONTRIBUTOR,
    EXEMPT,
}

enum class CreditStatus {
    APPROVED,
    SUSPENDED,
    BLOCKED,
}
