package org.opencontrol.erp.customfields.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.customfields.api.dto.CreateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.api.dto.CustomFieldDefinitionResponse
import org.opencontrol.erp.customfields.api.dto.EntityCustomFieldsResponse
import org.opencontrol.erp.customfields.api.dto.FieldOrderDto
import org.opencontrol.erp.customfields.api.dto.SaveEntityCustomFieldsRequest
import org.opencontrol.erp.customfields.api.dto.UpdateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.service.CustomFieldDefinitionService
import org.opencontrol.erp.customfields.service.CustomFieldEntityRegistry
import org.opencontrol.erp.customfields.service.CustomFieldValueService
import org.opencontrol.erp.customfields.service.TargetEntityDescriptor
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals

class CustomFieldControllerTest {
    private val definitionService = mockk<CustomFieldDefinitionService>()
    private val valueService = mockk<CustomFieldValueService>()
    private val entityRegistry = mockk<CustomFieldEntityRegistry>()

    private lateinit var controller: CustomFieldController

    private val sampleResponse =
        CustomFieldDefinitionResponse(
            id = UUID.randomUUID(),
            tenantId = UUID.randomUUID(),
            targetEntity = "CUSTOMER",
            fieldKey = "spouse_name",
            label = "Spouse Name",
            description = "Spouse name description",
            fieldType = FieldType.TEXT,
            required = true,
            adminOnly = false,
            readOnly = false,
            defaultValue = null,
            options = null,
            validationRules = null,
            displayOrder = 1,
            isActive = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        controller = CustomFieldController(definitionService, valueService, entityRegistry)
    }

    @Test
    fun `should list registered entities`() {
        val descriptors =
            listOf(
                TargetEntityDescriptor("CUSTOMER", "Customer", "CRM", "Customer entity"),
            )
        every { entityRegistry.listEntities() } returns descriptors

        val response = controller.listEntities()
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals("CUSTOMER", response.body?.get(0)?.entityCode)
    }

    @Test
    fun `should list definitions for an entity`() {
        every { definitionService.listDefinitions("CUSTOMER", true) } returns listOf(sampleResponse)

        val response = controller.listDefinitions("CUSTOMER", true)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.size)
        assertEquals("spouse_name", response.body?.get(0)?.fieldKey)
    }

    @Test
    fun `should get definition by ID`() {
        every { definitionService.getDefinitionById(sampleResponse.id) } returns sampleResponse

        val response = controller.getDefinitionById(sampleResponse.id)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(sampleResponse.id, response.body?.id)
    }

    @Test
    fun `should create definition`() {
        val req =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "CUSTOMER",
                fieldKey = "spouse_name",
                label = "Spouse Name",
                fieldType = FieldType.TEXT,
                required = true,
            )
        every { definitionService.createDefinition(req) } returns sampleResponse

        val response = controller.createDefinition(req)
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("spouse_name", response.body?.fieldKey)
    }

    @Test
    fun `should update definition`() {
        val updateReq = UpdateCustomFieldDefinitionRequest(label = "New Label")
        val updated = sampleResponse.copy(label = "New Label")
        every { definitionService.updateDefinition(sampleResponse.id, updateReq) } returns updated

        val response = controller.updateDefinition(sampleResponse.id, updateReq)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("New Label", response.body?.label)
    }

    @Test
    fun `should toggle definition status`() {
        val toggled = sampleResponse.copy(isActive = false)
        every { definitionService.toggleStatus(sampleResponse.id) } returns toggled

        val response = controller.toggleStatus(sampleResponse.id)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(false, response.body?.isActive)
    }

    @Test
    fun `should reorder definitions`() {
        val orders = listOf(FieldOrderDto(sampleResponse.id, 5))
        every { definitionService.reorderDefinitions(orders) } returns Unit

        val response = controller.reorderDefinitions(orders)
        assertEquals(HttpStatus.OK, response.statusCode)
        verify { definitionService.reorderDefinitions(orders) }
    }

    @Test
    fun `should get entity custom values`() {
        val entityId = UUID.randomUUID()
        val valuesResponse =
            EntityCustomFieldsResponse(
                targetEntity = "CUSTOMER",
                entityId = entityId,
                values = mapOf("spouse_name" to "Jane Doe"),
            )
        every { valueService.getEntityCustomValues("CUSTOMER", entityId) } returns valuesResponse

        val response = controller.getEntityCustomValues("CUSTOMER", entityId)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Jane Doe", response.body?.values?.get("spouse_name"))
    }

    @Test
    fun `should save entity custom values`() {
        val entityId = UUID.randomUUID()
        val saveReq = SaveEntityCustomFieldsRequest(values = mapOf("spouse_name" to "Jane Doe"))
        val valuesResponse =
            EntityCustomFieldsResponse(
                targetEntity = "CUSTOMER",
                entityId = entityId,
                values = mapOf("spouse_name" to "Jane Doe"),
            )
        every { valueService.saveEntityCustomValues("CUSTOMER", entityId, saveReq.values) } returns valuesResponse

        val response = controller.saveEntityCustomValues("CUSTOMER", entityId, saveReq)
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Jane Doe", response.body?.values?.get("spouse_name"))
    }
}
