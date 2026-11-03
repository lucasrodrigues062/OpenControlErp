package org.opencontrol.erp.crm.domain.events

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CustomerCreatedEvent(
    val customerId: UUID,
    val tenantId: UUID,
    val document: String,
    val legalName: String,
    val occurredAt: Instant = Instant.now(),
)

data class CustomerUpdatedEvent(
    val customerId: UUID,
    val tenantId: UUID,
    val legalName: String,
    val occurredAt: Instant = Instant.now(),
)

data class LeadConvertedEvent(
    val leadId: UUID,
    val tenantId: UUID,
    val customerId: UUID,
    val dealId: UUID,
    val occurredAt: Instant = Instant.now(),
)

data class DealWonEvent(
    val dealId: UUID,
    val tenantId: UUID,
    val customerId: UUID,
    val amount: BigDecimal,
    val closedAt: Instant = Instant.now(),
)

data class DealLostEvent(
    val dealId: UUID,
    val tenantId: UUID,
    val customerId: UUID,
    val lossReason: String,
    val closedAt: Instant = Instant.now(),
)
