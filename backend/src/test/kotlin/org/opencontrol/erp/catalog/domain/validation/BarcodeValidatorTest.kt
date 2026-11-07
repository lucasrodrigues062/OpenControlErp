package org.opencontrol.erp.catalog.domain.validation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class BarcodeValidatorTest {
    @Test
    @DisplayName("Should validate correct GS1 EAN-13 barcode")
    fun shouldValidateCorrectEan13() {
        // 7891000315507 is a valid EAN-13
        assertThat(BarcodeValidator.isValid("7891000315507")).isTrue()
        assertThat(BarcodeValidator.validateModulo10("7891000315507")).isTrue()
    }

    @Test
    @DisplayName("Should reject invalid GS1 EAN-13 check digit")
    fun shouldRejectInvalidEan13CheckDigit() {
        assertThat(BarcodeValidator.isValid("7891000315508")).isFalse()
        assertThat(BarcodeValidator.validateModulo10("7891000315508")).isFalse()
    }

    @Test
    @DisplayName("Should accept nullable or empty barcode")
    fun shouldAcceptNullOrEmptyBarcode() {
        assertThat(BarcodeValidator.isValid(null)).isTrue()
        assertThat(BarcodeValidator.isValid("")).isTrue()
        assertThat(BarcodeValidator.isValid("   ")).isTrue()
    }

    @Test
    @DisplayName("Should accept internal alphanumeric barcode")
    fun shouldAcceptInternalAlphanumericBarcode() {
        assertThat(BarcodeValidator.isValid("INT-PROD-001")).isTrue()
        assertThat(BarcodeValidator.isValid("SKU_9942")).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["8471.30.12", "84713012", "01012100", "99999999"])
    @DisplayName("Should validate valid 8-digit NCMs")
    fun shouldValidateValidNcm(ncm: String) {
        assertThat(BarcodeValidator.isValidNcm(ncm)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "123", "8471301", "847130129", "8471.30.1", "INVALID8"])
    @DisplayName("Should reject invalid NCMs")
    fun shouldRejectInvalidNcm(ncm: String) {
        assertThat(BarcodeValidator.isValidNcm(ncm)).isFalse()
    }

    @ParameterizedTest
    @ValueSource(strings = ["0100100", "01.001.00", "2803800"])
    @DisplayName("Should validate valid 7-digit CESTs")
    fun shouldValidateValidCest(cest: String) {
        assertThat(BarcodeValidator.isValidCest(cest)).isTrue()
    }

    @Test
    @DisplayName("Should allow null or blank CEST when product is not subject to tax substitution")
    fun shouldAllowBlankCest() {
        assertThat(BarcodeValidator.isValidCest(null)).isTrue()
        assertThat(BarcodeValidator.isValidCest("")).isTrue()
    }

    @ParameterizedTest
    @ValueSource(strings = ["123456", "12345678", "01.00.00", "ABCDEFG"])
    @DisplayName("Should reject invalid CESTs")
    fun shouldRejectInvalidCest(cest: String) {
        assertThat(BarcodeValidator.isValidCest(cest)).isFalse()
    }

    @Test
    @DisplayName("Should validate official Brazilian origin codes (0 to 8)")
    fun shouldValidateOriginCodes() {
        for (origin in 0..8) {
            assertThat(BarcodeValidator.isValidOrigin(origin)).isTrue()
        }
        assertThat(BarcodeValidator.isValidOrigin(-1)).isFalse()
        assertThat(BarcodeValidator.isValidOrigin(9)).isFalse()
        assertThat(BarcodeValidator.isValidOrigin(99)).isFalse()
    }
}
