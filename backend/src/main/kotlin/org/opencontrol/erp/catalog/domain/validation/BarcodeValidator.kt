package org.opencontrol.erp.catalog.domain.validation

object BarcodeValidator {
    /**
     * Validates whether a barcode string matches standard GS1 formats (EAN-8, UPC-A, EAN-13, ITF-14)
     * using the official Modulo 10 check digit algorithm, or conforms to a valid internal code.
     */
    fun isValid(barcode: String?): Boolean {
        if (barcode.isNullOrBlank()) return true // Nullable barcode is valid
        val cleaned = barcode.trim()
        if (cleaned.length > 50) return false

        // Standard GS1 length checks: 8, 12, 13, 14 numeric digits
        if (cleaned.matches(Regex("^[0-9]+$"))) {
            return when (cleaned.length) {
                8, 12, 13, 14 -> validateModulo10(cleaned)
                else -> true // Internal numeric code
            }
        }

        // Internal alphanumeric barcode
        return cleaned.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }

    /**
     * Computes or verifies the standard GS1 Modulo 10 check digit.
     * Weights alternate (3, 1, 3, 1...) reading from right to left (excluding the check digit itself).
     */
    fun validateModulo10(numericCode: String): Boolean {
        if (!numericCode.matches(Regex("^[0-9]+$")) || numericCode.length < 2) return false

        val body = numericCode.substring(0, numericCode.length - 1)
        val expectedCheckDigit = Character.getNumericValue(numericCode.last())

        var sum = 0
        var weight = 3
        for (i in body.length - 1 downTo 0) {
            val digit = Character.getNumericValue(body[i])
            sum += digit * weight
            weight = if (weight == 3) 1 else 3
        }

        val remainder = sum % 10
        val calculatedCheckDigit = if (remainder == 0) 0 else 10 - remainder

        return calculatedCheckDigit == expectedCheckDigit
    }

    /**
     * Validates Brazilian Mercosul Common Nomenclature (NCM).
     * Must be exactly 8 numeric digits.
     */
    fun isValidNcm(ncm: String?): Boolean {
        if (ncm.isNullOrBlank()) return false
        val digits = ncm.trim().replace(".", "")
        return digits.length == 8 && digits.matches(Regex("^[0-9]{8}$"))
    }

    /**
     * Validates Brazilian Tax Substitution Code (CEST).
     * Must be exactly 7 numeric digits (or null/blank if not subject to ST).
     */
    fun isValidCest(cest: String?): Boolean {
        if (cest.isNullOrBlank()) return true
        val digits = cest.trim().replace(".", "")
        return digits.length == 7 && digits.matches(Regex("^[0-9]{7}$"))
    }

    /**
     * Validates Goods Origin Table (Origem da Mercadoria: 0 to 8).
     */
    fun isValidOrigin(origin: Int): Boolean = origin in 0..8
}
