package org.opencontrol.erp.crm.domain.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BrazilianDocumentValidatorTest {
    private val validator = BrazilianDocumentValidator()

    @Test
    fun `should clean document removing all non-digit characters`() {
        assertEquals("53066208000170", validator.clean("53.066.208/0001-70"))
        assertEquals("52998224725", validator.clean("529.982.247-25"))
        assertEquals("", validator.clean("abc-def"))
    }

    @Test
    fun `should format CPF and CNPJ properly`() {
        assertEquals("529.982.247-25", validator.format("52998224725"))
        assertEquals("53.066.208/0001-70", validator.format("53066208000170"))
        assertEquals("123", validator.format("123"))
    }

    @Test
    fun `should validate valid CPFs`() {
        assertTrue(validator.isValidCpf("52998224725"))
        assertTrue(validator.isValidCpf("529.982.247-25"))
    }

    @Test
    fun `should reject invalid CPFs`() {
        assertFalse(validator.isValidCpf("11111111111"))
        assertFalse(validator.isValidCpf("00000000000"))
        assertFalse(validator.isValidCpf("52998224726")) // Wrong last digit
        assertFalse(validator.isValidCpf("1234567890")) // 10 digits
        assertFalse(validator.isValidCpf(null))
        assertFalse(validator.isValidCpf(""))
    }

    @Test
    fun `should validate valid CNPJs`() {
        assertTrue(validator.isValidCnpj("53066208000170"))
        assertTrue(validator.isValidCnpj("53.066.208/0001-70"))
        assertTrue(validator.isValidCnpj("04252011000110"))
    }

    @Test
    fun `should reject invalid CNPJs`() {
        assertFalse(validator.isValidCnpj("00000000000000"))
        assertFalse(validator.isValidCnpj("11111111111111"))
        assertFalse(validator.isValidCnpj("53066208000171")) // Wrong last digit
        assertFalse(validator.isValidCnpj("5306620800017")) // 13 digits
        assertFalse(validator.isValidCnpj(null))
        assertFalse(validator.isValidCnpj(""))
    }

    @Test
    fun `should validate by DocumentType`() {
        assertTrue(validator.isValid("52998224725", isIndividual = true))
        assertFalse(validator.isValid("53066208000170", isIndividual = true)) // CNPJ passed for CPF
        assertTrue(validator.isValid("53066208000170", isIndividual = false))
        assertFalse(validator.isValid("52998224725", isIndividual = false)) // CPF passed for CNPJ
    }
}
