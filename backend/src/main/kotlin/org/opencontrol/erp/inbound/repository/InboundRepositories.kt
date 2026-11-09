package org.opencontrol.erp.inbound.repository

import org.opencontrol.erp.inbound.domain.model.CostFormulaProfile
import org.opencontrol.erp.inbound.domain.model.GoodsReceipt
import org.opencontrol.erp.inbound.domain.model.InboundInvoice
import org.opencontrol.erp.inbound.domain.model.PurchaseOrder
import org.opencontrol.erp.inbound.domain.model.StockTransfer
import org.opencontrol.erp.inbound.domain.model.SupplierProductMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface CostFormulaProfileRepository : JpaRepository<CostFormulaProfile, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<CostFormulaProfile>
    fun findAllByTenantIdOrderByCodeAsc(tenantId: UUID): List<CostFormulaProfile>
    fun findByTenantIdAndIsDefaultTrue(tenantId: UUID): Optional<CostFormulaProfile>
    fun existsByTenantIdAndCode(tenantId: UUID, code: String): Boolean
}

@Repository
interface SupplierProductMappingRepository : JpaRepository<SupplierProductMapping, UUID> {
    fun findByTenantIdAndSupplierIdAndSupplierItemCode(
        tenantId: UUID,
        supplierId: UUID,
        supplierItemCode: String,
    ): Optional<SupplierProductMapping>

    fun findAllByTenantIdAndSupplierId(tenantId: UUID, supplierId: UUID): List<SupplierProductMapping>
}

@Repository
interface PurchaseOrderRepository : JpaRepository<PurchaseOrder, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<PurchaseOrder>
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<PurchaseOrder>
    fun findAllByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: UUID, branchId: UUID): List<PurchaseOrder>
    fun existsByTenantIdAndOrderNumber(tenantId: UUID, orderNumber: String): Boolean
}

@Repository
interface InboundInvoiceRepository : JpaRepository<InboundInvoice, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<InboundInvoice>
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<InboundInvoice>
    fun findAllByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId: UUID, branchId: UUID): List<InboundInvoice>
    fun findByTenantIdAndAccessKey(tenantId: UUID, accessKey: String): Optional<InboundInvoice>
}

@Repository
interface GoodsReceiptRepository : JpaRepository<GoodsReceipt, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<GoodsReceipt>
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<GoodsReceipt>
}

@Repository
interface StockTransferRepository : JpaRepository<StockTransfer, UUID> {
    fun findByIdAndTenantId(id: UUID, tenantId: UUID): Optional<StockTransfer>
    fun findAllByTenantIdOrderByCreatedAtDesc(tenantId: UUID): List<StockTransfer>
    fun existsByTenantIdAndTransferNumber(tenantId: UUID, transferNumber: String): Boolean
}
