package org.opencontrol.erp.customfields.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "custom_field_definitions")
class CustomFieldDefinition(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "tenant_id", nullable = false)
    val tenantId: UUID,
    @Column(name = "target_entity", nullable = false, length = 64)
    var targetEntity: String,
    @Column(name = "field_key", nullable = false, length = 64)
    var fieldKey: String,
    @Column(nullable = false, length = 128)
    var label: String,
    @Column(length = 255)
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 32)
    var fieldType: FieldType,
    @Column(nullable = false)
    var required: Boolean = false,
    @Column(name = "admin_only", nullable = false)
    var adminOnly: Boolean = false,
    @Column(name = "read_only", nullable = false)
    var readOnly: Boolean = false,
    @Column(name = "default_value", length = 255)
    var defaultValue: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var options: List<String>? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_rules", columnDefinition = "jsonb")
    var validationRules: CustomFieldValidationRules? = null,
    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),
)
