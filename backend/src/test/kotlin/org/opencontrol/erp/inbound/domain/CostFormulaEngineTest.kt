package org.opencontrol.erp.inbound.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

class CostFormulaEngineTest {
    private val engine = CostFormulaEngine()

    @Test
    fun `should evaluate simple arithmetic with native variables`() {
        val formula = "= INVOICE_UNIT_PRICE + (FREIGHT_ALLOCATED / QUANTITY)"
        val vars = mapOf(
            "INVOICE_UNIT_PRICE" to BigDecimal("100.00"),
            "FREIGHT_ALLOCATED" to BigDecimal("50.00"),
            "QUANTITY" to BigDecimal("10.00"),
        )
        val result = engine.evaluate(formula, vars)
        assertEquals(BigDecimal("105.0000"), result)
    }

    @Test
    fun `should evaluate formula with user custom variables`() {
        val formula = "= (INVOICE_UNIT_PRICE * (1 + FATOR_QUEBRA_PCT)) + ((FREIGHT_ALLOCATED + TAXA_DESCARGA) / QUANTITY) - ICMS_CREDIT"
        val vars = mapOf(
            "INVOICE_UNIT_PRICE" to BigDecimal("100.00"),
            "FATOR_QUEBRA_PCT" to BigDecimal("0.02"), // 2% quebra
            "FREIGHT_ALLOCATED" to BigDecimal("40.00"),
            "TAXA_DESCARGA" to BigDecimal("10.00"),
            "QUANTITY" to BigDecimal("10.00"),
            "ICMS_CREDIT" to BigDecimal("12.00"),
        )
        // 100 * 1.02 = 102
        // (40 + 10) / 10 = 5
        // 102 + 5 - 12 = 95.00
        val result = engine.evaluate(formula, vars)
        assertEquals(BigDecimal("95.0000"), result)
    }

    @Test
    fun `should evaluate Excel functions IF and ROUND correctly`() {
        val formula = "ROUND(IF(QUANTITY > 5, INVOICE_UNIT_PRICE * 0.9, INVOICE_UNIT_PRICE), 2)"
        val varsTier1 = mapOf(
            "INVOICE_UNIT_PRICE" to BigDecimal("100.00"),
            "QUANTITY" to BigDecimal("10.00"),
        )
        val res1 = engine.evaluate(formula, varsTier1)
        assertEquals(BigDecimal("90.0000"), res1)

        val varsTier2 = mapOf(
            "INVOICE_UNIT_PRICE" to BigDecimal("100.00"),
            "QUANTITY" to BigDecimal("2.00"),
        )
        val res2 = engine.evaluate(formula, varsTier2)
        assertEquals(BigDecimal("100.0000"), res2)
    }

    @Test
    fun `should handle unprovided variables gracefully as zero`() {
        val formula = "= INVOICE_UNIT_PRICE + MISSING_FEE"
        val vars = mapOf(
            "INVOICE_UNIT_PRICE" to BigDecimal("50.00"),
        )
        val result = engine.evaluate(formula, vars)
        assertEquals(BigDecimal("50.0000"), result)
    }
}
