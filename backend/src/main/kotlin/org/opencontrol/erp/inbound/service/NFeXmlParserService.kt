package org.opencontrol.erp.inbound.service

import org.springframework.stereotype.Service
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.InputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedNFeHeader(
    val accessKey: String,
    val invoiceNumber: String,
    val series: String,
    val issuanceDate: LocalDate,
    val supplierCnpjOrCpf: String,
    val supplierLegalName: String,
    val supplierTradeName: String?,
    val totalProducts: BigDecimal,
    val totalFreight: BigDecimal,
    val totalInsurance: BigDecimal,
    val totalExpenses: BigDecimal,
    val totalDiscount: BigDecimal,
    val totalInvoice: BigDecimal,
    val totalIcms: BigDecimal,
    val totalIpi: BigDecimal,
    val totalPis: BigDecimal,
    val totalCofins: BigDecimal,
)

data class ParsedNFeItem(
    val itemIndex: Int,
    val supplierItemCode: String,
    val supplierBarcode: String?,
    val description: String,
    val ncm: String,
    val cfop: String,
    val uom: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val freight: BigDecimal,
    val insurance: BigDecimal,
    val expenses: BigDecimal,
    val discount: BigDecimal,
    val icmsAmount: BigDecimal,
    val ipiAmount: BigDecimal,
    val pisAmount: BigDecimal,
    val cofinsAmount: BigDecimal,
)

data class ParsedNFe(
    val header: ParsedNFeHeader,
    val items: List<ParsedNFeItem>,
)

@Service
class NFeXmlParserService {

    fun parse(xmlStream: InputStream): ParsedNFe {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlStream)
        doc.documentElement.normalize()

        val infNFeList = doc.getElementsByTagName("infNFe")
        if (infNFeList.length == 0) {
            throw IllegalArgumentException("Arquivo XML não contém a tag raiz <infNFe> de NF-e SEFAZ válida.")
        }
        val infNFe = infNFeList.item(0) as Element
        val rawId = infNFe.getAttribute("Id") ?: ""
        val accessKey = rawId.replace("NFe", "").trim()

        val ide = doc.getElementsByTagName("ide").item(0) as? Element
            ?: throw IllegalArgumentException("Tag <ide> não encontrada no XML.")
        val invoiceNumber = getTagValue(ide, "nNF") ?: "0"
        val series = getTagValue(ide, "serie") ?: "1"
        val dhEmiStr = getTagValue(ide, "dhEmi") ?: getTagValue(ide, "dEmi") ?: ""
        val issuanceDate = parseDate(dhEmiStr)

        val emit = doc.getElementsByTagName("emit").item(0) as? Element
            ?: throw IllegalArgumentException("Tag <emit> não encontrada no XML.")
        val supplierDoc = getTagValue(emit, "CNPJ") ?: getTagValue(emit, "CPF") ?: ""
        val supplierLegalName = getTagValue(emit, "xNome") ?: "Fornecedor Desconhecido"
        val supplierTradeName = getTagValue(emit, "xFant")

        val icmsTot = doc.getElementsByTagName("ICMSTot").item(0) as? Element
        val totalProducts = parseBigDecimal(getTagValue(icmsTot, "vProd"))
        val totalFreight = parseBigDecimal(getTagValue(icmsTot, "vFrete"))
        val totalInsurance = parseBigDecimal(getTagValue(icmsTot, "vSeg"))
        val totalExpenses = parseBigDecimal(getTagValue(icmsTot, "vOutro"))
        val totalDiscount = parseBigDecimal(getTagValue(icmsTot, "vDesc"))
        val totalInvoice = parseBigDecimal(getTagValue(icmsTot, "vNF"))
        val totalIcms = parseBigDecimal(getTagValue(icmsTot, "vICMS"))
        val totalIpi = parseBigDecimal(getTagValue(icmsTot, "vIPI"))
        val totalPis = parseBigDecimal(getTagValue(icmsTot, "vPIS"))
        val totalCofins = parseBigDecimal(getTagValue(icmsTot, "vCOFINS"))

