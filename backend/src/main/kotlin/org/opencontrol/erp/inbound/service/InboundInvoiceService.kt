package org.opencontrol.erp.inbound.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.repository.ProductRepository
import org.opencontrol.erp.catalog.service.PricingService
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.inbound.api.dto.FinalizeInvoiceStockRequest
import org.opencontrol.erp.inbound.api.dto.InboundInvoiceResponse
import org.opencontrol.erp.inbound.api.dto.MatchProductRequest
import org.opencontrol.erp.inbound.domain.CostFormulaEngine
import org.opencontrol.erp.inbound.domain.model.CostFormulaProfile
import org.opencontrol.erp.inbound.domain.model.InboundInvoice
import org.opencontrol.erp.inbound.domain.model.InboundInvoiceItem
import org.opencontrol.erp.inbound.domain.model.InboundInvoiceStatus
import org.opencontrol.erp.inbound.domain.model.SupplierProductMapping
import org.opencontrol.erp.inbound.repository.CostFormulaProfileRepository
import org.opencontrol.erp.inbound.repository.InboundInvoiceRepository
import org.opencontrol.erp.inbound.repository.SupplierProductMappingRepository
import org.opencontrol.erp.inventory.domain.model.StockBalance
import org.opencontrol.erp.inventory.domain.model.StockMovement
import org.opencontrol.erp.inventory.domain.model.StockMovementType
import org.opencontrol.erp.inventory.repository.StockBalanceRepository
import org.opencontrol.erp.inventory.repository.StockMovementRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class InboundInvoiceService(
    private val invoiceRepository: InboundInvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val supplierProductMappingRepository: SupplierProductMappingRepository,
    private val costFormulaProfileRepository: CostFormulaProfileRepository,
    private val stockBalanceRepository: StockBalanceRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val xmlParserService: NFeXmlParserService,
    private val formulaEngine: CostFormulaEngine,
    private val pricingService: PricingService,
    private val auditService: AuditService,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listInvoices(branchId: UUID? = null, status: String? = null): List<InboundInvoiceResponse> {
        val tenantId = getCurrentTenantId()
        val list = if (branchId != null) {
            invoiceRepository.findAllByTenantIdAndBranchIdOrderByCreatedAtDesc(tenantId, branchId)
        } else {
            invoiceRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
        }

        val filtered = if (!status.isNullOrBlank()) {
            val s = InboundInvoiceStatus.valueOf(status.trim().uppercase())
            list.filter { it.status == s }
        } else {
            list
        }

        return filtered.map { mapToResponse(it) }
    }

    @Transactional(readOnly = true)
    fun getInvoiceById(id: UUID): InboundInvoiceResponse {
        val tenantId = getCurrentTenantId()
        val invoice = invoiceRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow { IllegalArgumentException("Nota fiscal de entrada '$id' não encontrada.") }
        return mapToResponse(invoice)
    }

    fun uploadXml(file: MultipartFile, branchId: UUID, profileId: UUID? = null): InboundInvoiceResponse {
        val tenantId = getCurrentTenantId()
        val parsed = xmlParserService.parse(file.inputStream)

        // Lookup or create supplier
        val supplier = lookupOrCreateSupplier(tenantId, parsed.header.supplierCnpjOrCpf, parsed.header.supplierLegalName, parsed.header.supplierTradeName)

        // Resolve cost formula profile
        val profile = if (profileId != null) {
            costFormulaProfileRepository.findByIdAndTenantId(profileId, tenantId).orElse(null)
        } else {
            costFormulaProfileRepository.findByTenantIdAndIsDefaultTrue(tenantId).orElse(null)
        }

        val invoice = InboundInvoice(
            tenantId = tenantId,
            branchId = branchId,
            supplierId = supplier.id,
            costFormulaProfileId = profile?.id,
            accessKey = parsed.header.accessKey.ifBlank { null },
            invoiceNumber = parsed.header.invoiceNumber,
            series = parsed.header.series,
            issuanceDate = parsed.header.issuanceDate,
            totalProducts = parsed.header.totalProducts,
            totalFreight = parsed.header.totalFreight,
            totalInsurance = parsed.header.totalInsurance,
            totalExpenses = parsed.header.totalExpenses,
            totalDiscount = parsed.header.totalDiscount,
            totalInvoiceAmount = parsed.header.totalInvoice,
            status = InboundInvoiceStatus.DRAFT,
        )

        // Map items and auto-match with known mappings
        parsed.items.forEach { itemParsed ->
            val mapping = supplierProductMappingRepository.findByTenantIdAndSupplierIdAndSupplierItemCode(
                tenantId, supplier.id, itemParsed.supplierItemCode
            ).orElse(null)

            val matchedProductId = mapping?.productId
            val conversionFactor = mapping?.conversionFactor ?: BigDecimal.ONE
            val internalQuantity = itemParsed.quantity.multiply(conversionFactor).setScale(4, RoundingMode.HALF_UP)

            val invoiceItem = InboundInvoiceItem(
                tenantId = tenantId,
                inboundInvoice = invoice,
                itemIndex = itemParsed.itemIndex,
                supplierItemCode = itemParsed.supplierItemCode,
                supplierItemDescription = itemParsed.description,
                matchedProductId = matchedProductId,
                supplierUom = itemParsed.uom,
                internalUom = mapping?.supplierUom ?: itemParsed.uom,
                conversionFactor = conversionFactor,
                invoicedQuantity = itemParsed.quantity,
                internalQuantity = internalQuantity,
                unitPrice = itemParsed.unitPrice,
                totalPrice = itemParsed.totalPrice,
                allocatedFreight = itemParsed.freight,
                allocatedInsurance = itemParsed.insurance,
                allocatedExpenses = itemParsed.expenses,
                icmsAmount = itemParsed.icmsAmount,
                icmsCreditAmount = itemParsed.icmsAmount,
                ipiAmount = itemParsed.ipiAmount,
                pisAmount = itemParsed.pisAmount,
                pisCreditAmount = itemParsed.pisAmount,
                cofinsAmount = itemParsed.cofinsAmount,
                cofinsCreditAmount = itemParsed.cofinsAmount,
            )

            calculateItemCost(invoiceItem, profile)
            invoice.items.add(invoiceItem)
        }

        val saved = invoiceRepository.save(invoice)

        auditService.recordAudit(
            entityType = "InboundInvoice",
            entityId = saved.id.toString(),
            action = "UPLOAD_XML",
            details = "Imported NF-e ${saved.invoiceNumber} from supplier ${supplier.legalName}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    fun matchProduct(invoiceId: UUID, itemId: UUID, request: MatchProductRequest): InboundInvoiceResponse {
        val tenantId = getCurrentTenantId()
        val invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
            .orElseThrow { IllegalArgumentException("Nota fiscal '$invoiceId' não encontrada.") }

        val item = invoice.items.find { it.id == itemId }
            ?: throw IllegalArgumentException("Item '$itemId' não encontrado na nota fiscal.")

        val product = productRepository.findByIdAndTenantId(request.productId, tenantId)
            .orElseThrow { IllegalArgumentException("Produto '${request.productId}' não encontrado.") }

        item.matchedProductId = product.id
        item.internalUom = product.unitOfMeasure
        item.conversionFactor = request.conversionFactor
        item.internalQuantity = item.invoicedQuantity.multiply(request.conversionFactor).setScale(4, RoundingMode.HALF_UP)

        // Persist mapping memory for future imports
        val existingMapping = supplierProductMappingRepository.findByTenantIdAndSupplierIdAndSupplierItemCode(
            tenantId, invoice.supplierId, item.supplierItemCode
        )
        val mapping = existingMapping.orElseGet {
            SupplierProductMapping(
                tenantId = tenantId,
                supplierId = invoice.supplierId,
                supplierItemCode = item.supplierItemCode,
                productId = product.id,
            )
        }
        mapping.productId = product.id
        mapping.conversionFactor = request.conversionFactor
        mapping.supplierBarcode = request.supplierBarcode
        mapping.supplierUom = item.supplierUom
        mapping.updatedAt = Instant.now()
        supplierProductMappingRepository.save(mapping)

        val profile = invoice.costFormulaProfileId?.let {
            costFormulaProfileRepository.findByIdAndTenantId(it, tenantId).orElse(null)
        }
        calculateItemCost(item, profile)

        invoice.updatedAt = Instant.now()
        val saved = invoiceRepository.save(invoice)
        return mapToResponse(saved)
    }

    fun finalizeStock(invoiceId: UUID, request: FinalizeInvoiceStockRequest): InboundInvoiceResponse {
        val tenantId = getCurrentTenantId()
        val invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
            .orElseThrow { IllegalArgumentException("Nota fiscal '$invoiceId' não encontrada.") }

        if (invoice.status == InboundInvoiceStatus.STOCKED) {
            throw IllegalStateException("Nota fiscal já foi estocada anteriormente.")
        }

        val unmapped = invoice.items.find { it.matchedProductId == null }
        if (unmapped != null) {
            throw IllegalStateException("Todos os itens da nota fiscal devem ser vinculados a um produto antes da estocagem. Item pendente: ${unmapped.supplierItemDescription}")
        }

        invoice.items.forEach { item ->
            val product = productRepository.findByIdAndTenantId(item.matchedProductId!!, tenantId)
                .orElseThrow { IllegalArgumentException("Produto ${item.matchedProductId} não encontrado.") }

            val inboundQty = item.internalQuantity
            val landedUnitCost = item.finalLandedUnitCost

            // Recalculate CMP
            val currentBalances = stockBalanceRepository.findAllByTenantIdAndProductId(tenantId, product.id)
            val currentStock = currentBalances.fold(BigDecimal.ZERO) { acc, sb -> acc.add(sb.quantityOnHand) }

            val newCmp = pricingService.calculateWeightedAverageCost(
                currentQty = currentStock,
                currentCmp = product.averageCost,
                inboundQty = inboundQty,
                inboundCost = landedUnitCost,
            )

            product.averageCost = newCmp
            product.lastAcquisitionCost = landedUnitCost
            product.updatedAt = Instant.now()
            productRepository.save(product)

            // Update StockBalance
            val balance = stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(request.warehouseId, product.id)
                .orElseGet {
                    StockBalance(
                        tenantId = tenantId,
                        warehouseId = request.warehouseId,
                        productId = product.id,
                    )
                }
            balance.quantityOnHand = balance.quantityOnHand.add(inboundQty)
            balance.quantityAvailable = balance.quantityAvailable.add(inboundQty)
            balance.updatedAt = Instant.now()
            stockBalanceRepository.save(balance)

            // Write double-entry stock movement record
            val movement = StockMovement(
                tenantId = tenantId,
                branchId = invoice.branchId,
                movementType = StockMovementType.INBOUND_PURCHASE,
                productId = product.id,
                destinationWarehouseId = request.warehouseId,
                quantity = inboundQty,
                unitCost = landedUnitCost,
                balanceAfter = balance.quantityOnHand,
                referenceDocument = "NF-e ${invoice.invoiceNumber}",
                notes = "Entrada NF-e ${invoice.invoiceNumber} - Fornecedor ${invoice.supplierId}",
            )
            stockMovementRepository.save(movement)
        }

        invoice.status = InboundInvoiceStatus.STOCKED
        invoice.updatedAt = Instant.now()
        val saved = invoiceRepository.save(invoice)

        auditService.recordAudit(
            entityType = "InboundInvoice",
            entityId = saved.id.toString(),
            action = "FINALIZE_STOCK",
            details = "Finalized NF-e ${saved.invoiceNumber} to warehouse ${request.warehouseId}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    private fun calculateItemCost(item: InboundInvoiceItem, profile: CostFormulaProfile?) {
        val formula = profile?.formulaExpression ?: "= INVOICE_UNIT_PRICE + (FREIGHT_ALLOCATED / QUANTITY)"
        val vars = mutableMapOf(
            "INVOICE_UNIT_PRICE" to item.unitPrice,
            "QUANTITY" to if (item.internalQuantity > BigDecimal.ZERO) item.internalQuantity else BigDecimal.ONE,
            "FREIGHT_ALLOCATED" to item.allocatedFreight,
            "INSURANCE_ALLOCATED" to item.allocatedInsurance,
            "OTHER_EXPENSES" to item.allocatedExpenses,
            "IPI_COST" to item.ipiAmount,
            "ICMS_CREDIT" to item.icmsCreditAmount,
            "PIS_CREDIT" to item.pisCreditAmount,
            "COFINS_CREDIT" to item.cofinsCreditAmount,
        )

        val landedUnitCost = formulaEngine.evaluate(formula, vars)
        item.finalLandedUnitCost = landedUnitCost
        item.finalTotalCost = landedUnitCost.multiply(item.internalQuantity).setScale(4, RoundingMode.HALF_UP)
    }

    private fun lookupOrCreateSupplier(tenantId: UUID, document: String, legalName: String, tradeName: String?): Customer {
        val cleanDoc = document.trim().replace(Regex("[^0-9]"), "")
        val existing = customerRepository.findByTenantIdAndDocument(tenantId, cleanDoc)
        if (existing.isPresent) {
            return existing.get()
        }

        val newSupplier = Customer(
            tenantId = tenantId,
            type = if (cleanDoc.length > 11) CustomerType.LEGAL_ENTITY else CustomerType.INDIVIDUAL,
            document = cleanDoc,
            legalName = legalName.trim(),
            tradeName = tradeName?.trim(),
        )
        return customerRepository.save(newSupplier)
    }

    private fun mapToResponse(inv: InboundInvoice): InboundInvoiceResponse {
        val tenantId = inv.tenantId
        val supplierName = customerRepository.findByIdAndTenantId(inv.supplierId, tenantId)
            .map { it.legalName }
            .orElse(null)

        val profileName = inv.costFormulaProfileId?.let {
            costFormulaProfileRepository.findByIdAndTenantId(it, tenantId).map { p -> p.name }.orElse(null)
        }

        val productIds = inv.items.mapNotNull { it.matchedProductId }.toSet()
        val products = productRepository.findAllById(productIds)
        val itemDetails = products.associate { it.id to Pair(it.name, it.sku) }

        return InboundInvoiceResponse.fromEntity(inv, supplierName, profileName, itemDetails)
    }
}
