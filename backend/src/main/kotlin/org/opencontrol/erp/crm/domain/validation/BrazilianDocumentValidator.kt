package org.opencontrol.erp.crm.domain.validation

import org.springframework.stereotype.Component

/**
 * Validates and formats statutory Brazilian identification documents:
 * - CPF (Cadastro de Pessoas Físicas - 11 numeric digits)
 * - CNPJ (Cadastro Nacional da Pessoa Jurídica - 14 numeric digits)
 *
 * Implements strict Modulo 11 check-digit verification per Brazilian Federal Revenue standards.
 */
@Component
class BrazilianDocumentValidator {
    fun clean(document: String?): String {
        if (document.isNullOrBlank()) return ""
        return document.replace(Regex("[^0-9]"), "")
    }

    fun isValidCpf(cpf: String?): Boolean {
        val cleanCpf = clean(cpf)
        if (cleanCpf.length != 11) return false
        if (cleanCpf.all { it == cleanCpf[0] }) return false

        val d1 = calculateCpfDigit(cleanCpf.substring(0, 9), 10)
        val d2 = calculateCpfDigit(cleanCpf.substring(0, 9) + d1, 11)

        return cleanCpf.endsWith("$d1$d2")
    }

    fun isValidCnpj(cnpj: String?): Boolean {
        val cleanCnpj = clean(cnpj)
        if (cleanCnpj.length != 14) return false
        if (cleanCnpj.all { it == cleanCnpj[0] }) return false

        val d1 = calculateCnpjDigit(cleanCnpj.substring(0, 12), intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))
        val d2 = calculateCnpjDigit(cleanCnpj.substring(0, 12) + d1, intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2))

        return cleanCnpj.endsWith("$d1$d2")
    }

    fun isValid(
        document: String?,
        isIndividual: Boolean,
    ): Boolean =
        if (isIndividual) {
            isValidCpf(document)
        } else {
            isValidCnpj(document)
        }

    fun format(document: String?): String {
        val cleanDoc = clean(document)
        return when (cleanDoc.length) {
            11 -> "${cleanDoc.substring(0, 3)}.${cleanDoc.substring(3, 6)}.${cleanDoc.substring(6, 9)}-${cleanDoc.substring(9, 11)}"
            14 -> "${cleanDoc.substring(
                0,
                2,
            )}.${cleanDoc.substring(2, 5)}.${cleanDoc.substring(5, 8)}/${cleanDoc.substring(8, 12)}-${cleanDoc.substring(12, 14)}"
            else -> document ?: ""
        }
    }

    private fun calculateCpfDigit(
        str: String,
        weightStart: Int,
    ): Int {
        var sum = 0
        var weight = weightStart
        for (ch in str) {
            sum += (ch - '0') * weight
            weight--
        }
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }

    private fun calculateCnpjDigit(
        str: String,
        weights: IntArray,
    ): Int {
        var sum = 0
        for (i in str.indices) {
            sum += (str[i] - '0') * weights[i]
        }
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }
}
