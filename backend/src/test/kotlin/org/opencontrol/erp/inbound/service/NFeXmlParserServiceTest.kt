package org.opencontrol.erp.inbound.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal

class NFeXmlParserServiceTest {
    private val parser = NFeXmlParserService()

    @Test
    fun `should parse valid SEFAZ NFe XML`() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <nfeProc versao="4.00" xmlns="http://www.portalfiscal.inf.br/nfe">
              <NFe>
                <infNFe Id="NFe35261100000000000191550010000123451000123450">
                  <ide>
                    <nNF>12345</nNF>
                    <serie>1</serie>
                    <dhEmi>2026-11-08T10:00:00-03:00</dhEmi>
                  </ide>
                  <emit>
                    <CNPJ>12345678000199</CNPJ>
                    <xNome>Fornecedor Industrial Alfa SA</xNome>
                    <xFant>Alfa Industria</xFant>
                  </emit>
                  <total>
                    <ICMSTot>
                      <vProd>1000.00</vProd>
                      <vFrete>50.00</vFrete>
                      <vSeg>10.00</vSeg>
                      <vDesc>0.00</vDesc>
                      <vOutro>5.00</vOutro>
                      <vNF>1065.00</vNF>
                      <vICMS>120.00</vICMS>
                      <vIPI>50.00</vIPI>
                      <vPIS>16.50</vPIS>
                      <vCOFINS>76.00</vCOFINS>
                    </ICMSTot>
                  </total>
                  <det nItem="1">
                    <prod>
                      <cProd>ALFA-99</cProd>
                      <cEAN>7891234567890</cEAN>
                      <xProd>Parafuso Sextavado Aço 10mm</xProd>
                      <NCM>73181500</NCM>
                      <CFOP>5102</CFOP>
                      <uCom>CX</uCom>
                      <qCom>10.0000</qCom>
                      <vUnCom>100.0000</vUnCom>
                      <vProd>1000.00</vProd>
                      <vFrete>50.00</vFrete>
                      <vSeg>10.00</vSeg>
                      <vOutro>5.00</vOutro>
                    </prod>
                    <imposto>
                      <ICMS>
                        <ICMS00>
                          <vICMS>120.00</vICMS>
                        </ICMS00>
                      </ICMS>
                      <IPI>
                        <IPITrib>
                          <vIPI>50.00</vIPI>
                        </IPITrib>
                      </IPI>
                      <PIS>
                        <PISAliq>
                          <vPIS>16.50</vPIS>
                        </PISAliq>
                      </PIS>
                      <COFINS>
                        <COFINSAliq>
                          <vCOFINS>76.00</vCOFINS>
                        </COFINSAliq>
                      </COFINS>
                    </imposto>
                  </det>
                </infNFe>
              </NFe>
            </nfeProc>
        """.trimIndent()

        val parsed = parser.parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
        assertNotNull(parsed)
        assertEquals("35261100000000000191550010000123451000123450", parsed.header.accessKey)
        assertEquals("12345", parsed.header.invoiceNumber)
        assertEquals("12345678000199", parsed.header.supplierCnpjOrCpf)
        assertEquals("Fornecedor Industrial Alfa SA", parsed.header.supplierLegalName)
        assertEquals(BigDecimal("1065.0000"), parsed.header.totalInvoice)

        assertEquals(1, parsed.items.size)
        val item = parsed.items[0]
        assertEquals("ALFA-99", item.supplierItemCode)
        assertEquals("7891234567890", item.supplierBarcode)
        assertEquals("CX", item.uom)
        assertEquals(BigDecimal("10.0000"), item.quantity)
        assertEquals(BigDecimal("100.0000"), item.unitPrice)
        assertEquals(BigDecimal("50.0000"), item.freight)
    }
}
