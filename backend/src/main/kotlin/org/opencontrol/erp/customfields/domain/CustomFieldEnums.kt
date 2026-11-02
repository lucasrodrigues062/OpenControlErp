package org.opencontrol.erp.customfields.domain

/**
 * Supported data types for dynamic custom fields.
 */
enum class FieldType {
    TEXT,
    NUMBER,
    DECIMAL,
    DATE,
    BOOLEAN,
    SELECT,
}

/**
 * Declarative validation rules configured for a custom field definition.
 */
data class CustomFieldValidationRules(
    val min: Double? = null,
    val max: Double? = null,
    val regex: String? = null,
)
