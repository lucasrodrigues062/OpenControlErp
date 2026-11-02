package org.opencontrol.erp.customfields.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.opencontrol.erp.customfields.domain.CustomFieldValidationRules
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import java.util.UUID

class CustomFieldValidatorTest {
    private val validator = CustomFieldValidator()
    private val tenantId = UUID.randomUUID()

    @Test
    fun `should pass validation for valid text, number, decimal, date, boolean, and select`() {
        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "spouse_name",
                    label = "Spouse Name",
                    fieldType = FieldType.TEXT,
                    required = true,
                    validationRules = CustomFieldValidationRules(min = 2.0, max = 50.0),
                ),
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "dependents_count",
                    label = "Dependents",
                    fieldType = FieldType.NUMBER,
                    validationRules = CustomFieldValidationRules(min = 0.0, max = 20.0),
                ),
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "credit_limit",
                    label = "Credit Limit",
                    fieldType = FieldType.DECIMAL,
                    validationRules = CustomFieldValidationRules(min = 0.0),
                ),
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "wedding_date",
                    label = "Wedding Date",
                    fieldType = FieldType.DATE,
                ),
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "is_vip",
                    label = "Is VIP",
                    fieldType = FieldType.BOOLEAN,
                ),
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "tier",
                    label = "Tier",
                    fieldType = FieldType.SELECT,
                    options = listOf("BRONZE", "SILVER", "GOLD"),
                ),
            )

        val input =
            mapOf(
                "spouse_name" to "Jane Doe",
                "dependents_count" to 3,
                "credit_limit" to "1500.50",
                "wedding_date" to "2020-05-15",
                "is_vip" to "true",
                "tier" to "GOLD",
            )

        val result = validator.validateAndSanitize(defs, input)

        assertEquals("Jane Doe", result["spouse_name"])
        assertEquals(3L, result["dependents_count"])
        assertEquals(1500.50, result["credit_limit"])
        assertEquals("2020-05-15", result["wedding_date"])
        assertEquals(true, result["is_vip"])
        assertEquals("GOLD", result["tier"])
    }

    @Test
    fun `should throw when required field is missing`() {
        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "spouse_name",
                    label = "Spouse Name",
                    fieldType = FieldType.TEXT,
                    required = true,
                ),
            )

        val input = mapOf<String, Any?>("other_field" to "value")

        val ex =
            assertThrows<InvalidFieldValueException> {
                validator.validateAndSanitize(defs, input)
            }
        assertTrue(ex.message!!.contains("is required"))
    }

    @Test
    fun `should throw when text violates min or max length`() {
        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "code",
                    label = "Code",
                    fieldType = FieldType.TEXT,
                    validationRules = CustomFieldValidationRules(min = 3.0, max = 5.0),
                ),
            )

        assertThrows<InvalidFieldValueException> {
            validator.validateAndSanitize(defs, mapOf("code" to "AB"))
        }

        assertThrows<InvalidFieldValueException> {
            validator.validateAndSanitize(defs, mapOf("code" to "TOOLONG"))
        }
    }

    @Test
    fun `should throw when select value is not in options`() {
        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "tier",
                    label = "Tier",
                    fieldType = FieldType.SELECT,
                    options = listOf("BRONZE", "SILVER"),
                ),
            )

        val ex =
            assertThrows<InvalidFieldValueException> {
                validator.validateAndSanitize(defs, mapOf("tier" to "DIAMOND"))
            }
        assertTrue(ex.message!!.contains("Allowed options"))
    }

    @Test
    fun `should throw when date is malformed`() {
        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "birth",
                    label = "Birth Date",
                    fieldType = FieldType.DATE,
                ),
            )

        assertThrows<InvalidFieldValueException> {
            validator.validateAndSanitize(defs, mapOf("birth" to "15/05/1990"))
        }
    }
}