        val header = ParsedNFeHeader(
            accessKey = accessKey,
            invoiceNumber = invoiceNumber,
            series = series,
            issuanceDate = issuanceDate,
            supplierCnpjOrCpf = supplierDoc,
            supplierLegalName = supplierLegalName,
            supplierTradeName = supplierTradeName,
            totalProducts = totalProducts,
            totalFreight = totalFreight,
            totalInsurance = totalInsurance,
            totalExpenses = totalExpenses,
            totalDiscount = totalDiscount,
            totalInvoice = totalInvoice,
            totalIcms = totalIcms,
            totalIpi = totalIpi,
            totalPis = totalPis,
            totalCofins = totalCofins,
        )

        val detList = doc.getElementsByTagName("det")
        val items = mutableListOf<ParsedNFeItem>()

        for (i in 0 until detList.length) {
            val det = detList.item(i) as Element
            val itemIndex = det.getAttribute("nItem")?.toIntOrNull() ?: (i + 1)
            val prod = det.getElementsByTagName("prod").item(0) as? Element ?: continue

            val supplierItemCode = getTagValue(prod, "cProd") ?: "ITEM-$itemIndex"
            val supplierBarcode = getTagValue(prod, "cEAN")?.takeIf { it != "SEM GTIN" }
            val description = getTagValue(prod, "xProd") ?: "Produto sem descrição"
            val ncm = getTagValue(prod, "NCM") ?: ""
            val cfop = getTagValue(prod, "CFOP") ?: ""
            val uom = getTagValue(prod, "uCom") ?: "UN"
            val quantity = parseBigDecimal(getTagValue(prod, "qCom"))
            val unitPrice = parseBigDecimal(getTagValue(prod, "vUnCom"))
            val totalPrice = parseBigDecimal(getTagValue(prod, "vProd"))
            val freight = parseBigDecimal(getTagValue(prod, "vFrete"))
            val insurance = parseBigDecimal(getTagValue(prod, "vSeg"))
            val expenses = parseBigDecimal(getTagValue(prod, "vOutro"))
            val discount = parseBigDecimal(getTagValue(prod, "vDesc"))

            val imposto = det.getElementsByTagName("imposto").item(0) as? Element
            val icmsAmount = parseBigDecimal(getTagValue(imposto, "vICMS"))
            val ipiAmount = parseBigDecimal(getTagValue(imposto, "vIPI"))
            val pisAmount = parseBigDecimal(getTagValue(imposto, "vPIS"))
            val cofinsAmount = parseBigDecimal(getTagValue(imposto, "vCOFINS"))

            items.add(
                ParsedNFeItem(
                    itemIndex = itemIndex,
                    supplierItemCode = supplierItemCode,
                    supplierBarcode = supplierBarcode,
                    description = description,
                    ncm = ncm,
                    cfop = cfop,
                    uom = uom,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    totalPrice = totalPrice,
                    freight = freight,
                    insurance = insurance,
                    expenses = expenses,
                    discount = discount,
                    icmsAmount = icmsAmount,
                    ipiAmount = ipiAmount,
                    pisAmount = pisAmount,
                    cofinsAmount = cofinsAmount,
                )
            )
        }

        return ParsedNFe(header = header, items = items)
    }

    private fun getTagValue(element: Element?, tagName: String): String? {
        if (element == null) return null
        val list = element.getElementsByTagName(tagName)
        if (list.length > 0) {
            val node = list.item(0)
            return node?.textContent?.trim()
        }
        return null
    }

    private fun parseBigDecimal(value: String?): BigDecimal {
        if (value.isNullOrBlank()) return BigDecimal.ZERO
        return try {
            BigDecimal(value).setScale(4, RoundingMode.HALF_UP)
        } catch (_: Exception) {
            BigDecimal.ZERO
        }
    }

    private fun parseDate(dateStr: String): LocalDate {
        if (dateStr.isBlank()) return LocalDate.now()
        return try {
            if (dateStr.contains("T")) {
                LocalDate.parse(dateStr.substringBefore("T"))
            } else {
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            }
        } catch (_: Exception) {
            LocalDate.now()
        }
    }
}
