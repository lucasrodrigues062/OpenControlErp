package org.opencontrol.erp.companies.service

/**
 * Utility for Brazilian CNPJ (Cadastro Nacional da Pessoa Jurídica) validation and formatting.
 * Implements the official Receita Federal Modulo 11 check digit verification algorithm.
 */
object CnpjValidator {
    private val WEIGHTS_FIRST_DIGIT = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
    private val WEIGHTS_SECOND_DIGIT = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)

    /**
     * Strips all non-digit characters from the input string.
     */
    fun clean(cnpj: String?): String = cnpj?.replace("\\D".toRegex(), "") ?: ""

    /**
     * Formats a 14-digit unmasked CNPJ to standard Brazilian display format: XX.XXX.XXX/XXXX-XX.
     */
    fun format(cnpj: String?): String {
        val digits = clean(cnpj)
        if (digits.length != 14) return digits
        return "${digits.substring(
            0,
            2,
        )}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.substring(12, 14)}"
    }

    /**
     * Validates whether the given CNPJ is mathematically valid according to Modulo 11.
     */
    fun isValid(cnpj: String?): Boolean {
        val digits = clean(cnpj)
        if (digits.length != 14) return false

        // Invalidate repeated character sequences (e.g., 00000000000000, 11111111111111)
        if (digits.all { it == digits[0] }) return false

        val numbers = digits.map { it.toString().toInt() }

        // Calculate 1st verification digit
        var sum1 = 0
        for (i in 0 until 12) {
            sum1 += numbers[i] * WEIGHTS_FIRST_DIGIT[i]
        }
        val remainder1 = sum1 % 11
        val expectedDigit1 = if (remainder1 < 2) 0 else 11 - remainder1
        if (numbers[12] != expectedDigit1) return false

        // Calculate 2nd verification digit
        var sum2 = 0
        for (i in 0 until 13) {
            sum2 += numbers[i] * WEIGHTS_SECOND_DIGIT[i]
        }
        val remainder2 = sum2 % 11
        val expectedDigit2 = if (remainder2 < 2) 0 else 11 - remainder2

        return numbers[13] == expectedDigit2
    }
}
