package org.opencontrol.erp.crm.domain.model

enum class LeadStatus {
    NEW,
    IN_QUALIFICATION,
    QUALIFIED,
    DISQUALIFIED,
    CONVERTED,
}

enum class LeadSource {
    WEBSITE,
    INBOUND,
    EVENT,
    OUTBOUND,
    REFERRAL,
    SOCIAL_MEDIA,
    OTHER,
}

enum class DealStatus {
    OPEN,
    WON,
    LOST,
}

enum class StageType {
    IN_PROGRESS,
    WON,
    LOST,
}
