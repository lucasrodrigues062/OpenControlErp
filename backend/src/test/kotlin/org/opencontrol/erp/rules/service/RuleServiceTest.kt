package org.opencontrol.erp.rules.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.OpenControlException
import org.opencontrol.erp.rules.api.dto.RuleRequestDto
import org.opencontrol.erp.rules.domain.AuthoringMode
import org.opencontrol.erp.rules.domain.CustomRule
import org.opencontrol.erp.rules.domain.ErrorHandlingPolicy
import org.opencontrol.erp.rules.domain.RuleActionType
import org.opencontrol.erp.rules.domain.RuleTiming
import org.opencontrol.erp.rules.repository.CustomRuleRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RuleServiceTest {
    private val ruleRepository: CustomRuleRepository = mockk(relaxed = true)
    private lateinit var service: RuleService
    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service = RuleService(ruleRepository)
    }

    @Test
    fun `should create custom rule when name is unique`() {
        val request =
            RuleRequestDto(
                name = "Wholesale Rule",
                description = "Validates discount limits",
                triggerEvent = "sale.item_added",
                timing = RuleTiming.BEFORE,
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = "#item.discount > 15.0",
                actionType = RuleActionType.BLOCK_OPERATION,
                actionPayload = """{"message": "Discount exceeded"}""",
                priority = 50,
                isActive = true,
                errorHandlingPolicy = ErrorHandlingPolicy.ABORT_TRANSACTION,
            )

        every { ruleRepository.existsByTenantIdAndName(tenantId, "Wholesale Rule") } returns false
        every { ruleRepository.save(any<CustomRule>()) } answers { firstArg() }

        val created = service.createRule(tenantId, request)

        assertNotNull(created.id)
        assertEquals("Wholesale Rule", created.name)
        assertEquals(RuleTiming.BEFORE, created.timing)
        assertEquals(50, created.priority)
        assertTrue(created.isActive)

        verify { ruleRepository.save(any<CustomRule>()) }
    }

    @Test
    fun `should throw exception when creating rule with duplicate name in tenant`() {
        val request =
            RuleRequestDto(
                name = "Duplicate Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        every { ruleRepository.existsByTenantIdAndName(tenantId, "Duplicate Rule") } returns true

        assertThrows<OpenControlException> {
            service.createRule(tenantId, request)
        }
    }

    @Test
    fun `should update existing rule`() {
        val ruleId = UUID.randomUUID()
        val existing =
            CustomRule(
                id = ruleId,
                tenantId = tenantId,
                name = "Old Rule Name",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        val updateRequest =
            RuleRequestDto(
                name = "New Rule Name",
                description = "Updated description",
                triggerEvent = "sale.item_added",
                timing = RuleTiming.AFTER,
                authoringMode = AuthoringMode.ADVANCED_DSL,
                conditionExpression = "#sale.totalAmount > 100",
                actionType = RuleActionType.DISPATCH_NOTIFICATION,
                priority = 20,
                isActive = true,
            )

        every { ruleRepository.findByTenantIdAndId(tenantId, ruleId) } returns Optional.of(existing)
        every { ruleRepository.existsByTenantIdAndNameAndIdNot(tenantId, "New Rule Name", ruleId) } returns false
        every { ruleRepository.save(any<CustomRule>()) } answers { firstArg() }

        val updated = service.updateRule(tenantId, ruleId, updateRequest)

        assertEquals("New Rule Name", updated.name)
        assertEquals("sale.item_added", updated.triggerEvent)
        assertEquals(RuleTiming.AFTER, updated.timing)
        assertEquals(RuleActionType.DISPATCH_NOTIFICATION, updated.actionType)
    }

    @Test
    fun `should throw exception when updating non-existent rule`() {
        val ruleId = UUID.randomUUID()
        val request =
            RuleRequestDto(
                name = "Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        every { ruleRepository.findByTenantIdAndId(tenantId, ruleId) } returns Optional.empty()

        assertThrows<EntityNotFoundException> {
            service.updateRule(tenantId, ruleId, request)
        }
    }

    @Test
    fun `should get rule by id`() {
        val ruleId = UUID.randomUUID()
        val existing =
            CustomRule(
                id = ruleId,
                tenantId = tenantId,
                name = "Existing Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        every { ruleRepository.findByTenantIdAndId(tenantId, ruleId) } returns Optional.of(existing)

        val result = service.getRule(tenantId, ruleId)
        assertEquals("Existing Rule", result.name)
    }

    @Test
    fun `should list rules with pagination`() {
        val pageable = PageRequest.of(0, 10)
        val rule =
            CustomRule(
                tenantId = tenantId,
                name = "List Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        every { ruleRepository.findAllByTenantId(tenantId, pageable) } returns PageImpl(listOf(rule), pageable, 1)

        val page = service.listRules(tenantId, pageable)
        assertEquals(1, page.totalElements)
        assertEquals("List Rule", page.content.first().name)
    }

    @Test
    fun `should toggle rule active state`() {
        val ruleId = UUID.randomUUID()
        val existing =
            CustomRule(
                id = ruleId,
                tenantId = tenantId,
                name = "Toggle Rule",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
                isActive = true,
            )

        every { ruleRepository.findByTenantIdAndId(tenantId, ruleId) } returns Optional.of(existing)
        every { ruleRepository.save(any<CustomRule>()) } answers { firstArg() }

        val toggled = service.toggleRule(tenantId, ruleId)
        assertFalse(toggled.isActive)

        val toggledAgain = service.toggleRule(tenantId, ruleId)
        assertTrue(toggledAgain.isActive)
    }

    @Test
    fun `should delete rule by id`() {
        val ruleId = UUID.randomUUID()
        val existing =
            CustomRule(
                id = ruleId,
                tenantId = tenantId,
                name = "To Delete",
                triggerEvent = "sale.finalized",
                conditionExpression = "true",
                actionType = RuleActionType.BLOCK_OPERATION,
            )

        every { ruleRepository.findByTenantIdAndId(tenantId, ruleId) } returns Optional.of(existing)

        service.deleteRule(tenantId, ruleId)
        verify { ruleRepository.delete(existing) }
    }
}
