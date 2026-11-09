package org.opencontrol.erp.inbound.domain.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class InboundInvoiceStatus {
    DRAFT,
    PENDING_CONFERENCE,
    CONFERRED,
    STOCKED,
    CANCELLED,
}

@Entity
@Table(name = "inbound_invoices")
class InboundInvoice(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "branch_id", nullable = false)
    var branchId: UUID,
    @Column(name = "supplier_id", nullable = false)
    var supplierId: UUID,
    @Column(name = "cost_formula_profile_id")
    var costFormulaProfileId: UUID? = null,
    @Column(name = "purchase_order_id")
    var purchaseOrderId: UUID? = null,
    @Column(name = "access_key", length = 44)
    var accessKey: String? = null,
    @Column(name = "invoice_number", length = 30, nullable = false)
    var invoiceNumber: String,
    @Column(name = "series", length = 10, nullable = false)
    var series: String = "1",
    @Column(name = "issuance_date", nullable = false)
    var issuanceDate: LocalDate,
    @Column(name = "entry_date", nullable = false)
    var entryDate: LocalDate = LocalDate.now(),
    @Column(name = "total_products", precision = 15, scale = 4, nullable = false)
    var totalProducts: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_freight", precision = 15, scale = 4, nullable = false)
    var totalFreight: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_insurance", precision = 15, scale = 4, nullable = false)
    var totalInsurance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_expenses", precision = 15, scale = 4, nullable = false)
    var totalExpenses: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_discount", precision = 15, scale = 4, nullable = false)
    var totalDiscount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "total_invoice_amount", precision = 15, scale = 4, nullable = false)
    var totalInvoiceAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "custom_variable_values", columnDefinition = "JSONB", nullable = false)
    var customVariableValuesJson: String = "{}",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    var status: InboundInvoiceStatus = InboundInvoiceStatus.DRAFT,
    @Column(name = "raw_xml_payload", columnDefinition = "TEXT")
    var rawXmlPayload: String? = null,
    @OneToMany(mappedBy = "inboundInvoice", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var items: MutableList<InboundInvoiceItem> = mutableListOf(),
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)

@Entity
@Table(name = "inbound_invoice_items")
class InboundInvoiceItem(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbound_invoice_id", nullable = false)
    var inboundInvoice: InboundInvoice,
    @Column(name = "item_index", nullable = false)
    var itemIndex: Int,
    @Column(name = "supplier_item_code", length = 100, nullable = false)
    var supplierItemCode: String,
    @Column(name = "supplier_item_description", length = 255, nullable = false)
    var supplierItemDescription: String,
    @Column(name = "matched_product_id")
    var matchedProductId: UUID? = null,
    @Column(name = "supplier_uom", length = 10, nullable = false)
    var supplierUom: String = "UN",
    @Column(name = "internal_uom", length = 10, nullable = false)
    var internalUom: String = "UN",
    @Column(name = "conversion_factor", precision = 15, scale = 4, nullable = false)
    var conversionFactor: BigDecimal = BigDecimal.ONE,
    @Column(name = "invoiced_quantity", precision = 15, scale = 4, nullable = false)
    var invoicedQuantity: BigDecimal,
    @Column(name = "internal_quantity", precision = 15, scale = 4, nullable = false)
    var internalQuantity: BigDecimal,
    @Column(name = "unit_price", precision = 15, scale = 4, nullable = false)
    var unitPrice: BigDecimal,
    @Column(name = "total_price", precision = 15, scale = 4, nullable = false)
    var totalPrice: BigDecimal,
    @Column(name = "allocated_freight", precision = 15, scale = 4, nullable = false)
    var allocatedFreight: BigDecimal = BigDecimal.ZERO,
    @Column(name = "allocated_insurance", precision = 15, scale = 4, nullable = false)
    var allocatedInsurance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "allocated_expenses", precision = 15, scale = 4, nullable = false)
    var allocatedExpenses: BigDecimal = BigDecimal.ZERO,
    @Column(name = "icms_amount", precision = 15, scale = 4, nullable = false)
    var icmsAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "icms_credit_amount", precision = 15, scale = 4, nullable = false)
    var icmsCreditAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "ipi_amount", precision = 15, scale = 4, nullable = false)
    var ipiAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "pis_amount", precision = 15, scale = 4, nullable = false)
    var pisAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "pis_credit_amount", precision = 15, scale = 4, nullable = false)
    var pisCreditAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "cofins_amount", precision = 15, scale = 4, nullable = false)
    var cofinsAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "cofins_credit_amount", precision = 15, scale = 4, nullable = false)
    var cofinsCreditAmount: BigDecimal = BigDecimal.ZERO,
    @Column(name = "final_landed_unit_cost", precision = 15, scale = 4, nullable = false)
    var finalLandedUnitCost: BigDecimal = BigDecimal.ZERO,
    @Column(name = "final_total_cost", precision = 15, scale = 4, nullable = false)
    var finalTotalCost: BigDecimal = BigDecimal.ZERO,
)

@Entity
@Table(name = "supplier_product_mappings")
class SupplierProductMapping(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "supplier_id", nullable = false)
    val supplierId: UUID,
    @Column(name = "supplier_item_code", length = 100, nullable = false)
    var supplierItemCode: String,
    @Column(name = "supplier_barcode", length = 50)
    var supplierBarcode: String? = null,
    @Column(name = "product_id", nullable = false)
    var productId: UUID,
    @Column(name = "supplier_uom", length = 10, nullable = false)
    var supplierUom: String = "UN",
    @Column(name = "conversion_factor", precision = 15, scale = 4, nullable = false)
    var conversionFactor: BigDecimal = BigDecimal.ONE,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
