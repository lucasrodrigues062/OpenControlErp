package org.opencontrol.erp.customfields.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.opencontrol.erp.customfields.domain.EntityCustomFields
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.exception.FieldSecurityException
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import org.opencontrol.erp.customfields.repository.CustomFieldDefinitionRepository
import org.opencontrol.erp.customfields.repository.EntityCustomFieldsRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID

class CustomFieldValueServiceTest {
    private val entityCustomFieldsRepository = mockk<EntityCustomFieldsRepository>()
    private val definitionRepository = mockk<CustomFieldDefinitionRepository>()
    private val entityRegistry = CustomFieldEntityRegistry()
    private val validator = CustomFieldValidator()
    private lateinit var service: CustomFieldValueService

    private val tenantId = UUID.randomUUID()
    private val entityId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "user-1",
                username = "admin",
                roles = setOf("ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_1"),
            ),
        )
        service =
            CustomFieldValueService(
                entityCustomFieldsRepository,
                definitionRepository,
                entityRegistry,
                validator,
            )
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should save and return valid custom field values`() {
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

        every {
            definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, "CUSTOMER")
        } returns defs
        every {
            entityCustomFieldsRepository.findByTenantIdAndTargetEntityAndEntityId(tenantId, "CUSTOMER", entityId)
        } returns Optional.empty()
        every { entityCustomFieldsRepository.save(any()) } answers { firstArg() }

        val res = service.saveEntityCustomValues("CUSTOMER", entityId, mapOf("spouse_name" to "Jane Doe"))

        assertEquals("Jane Doe", res.values["spouse_name"])
        verify { entityCustomFieldsRepository.save(any()) }
    }

    @Test
    fun `should strip admin-only fields when non-admin reads`() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "user-2",
                username = "operator",
                roles = setOf("OPERATOR"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_1"),
            ),
        )

        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "public_note",
                    label = "Note",
                    fieldType = FieldType.TEXT,
                    adminOnly = false,
                ),
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "credit_score",
                    label = "Score",
                    fieldType = FieldType.NUMBER,
                    adminOnly = true,
                ),
            )

        val existing =
            EntityCustomFields(
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                entityId = entityId,
                fieldValues = mutableMapOf("public_note" to "Hello", "credit_score" to 750L),
            )

        every {
            definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, "CUSTOMER")
        } returns defs
        every {
            entityCustomFieldsRepository.findByTenantIdAndTargetEntityAndEntityId(tenantId, "CUSTOMER", entityId)
        } returns Optional.of(existing)

        val res = service.getEntityCustomValues("CUSTOMER", entityId)

        assertEquals("Hello", res.values["public_note"])
        assertNull(res.values["credit_score"])
    }

    @Test
    fun `should throw when non-admin modifies admin-only field`() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "user-2",
                username = "operator",
                roles = setOf("OPERATOR"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_1"),
            ),
        )

        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "credit_score",
                    label = "Score",
                    fieldType = FieldType.NUMBER,
                    adminOnly = true,
                ),
            )

        every {
            definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, "CUSTOMER")
        } returns defs
        every {
            entityCustomFieldsRepository.findByTenantIdAndTargetEntityAndEntityId(tenantId, "CUSTOMER", entityId)
        } returns Optional.empty()

        assertThrows<FieldSecurityException> {
            service.saveEntityCustomValues("CUSTOMER", entityId, mapOf("credit_score" to 800))
        }
    }

    @Test
    fun `should throw when updating existing record with modified read-only field`() {
        val defs =
            listOf(
                CustomFieldDefinition(
                    tenantId = tenantId,
                    targetEntity = "CUSTOMER",
                    fieldKey = "branch_code",
                    label = "Branch Code",
                    fieldType = FieldType.TEXT,
                    readOnly = true,
                ),
            )

        val existing =
            EntityCustomFields(
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                entityId = entityId,
                fieldValues = mutableMapOf("branch_code" to "SP-01"),
            )

        every {
            definitionRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, "CUSTOMER")
        } returns defs
        every {
            entityCustomFieldsRepository.findByTenantIdAndTargetEntityAndEntityId(tenantId, "CUSTOMER", entityId)
        } returns Optional.of(existing)

        assertThrows<InvalidFieldValueException> {
            service.saveEntityCustomValues("CUSTOMER", entityId, mapOf("branch_code" to "RJ-02"))
        }
    }
}
