package org.opencontrol.erp.customfields.service

import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Component
class CustomFieldValidator {
    /**
     * Validates a map of field values against the active definitions for an entity.
     * Returns sanitized and typed value map.
     */
    fun validateAndSanitize(
        definitions: List<CustomFieldDefinition>,
        incomingValues: Map<String, Any?>,
    ): Map<String, Any?> {
        val sanitized = mutableMapOf<String, Any?>()
        val defsByKey = definitions.associateBy { it.fieldKey }

        // 1. Check required fields
        for (def in definitions) {
            val value = incomingValues[def.fieldKey]
            val isMissing = value == null || (value is String && value.isBlank())

            if (def.required && isMissing) {
                throw InvalidFieldValueException("Field '${def.label}' (${def.fieldKey}) is required")
            }
        }

        // 2. Validate and convert each incoming value
        for ((key, rawVal) in incomingValues) {
            val def = defsByKey[key] ?: continue // Ignore unknown fields or preserve as configured

            if (rawVal == null || (rawVal is String && rawVal.isBlank())) {
                sanitized[key] = null
                continue
            }

            sanitized[key] = validateFieldTypeAndRules(def, rawVal)
        }

        return sanitized
    }

    private fun validateFieldTypeAndRules(
        def: CustomFieldDefinition,
        raw: Any,
    ): Any {
        val rules = def.validationRules

        return when (def.fieldType) {
            FieldType.TEXT -> {
                val str = raw.toString()
                rules?.min?.let { minLen ->
                    if (str.length < minLen.toInt()) {
                        throw InvalidFieldValueException("Field '${def.label}' must have at least ${minLen.toInt()} characters")
                    }
                }
                rules?.max?.let { maxLen ->
                    if (str.length > maxLen.toInt()) {
                        throw InvalidFieldValueException("Field '${def.label}' cannot exceed ${maxLen.toInt()} characters")
                    }
                }
                rules?.regex?.let { reg ->
                    try {
                        val pattern = Pattern.compile(reg)
                        if (!pattern.matcher(str).matches()) {
                            throw InvalidFieldValueException("Field '${def.label}' format does not match required pattern")
                        }
                    } catch (e: PatternSyntaxException) {
                        // ignore malformed regex configuration gracefully
                    }
                }
                str
            }

            FieldType.NUMBER -> {
                val num =
                    when (raw) {
                        is Number -> raw.toLong()
                        is String ->
                            raw.trim().toLongOrNull()
                                ?: throw InvalidFieldValueException("Field '${def.label}' must be an integer number")
                        else -> throw InvalidFieldValueException("Field '${def.label}' must be an integer number")
                    }
                rules?.min?.let { min ->
                    if (num < min.toLong()) {
                        throw InvalidFieldValueException("Field '${def.label}' cannot be less than ${min.toLong()}")
                    }
                }
                rules?.max?.let { max ->
                    if (num > max.toLong()) {
                        throw InvalidFieldValueException("Field '${def.label}' cannot exceed ${max.toLong()}")
                    }
                }
                num
            }

            FieldType.DECIMAL -> {
                val dec =
                    when (raw) {
                        is Number -> raw.toDouble()
                        is String ->
                            raw.trim().replace(',', '.').toDoubleOrNull()
                                ?: throw InvalidFieldValueException("Field '${def.label}' must be a valid decimal number")
                        else -> throw InvalidFieldValueException("Field '${def.label}' must be a valid decimal number")
                    }
                rules?.min?.let { min ->
                    if (dec < min) {
                        throw InvalidFieldValueException("Field '${def.label}' cannot be less than $min")
                    }
                }
                rules?.max?.let { max ->
                    if (dec > max) {
                        throw InvalidFieldValueException("Field '${def.label}' cannot exceed $max")
                    }
                }
                dec
            }

            FieldType.DATE -> {
                val dateStr = raw.toString().trim()
                try {
                    LocalDate.parse(dateStr)
                    dateStr
                } catch (e: DateTimeParseException) {
                    throw InvalidFieldValueException("Field '${def.label}' must be a valid date in ISO-8601 format (YYYY-MM-DD)")
                }
            }

            FieldType.BOOLEAN -> {
                when (raw) {
                    is Boolean -> raw
                    is String ->
                        raw.trim().lowercase().let {
                            if (it == "true" || it == "1" || it == "yes" || it == "sim") {
                                true
                            } else if (it == "false" || it == "0" || it == "no" || it == "nao" || it == "não") {
                                false
                            } else {
                                throw InvalidFieldValueException("Field '${def.label}' must be a boolean (true/false)")
                            }
                        }
                    else -> throw InvalidFieldValueException("Field '${def.label}' must be a boolean (true/false)")
                }
            }

            FieldType.SELECT -> {
                val selected = raw.toString().trim()
                val options = def.options ?: emptyList()
                if (!options.contains(selected)) {
                    throw InvalidFieldValueException(
                        "Value '$selected' is not a valid option for field '${def.label}'. Allowed options: $options",
                    )
                }
                selected
            }
        }
    }
}
