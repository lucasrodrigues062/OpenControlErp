package org.opencontrol.erp.customfields.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.customfields.api.dto.CreateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.api.dto.FieldOrderDto
import org.opencontrol.erp.customfields.api.dto.UpdateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.domain.CustomFieldDefinition
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.exception.CustomFieldNotFoundException
import org.opencontrol.erp.customfields.exception.DuplicateFieldKeyException
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import org.opencontrol.erp.customfields.exception.UnsupportedTargetEntityException
import org.opencontrol.erp.customfields.repository.CustomFieldDefinitionRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import java.util.Optional
import java.util.UUID

class CustomFieldDefinitionServiceTest {
    private val definitionRepository = mockk<CustomFieldDefinitionRepository>()
    private val entityRegistry = CustomFieldEntityRegistry()
    private lateinit var service: CustomFieldDefinitionService

    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "test-user",
                username = "admin",
                roles = setOf("ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_1"),
            ),
        )
        service = CustomFieldDefinitionService(definitionRepository, entityRegistry)
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should create definition successfully`() {
        val request =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "CUSTOMER",
                fieldKey = "spouse_name",
                label = "Spouse Name",
                fieldType = FieldType.TEXT,
                required = true,
                displayOrder = 1,
            )

        every {
            definitionRepository.existsByTenantIdAndTargetEntityAndFieldKey(tenantId, "CUSTOMER", "spouse_name")
        } returns false
        every { definitionRepository.save(any()) } answers { firstArg() }

        val response = service.createDefinition(request)

        assertEquals("spouse_name", response.fieldKey)
        assertEquals("Spouse Name", response.label)
        assertEquals(FieldType.TEXT, response.fieldType)
        assertTrue(response.required)
        assertTrue(response.isActive)
        verify { definitionRepository.save(any()) }
    }

    @Test
    fun `should throw when entity is not registered`() {
        val request =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "UNKNOWN_ENTITY",
                fieldKey = "test_key",
                label = "Test",
                fieldType = FieldType.TEXT,
            )

        assertThrows<UnsupportedTargetEntityException> {
            service.createDefinition(request)
        }
    }

    @Test
    fun `should throw when SELECT field has empty options`() {
        val request =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "CUSTOMER",
                fieldKey = "tier",
                label = "Tier",
                fieldType = FieldType.SELECT,
                options = emptyList(),
            )

        assertThrows<InvalidFieldValueException> {
            service.createDefinition(request)
        }
    }

    @Test
    fun `should throw when duplicate key exists for entity`() {
        val request =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "CUSTOMER",
                fieldKey = "spouse_name",
                label = "Spouse Name",
                fieldType = FieldType.TEXT,
            )

        every {
            definitionRepository.existsByTenantIdAndTargetEntityAndFieldKey(tenantId, "CUSTOMER", "spouse_name")
        } returns true

        assertThrows<DuplicateFieldKeyException> {
            service.createDefinition(request)
        }
    }

    @Test
    fun `should list definitions ordered`() {
        val def =
            CustomFieldDefinition(
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                fieldKey = "key1",
                label = "Label 1",
                fieldType = FieldType.TEXT,
                displayOrder = 1,
            )

        every {
            definitionRepository.findByTenantIdAndTargetEntityAndIsActiveTrueOrderByDisplayOrderAsc(tenantId, "CUSTOMER")
        } returns listOf(def)

        val list = service.listDefinitions("CUSTOMER", activeOnly = true)

        assertEquals(1, list.size)
        assertEquals("key1", list[0].fieldKey)
    }

    @Test
    fun `should get definition by ID`() {
        val defId = UUID.randomUUID()
        val def =
            CustomFieldDefinition(
                id = defId,
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                fieldKey = "key1",
                label = "Label 1",
                fieldType = FieldType.TEXT,
            )

        every { definitionRepository.findByIdAndTenantId(defId, tenantId) } returns Optional.of(def)

        val res = service.getDefinitionById(defId)
        assertEquals(defId, res.id)
    }

    @Test
    fun `should throw when getting non-existent definition`() {
        val defId = UUID.randomUUID()
        every { definitionRepository.findByIdAndTenantId(defId, tenantId) } returns Optional.empty()

        assertThrows<CustomFieldNotFoundException> {
            service.getDefinitionById(defId)
        }
    }

    @Test
    fun `should update definition metadata`() {
        val defId = UUID.randomUUID()
        val def =
            CustomFieldDefinition(
                id = defId,
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                fieldKey = "key1",
                label = "Old Label",
                fieldType = FieldType.TEXT,
            )

        every { definitionRepository.findByIdAndTenantId(defId, tenantId) } returns Optional.of(def)
        every { definitionRepository.save(any()) } answers { firstArg() }

        val updated =
            service.updateDefinition(
                defId,
                UpdateCustomFieldDefinitionRequest(
                    label = "New Label",
                    required = true,
                    displayOrder = 5,
                ),
            )

        assertEquals("New Label", updated.label)
        assertTrue(updated.required)
        assertEquals(5, updated.displayOrder)
    }

    @Test
    fun `should toggle active status`() {
        val defId = UUID.randomUUID()
        val def =
            CustomFieldDefinition(
                id = defId,
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                fieldKey = "key1",
                label = "Label",
                fieldType = FieldType.TEXT,
                isActive = true,
            )

        every { definitionRepository.findByIdAndTenantId(defId, tenantId) } returns Optional.of(def)
        every { definitionRepository.save(any()) } answers { firstArg() }

        val res = service.toggleStatus(defId)
        assertFalse(res.isActive)

        val res2 = service.toggleStatus(defId)
        assertTrue(res2.isActive)
    }

    @Test
    fun `should reorder definitions`() {
        val defId1 = UUID.randomUUID()
        val def1 =
            CustomFieldDefinition(
                id = defId1,
                tenantId = tenantId,
                targetEntity = "CUSTOMER",
                fieldKey = "key1",
                label = "Label 1",
                fieldType = FieldType.TEXT,
                displayOrder = 1,
            )

        every { definitionRepository.findByIdAndTenantId(defId1, tenantId) } returns Optional.of(def1)
        every { definitionRepository.save(any()) } answers { firstArg() }

        service.reorderDefinitions(listOf(FieldOrderDto(defId1, 10)))

        assertEquals(10, def1.displayOrder)
        verify { definitionRepository.save(def1) }
    }
}
