package org.opencontrol.erp.companies.service

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CnpjValidatorTest {
    @Test
    fun `should validate valid CNPJs correctly`() {
        // Valid Brazilian CNPJs (Receita Federal algorithm)
        assertTrue(CnpjValidator.isValid("11.222.333/0001-81"))
        assertTrue(CnpjValidator.isValid("11222333000181"))
        assertTrue(CnpjValidator.isValid("00.000.000/0001-91")) // Banco do Brasil
        assertTrue(CnpjValidator.isValid("33.000.167/0001-01")) // Petrobras
    }

    @Test
    fun `should reject invalid CNPJs`() {
        assertFalse(CnpjValidator.isValid(null))
        assertFalse(CnpjValidator.isValid(""))
        assertFalse(CnpjValidator.isValid("123"))
        assertFalse(CnpjValidator.isValid("00000000000000"))
        assertFalse(CnpjValidator.isValid("11111111111111"))
        assertFalse(CnpjValidator.isValid("99999999999999"))
        assertFalse(CnpjValidator.isValid("11.222.333/0001-82")) // Wrong check digit
        assertFalse(CnpjValidator.isValid("11222333000180"))
    }

    @Test
    fun `should format unmasked CNPJ to standard format`() {
        assertEquals("11.222.333/0001-81", CnpjValidator.format("11222333000181"))
        assertEquals("11.222.333/0001-81", CnpjValidator.format("11.222.333/0001-81"))
        assertEquals("123", CnpjValidator.format("123")) // Non-14 digits returned as cleaned
    }

    @Test
    fun `should clean non-digits from string`() {
        assertEquals("11222333000181", CnpjValidator.clean("11.222.333/0001-81"))
        assertEquals("", CnpjValidator.clean(null))
        assertEquals("123", CnpjValidator.clean("abc-1.2/3"))
    }
}
