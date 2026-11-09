package org.opencontrol.erp.inbound.service

import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.catalog.domain.model.Product
import org.opencontrol.erp.catalog.domain.model.ProductCategory
import org.opencontrol.erp.catalog.repository.ProductCategoryRepository
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
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class InboundInvoiceService(
    private val invoiceRepository: InboundInvoiceRepository,
    private val customerRepository: CustomerRepository,
    private val productRepository: ProductRepository,
    private val categoryRepository: ProductCategoryRepository,
    private val supplierProductMappingRepository: SupplierProductMappingRepository,
    private val costFormulaProfileRepository: CostFormulaProfileRepository,
    private val stockBalanceRepository: StockBalanceRepository,
    private val stockMovementRepository: StockMovementRepository,
    private val xmlParserService: NFeXmlParserService,
    private val formulaEngine: CostFormulaEngine,
    private val pricingService: PricingService,
    private val auditService: AuditService,
) {
    private val random = SecureRandom()
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

    fun uploadXml(
        file: MultipartFile,
        branchId: UUID,
        profileId: UUID? = null,
        autoRegisterMissingProducts: Boolean = false,
    ): InboundInvoiceResponse {
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

        // Map items and auto-match with known mappings OR auto-identify in catalog
        parsed.items.forEach { itemParsed ->
            var mapping = supplierProductMappingRepository.findByTenantIdAndSupplierIdAndSupplierItemCode(
                tenantId, supplier.id, itemParsed.supplierItemCode
            ).orElse(null)

            var matchedProduct: Product? = null

            if (mapping != null) {
                matchedProduct = productRepository.findByIdAndTenantId(mapping.productId, tenantId).orElse(null)
            } else {
                // Auto-identification heuristics from existing catalog
                val barcode = itemParsed.supplierBarcode?.trim()?.takeIf {
                    it.isNotBlank() && !it.equals("SEM GTIN", ignoreCase = true) && !it.equals("SEM_GTIN", ignoreCase = true)
                }

                // 1. By Barcode / EAN
                if (barcode != null) {
                    matchedProduct = productRepository.findByTenantIdAndBarcode(tenantId, barcode).orElse(null)
                }

                // 2. By SKU matching supplier item code
                if (matchedProduct == null) {
                    matchedProduct = productRepository.findByTenantIdAndSku(tenantId, itemParsed.supplierItemCode.trim()).orElse(null)
                }

                // 3. By exact Product Name (ignore case)
                if (matchedProduct == null) {
                    matchedProduct = productRepository.findByTenantIdAndNameIgnoreCase(tenantId, itemParsed.description.trim()).orElse(null)
                }

                // If found in catalog, save de-para mapping for future imports
                if (matchedProduct != null) {
                    val newMapping = SupplierProductMapping(
                        tenantId = tenantId,
                        supplierId = supplier.id,
                        supplierItemCode = itemParsed.supplierItemCode.trim(),
                        supplierBarcode = barcode,
                        productId = matchedProduct.id,
                        supplierUom = itemParsed.uom,
                        conversionFactor = BigDecimal.ONE,
                    )
                    mapping = supplierProductMappingRepository.save(newMapping)
                }
            }

            // If still not identified and user requested automatic registration:
            if (matchedProduct == null && autoRegisterMissingProducts) {
                matchedProduct = createAndMapProduct(tenantId, supplier.id, itemParsed)
                mapping = supplierProductMappingRepository.findByTenantIdAndSupplierIdAndSupplierItemCode(
                    tenantId, supplier.id, itemParsed.supplierItemCode.trim()
                ).orElse(null)
            }

            val matchedProductId = matchedProduct?.id ?: mapping?.productId
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
                internalUom = matchedProduct?.unitOfMeasure ?: mapping?.supplierUom ?: itemParsed.uom,
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

    fun autoRegisterMissingProducts(invoiceId: UUID): InboundInvoiceResponse {
        val tenantId = getCurrentTenantId()
        val invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
            .orElseThrow { IllegalArgumentException("Nota fiscal com ID '$invoiceId' não encontrada.") }

        val unmappedItems = invoice.items.filter { it.matchedProductId == null }
        if (unmappedItems.isEmpty()) {
            return mapToResponse(invoice)
        }

        val profile = invoice.costFormulaProfileId?.let {
            costFormulaProfileRepository.findByIdAndTenantId(it, tenantId).orElse(null)
        }

        for (item in unmappedItems) {
            // First check if catalog already has this SKU or name
            var matchedProduct = productRepository.findByTenantIdAndSku(tenantId, item.supplierItemCode.trim()).orElse(null)
            if (matchedProduct == null) {
                matchedProduct = productRepository.findByTenantIdAndNameIgnoreCase(tenantId, item.supplierItemDescription.trim()).orElse(null)
            }

            if (matchedProduct == null) {
                // Register new product in catalog
                val parsedMock = ParsedNFeItem(
                    itemIndex = item.itemIndex,
                    supplierItemCode = item.supplierItemCode,
                    supplierBarcode = null,
                    description = item.supplierItemDescription,
                    ncm = "00000000",
                    cfop = "0000",
                    uom = item.supplierUom,
                    quantity = item.invoicedQuantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice,
                    freight = item.allocatedFreight,
                    insurance = item.allocatedInsurance,
                    expenses = item.allocatedExpenses,
                    discount = BigDecimal.ZERO,
                    icmsAmount = item.icmsAmount,
                    ipiAmount = item.ipiAmount,
                    pisAmount = item.pisAmount,
                    cofinsAmount = item.cofinsAmount,
                )
                matchedProduct = createAndMapProduct(tenantId, invoice.supplierId, parsedMock)
            } else {
                // Save de-para mapping
                val mapping = SupplierProductMapping(
                    tenantId = tenantId,
                    supplierId = invoice.supplierId,
                    supplierItemCode = item.supplierItemCode.trim(),
                    productId = matchedProduct.id,
                    supplierUom = item.supplierUom,
                    conversionFactor = BigDecimal.ONE,
                )
                supplierProductMappingRepository.save(mapping)
            }

            item.matchedProductId = matchedProduct.id
            item.internalUom = matchedProduct.unitOfMeasure
            item.conversionFactor = BigDecimal.ONE
            item.internalQuantity = item.invoicedQuantity

            calculateItemCost(item, profile)
        }

        invoice.updatedAt = Instant.now()
        val saved = invoiceRepository.save(invoice)

        auditService.recordAudit(
            entityType = "InboundInvoice",
            entityId = saved.id.toString(),
            action = "AUTO_REGISTER_PRODUCTS",
            details = "Cadastrados e vinculados automaticamente ${unmappedItems.size} produtos na NF-e ${saved.invoiceNumber}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    private fun createAndMapProduct(tenantId: UUID, supplierId: UUID, item: ParsedNFeItem): Product {
        val category = categoryRepository.findAllByTenantIdAndIsActiveTrueOrderByCodeAsc(tenantId)
            .firstOrNull()
            ?: categoryRepository.save(
                ProductCategory(
                    tenantId = tenantId,
                    code = "GERAL",
                    name = "Geral",
                    colorHex = "#10B981",
                    isActive = true,
                )
            )

        val cleanSku = if (!productRepository.existsByTenantIdAndSku(tenantId, item.supplierItemCode.trim())) {
            item.supplierItemCode.trim()
        } else {
            "PRD-${random.nextInt(90000) + 10000}"
        }

        val barcode = item.supplierBarcode?.trim()?.takeIf {
            it.isNotBlank() && !it.equals("SEM GTIN", ignoreCase = true) && !it.equals("SEM_GTIN", ignoreCase = true)
        }

        val finalBarcode = if (barcode != null && !productRepository.existsByTenantIdAndBarcode(tenantId, barcode)) {
            barcode
        } else {
            null
        }

        val cleanNcm = if (item.ncm.isNotBlank() && item.ncm.length <= 8) item.ncm.trim() else "00000000"
        val cleanUom = if (item.uom.isNotBlank() && item.uom.length <= 10) item.uom.trim().uppercase() else "UN"

        val product = Product(
            tenantId = tenantId,
            sku = cleanSku,
            barcode = finalBarcode,
            name = item.description.trim(),
            unitOfMeasure = cleanUom,
            categoryId = category.id,
            ncm = cleanNcm,
            lastAcquisitionCost = item.unitPrice,
            averageCost = item.unitPrice,
            replacementCost = item.unitPrice,
            isActive = true,
        )

        val saved = productRepository.save(product)

        val mapping = SupplierProductMapping(
            tenantId = tenantId,
            supplierId = supplierId,
            supplierItemCode = item.supplierItemCode.trim(),
            supplierBarcode = finalBarcode,
            productId = saved.id,
            supplierUom = item.uom,
            conversionFactor = BigDecimal.ONE,
        )
        supplierProductMappingRepository.save(mapping)

        return saved
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

        if (existingMapping.isPresent) {
            val mapping = existingMapping.get()
            mapping.productId = product.id
            mapping.conversionFactor = request.conversionFactor
            mapping.updatedAt = Instant.now()
            supplierProductMappingRepository.save(mapping)
        } else {
            val newMapping = SupplierProductMapping(
                tenantId = tenantId,
                supplierId = invoice.supplierId,
                supplierItemCode = item.supplierItemCode,
                productId = product.id,
                supplierUom = item.supplierUom,
                conversionFactor = request.conversionFactor,
            )
            supplierProductMappingRepository.save(newMapping)
        }

        // Recalculate cost
        val profile = invoice.costFormulaProfileId?.let {
            costFormulaProfileRepository.findByIdAndTenantId(it, tenantId).orElse(null)
        }
        calculateItemCost(item, profile)

        invoice.updatedAt = Instant.now()
        val saved = invoiceRepository.save(invoice)

        auditService.recordAudit(
            entityType = "InboundInvoice",
            entityId = saved.id.toString(),
            action = "MATCH_PRODUCT",
            details = "Matched item '${item.supplierItemCode}' to product '${product.name}' with factor ${request.conversionFactor}",
            stateAfter = saved,
        )

        return mapToResponse(saved)
    }

    private fun calculateItemCost(item: InboundInvoiceItem, profile: CostFormulaProfile?) {
        val qty = if (item.internalQuantity > BigDecimal.ZERO) item.internalQuantity else BigDecimal.ONE
        val invoiceUnitPrice = item.totalPrice.divide(qty, 4, RoundingMode.HALF_UP)

        if (profile != null && profile.formulaExpression.isNotBlank()) {
            val vars = mutableMapOf<String, BigDecimal>()
            vars["INVOICE_UNIT_PRICE"] = invoiceUnitPrice
            vars["QUANTITY"] = qty
            vars["FREIGHT_ALLOCATED"] = item.allocatedFreight
            vars["INSURANCE_ALLOCATED"] = item.allocatedInsurance
            vars["OTHER_EXPENSES_ALLOCATED"] = item.allocatedExpenses
            vars["ICMS_CREDIT"] = item.icmsCreditAmount.divide(qty, 4, RoundingMode.HALF_UP)
            vars["PIS_CREDIT"] = item.pisCreditAmount.divide(qty, 4, RoundingMode.HALF_UP)
            vars["COFINS_CREDIT"] = item.cofinsCreditAmount.divide(qty, 4, RoundingMode.HALF_UP)
            vars["IPI_COST"] = item.ipiAmount.divide(qty, 4, RoundingMode.HALF_UP)
            vars["ICMS_ST_COST"] = BigDecimal.ZERO

            val computedUnitCost = formulaEngine.evaluate(profile.formulaExpression, vars)
            item.finalLandedUnitCost = computedUnitCost
            item.finalTotalCost = computedUnitCost.multiply(qty).setScale(4, RoundingMode.HALF_UP)
        } else {
            // Default statutory landed cost: UnitPrice + (Freight+Insurance+Expenses)/Qty - Taxes
            val extraPerUnit = item.allocatedFreight.add(item.allocatedInsurance).add(item.allocatedExpenses)
                .divide(qty, 4, RoundingMode.HALF_UP)
            val taxCreditsPerUnit = item.icmsCreditAmount.add(item.pisCreditAmount).add(item.cofinsCreditAmount)
                .divide(qty, 4, RoundingMode.HALF_UP)
            val defaultUnitCost = invoiceUnitPrice.add(extraPerUnit).subtract(taxCreditsPerUnit).max(BigDecimal.ZERO)

            item.finalLandedUnitCost = defaultUnitCost
            item.finalTotalCost = defaultUnitCost.multiply(qty).setScale(4, RoundingMode.HALF_UP)
        }
    }

    fun finalizeStock(invoiceId: UUID, request: FinalizeInvoiceStockRequest): InboundInvoiceResponse {
        val tenantId = getCurrentTenantId()
        val invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
            .orElseThrow { IllegalArgumentException("Nota fiscal de entrada '$invoiceId' não encontrada.") }

        if (invoice.status == InboundInvoiceStatus.STOCKED) {
            throw IllegalStateException("Nota fiscal já foi estocada anteriormente.")
        }

        // Verify all items are matched
        val unmapped = invoice.items.filter { it.matchedProductId == null }
        if (unmapped.isNotEmpty()) {
            throw IllegalStateException("Existem ${unmapped.size} itens não vinculados a produtos do catálogo.")
        }

        invoice.items.forEach { item ->
            val product = productRepository.findByIdAndTenantId(item.matchedProductId!!, tenantId)
                .orElseThrow { NoSuchElementException("Produto não encontrado: ${item.matchedProductId}") }

            val inboundQty = item.internalQuantity
            val landedUnitCost = item.finalLandedUnitCost

            val currentStock = stockBalanceRepository.findAllByTenantIdAndProductId(tenantId, product.id)
                .map { it.quantityOnHand }
                .fold(BigDecimal.ZERO) { acc, q -> acc.add(q) }

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
            val balance = stockBalanceRepository.findByWarehouseIdAndProductIdAndLocationIdIsNull(
                request.warehouseId, product.id
            ).orElseGet {
                StockBalance(
                    tenantId = tenantId,
                    warehouseId = request.warehouseId,
                    productId = product.id,
                    quantityOnHand = BigDecimal.ZERO,
                    quantityReserved = BigDecimal.ZERO,
                    quantityAvailable = BigDecimal.ZERO,
                )
            }

            balance.quantityOnHand = balance.quantityOnHand.add(inboundQty)
            balance.quantityAvailable = balance.quantityOnHand.subtract(balance.quantityReserved)
            balance.updatedAt = Instant.now()
            stockBalanceRepository.save(balance)

            // Immutable Movement posting (TRANSFER_IN / INBOUND_PURCHASE)
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
            .orElse("Fornecedor não identificado")

        val profileName = inv.costFormulaProfileId?.let {
            costFormulaProfileRepository.findByIdAndTenantId(it, tenantId).map { p -> p.name }.orElse(null)
        }

        val productIds = inv.items.mapNotNull { it.matchedProductId }
        val productMap = if (productIds.isNotEmpty()) {
            productRepository.findAllById(productIds).associateBy { it.id }
        } else {
            emptyMap()
        }

        val itemDetails = inv.items.mapNotNull { item ->
            item.matchedProductId?.let { id ->
                val prod = productMap[id]
                if (prod != null) id to Pair(prod.name, prod.sku) else null
            }
        }.toMap()

        return InboundInvoiceResponse.fromEntity(
            inv = inv,
            supplierName = supplierName,
            profileName = profileName,
            itemDetails = itemDetails,
        )
    }
}
