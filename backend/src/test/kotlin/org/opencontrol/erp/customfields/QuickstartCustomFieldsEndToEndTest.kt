package org.opencontrol.erp.customfields

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.customfields.api.CustomFieldController
import org.opencontrol.erp.customfields.api.dto.CreateCustomFieldDefinitionRequest
import org.opencontrol.erp.customfields.api.dto.SaveEntityCustomFieldsRequest
import org.opencontrol.erp.customfields.domain.FieldType
import org.opencontrol.erp.customfields.exception.InvalidFieldValueException
import org.opencontrol.erp.customfields.repository.CustomFieldDefinitionRepository
import org.opencontrol.erp.customfields.repository.EntityCustomFieldsRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@WithMockUser(authorities = ["CUSTOM_FIELDS_READ", "CUSTOM_FIELDS_MANAGE"])
class QuickstartCustomFieldsEndToEndTest {
    @Autowired
    private lateinit var controller: CustomFieldController

    @Autowired
    private lateinit var definitionRepository: CustomFieldDefinitionRepository

    @Autowired
    private lateinit var entityCustomFieldsRepository: EntityCustomFieldsRepository

    private val tenantId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = "admin-1",
                username = "admin",
                roles = setOf("ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_E2E"),
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        entityCustomFieldsRepository.deleteAll()
        definitionRepository.deleteAll()
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `quickstart scenarios - full lifecycle verification`() {
        // --- Scenario 1: Define Custom Fields for Customer Entity ---
        val spouseReq =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "CUSTOMER",
                fieldKey = "spouse_name",
                label = "Spouse Name",
                fieldType = FieldType.TEXT,
                required = true,
                displayOrder = 1,
            )

        val spouseResp = controller.createDefinition(spouseReq)
        assertEquals(HttpStatus.CREATED, spouseResp.statusCode)
        assertEquals("spouse_name", spouseResp.body?.fieldKey)
        assertTrue(spouseResp.body?.required == true)

        val tierReq =
            CreateCustomFieldDefinitionRequest(
                targetEntity = "CUSTOMER",
                fieldKey = "customer_tier",
                label = "Customer Tier",
                fieldType = FieldType.SELECT,
                required = false,
                options = listOf("BRONZE", "SILVER", "GOLD", "PLATINUM"),
                displayOrder = 2,
            )

        val tierResp = controller.createDefinition(tierReq)
        assertEquals(HttpStatus.CREATED, tierResp.statusCode)
        assertEquals("customer_tier", tierResp.body?.fieldKey)

        // Verify list returns 2 definitions ordered
        val listResp = controller.listDefinitions("CUSTOMER", true)
        assertEquals(HttpStatus.OK, listResp.statusCode)
        assertEquals(2, listResp.body?.size)
        assertEquals("spouse_name", listResp.body?.get(0)?.fieldKey)
        assertEquals("customer_tier", listResp.body?.get(1)?.fieldKey)

        // --- Scenario 2: Save and Validate Custom Field Values on Entity Record ---
        val customerId = UUID.randomUUID()

        // 1. Invalid Option Test ("DIAMOND" is not in options)
        val invalidOptionPayload =
            SaveEntityCustomFieldsRequest(
                values =
                    mapOf(
                        "spouse_name" to "Mary Jane",
                        "customer_tier" to "DIAMOND",
                    ),
            )
        assertThrows<InvalidFieldValueException> {
            controller.saveEntityCustomValues("CUSTOMER", customerId, invalidOptionPayload)
        }

        // 2. Missing Required Field Test (omitting spouse_name)
        val missingRequiredPayload =
            SaveEntityCustomFieldsRequest(
                values = mapOf("customer_tier" to "GOLD"),
            )
        assertThrows<InvalidFieldValueException> {
            controller.saveEntityCustomValues("CUSTOMER", customerId, missingRequiredPayload)
        }

        // 3. Valid Payload
        val validPayload =
            SaveEntityCustomFieldsRequest(
                values =
                    mapOf(
                        "spouse_name" to "Mary Jane",
                        "customer_tier" to "GOLD",
                    ),
            )
        val saveResp = controller.saveEntityCustomValues("CUSTOMER", customerId, validPayload)
        assertEquals(HttpStatus.OK, saveResp.statusCode)
        assertEquals("Mary Jane", saveResp.body?.values?.get("spouse_name"))
        assertEquals("GOLD", saveResp.body?.values?.get("customer_tier"))

        // 4. Retrieve Values
        val getResp = controller.getEntityCustomValues("CUSTOMER", customerId)
        assertEquals(HttpStatus.OK, getResp.statusCode)
        assertEquals("Mary Jane", getResp.body?.values?.get("spouse_name"))
        assertEquals("GOLD", getResp.body?.values?.get("customer_tier"))
    }
}
