package org.opencontrol.erp.crm.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateCustomerRequest
import org.opencontrol.erp.crm.api.dto.UpdateCustomerRequest
import org.opencontrol.erp.crm.domain.events.CustomerCreatedEvent
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.validation.BrazilianDocumentValidator
import org.opencontrol.erp.crm.exception.CustomerNotFoundException
import org.opencontrol.erp.crm.exception.DuplicateCustomerDocumentException
import org.opencontrol.erp.crm.exception.InvalidDocumentException
import org.opencontrol.erp.crm.repository.CustomerCategoryRepository
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.crm.repository.EconomicGroupRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.context.ApplicationEventPublisher
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class CustomerServiceTest {
    private val customerRepository: CustomerRepository = mockk()
    private val categoryRepository: CustomerCategoryRepository = mockk()
    private val economicGroupRepository: EconomicGroupRepository = mockk()
    private val documentValidator = BrazilianDocumentValidator()
    private val auditService: AuditService = mockk(relaxed = true)
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)

    private lateinit var customerService: CustomerService

    private val tenantId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.setContext(
            UserContext(
                userId = UUID.randomUUID().toString(),
                username = "admin",
                roles = setOf("CRM_ADMIN"),
                tenant = TenantContext(tenantId = tenantId, tenantCode = "TENANT_TEST"),
            ),
        )
        customerService =
            CustomerService(
                customerRepository = customerRepository,
                categoryRepository = categoryRepository,
                economicGroupRepository = economicGroupRepository,
                documentValidator = documentValidator,
                auditService = auditService,
                eventPublisher = eventPublisher,
            )
    }

    @Test
    fun `should create legal entity customer with valid CNPJ successfully`() {
        val request =
            CreateCustomerRequest(
                type = CustomerType.LEGAL_ENTITY,
                document = "53066208000170",
                legalName = "Sym Soluções em Tecnologia Ltda",
                tradeName = "Sym Tech",
                creditLimit = BigDecimal("50000.00"),
            )

        every { customerRepository.existsByTenantIdAndDocument(tenantId, "53066208000170") } returns false
        every { customerRepository.save(any()) } answers { firstArg() }

        val response = customerService.createCustomer(request)

        assertNotNull(response)
        assertEquals("53066208000170", response.document)
        assertEquals("53.066.208/0001-70", response.formattedDocument)
        assertEquals("Sym Soluções em Tecnologia Ltda", response.legalName)
        verify { eventPublisher.publishEvent(any<CustomerCreatedEvent>()) }
    }

    @Test
    fun `should reject customer creation when CNPJ has invalid Modulo 11 check digits`() {
        val request =
            CreateCustomerRequest(
                type = CustomerType.LEGAL_ENTITY,
                document = "11111111111111",
                legalName = "Empresa Invalida",
            )

        assertThrows<InvalidDocumentException> {
            customerService.createCustomer(request)
        }
    }

    @Test
    fun `should reject duplicate customer document in same tenant`() {
        val request =
            CreateCustomerRequest(
                type = CustomerType.LEGAL_ENTITY,
                document = "53066208000170",
                legalName = "Empresa Duplicada",
            )

        every { customerRepository.existsByTenantIdAndDocument(tenantId, "53066208000170") } returns true

        assertThrows<DuplicateCustomerDocumentException> {
            customerService.createCustomer(request)
        }
    }

    @Test
    fun `should create individual customer with valid CPF successfully`() {
        val request =
            CreateCustomerRequest(
                type = CustomerType.INDIVIDUAL,
                document = "52998224725",
                legalName = "Lucas Souza",
                creditLimit = BigDecimal("5000.00"),
            )

        every { customerRepository.existsByTenantIdAndDocument(tenantId, "52998224725") } returns false
        every { customerRepository.save(any()) } answers { firstArg() }

        val response = customerService.createCustomer(request)

        assertNotNull(response)
        assertEquals("52998224725", response.document)
        assertEquals("529.982.247-25", response.formattedDocument)
        assertEquals("Lucas Souza", response.legalName)
    }

    @Test
    fun `should update existing customer details`() {
        val customerId = UUID.randomUUID()
        val existing =
            Customer(
                id = customerId,
                tenantId = tenantId,
                document = "53066208000170",
                legalName = "Nome Antigo",
            )

        every { customerRepository.findByIdAndTenantId(customerId, tenantId) } returns Optional.of(existing)
        every { customerRepository.save(any()) } answers { firstArg() }

        val updateRequest =
            UpdateCustomerRequest(
                legalName = "Nome Atualizado",
                tradeName = "Nome Fantasia Novo",
                creditLimit = BigDecimal("100000.00"),
            )

        val updated = customerService.updateCustomer(customerId, updateRequest)

        assertEquals("Nome Atualizado", updated.legalName)
        assertEquals("Nome Fantasia Novo", updated.tradeName)
        assertEquals(BigDecimal("100000.00"), updated.creditLimit)
    }

    @Test
    fun `should throw when updating nonexistent customer`() {
        val customerId = UUID.randomUUID()
        every { customerRepository.findByIdAndTenantId(customerId, tenantId) } returns Optional.empty()

        assertThrows<CustomerNotFoundException> {
            customerService.updateCustomer(customerId, UpdateCustomerRequest(legalName = "Qualquer"))
        }
    }

    @Test
    fun `should get customer by id`() {
        val customerId = UUID.randomUUID()
        val customer =
            Customer(
                id = customerId,
                tenantId = tenantId,
                document = "53066208000170",
                legalName = "Cliente Teste",
            )
        every { customerRepository.findByIdAndTenantId(customerId, tenantId) } returns Optional.of(customer)

        val response = customerService.getById(customerId)
        assertEquals("Cliente Teste", response.legalName)
        assertEquals("53.066.208/0001-70", response.formattedDocument)
    }

    @Test
    fun `should throw when getting nonexistent customer`() {
        val customerId = UUID.randomUUID()
        every { customerRepository.findByIdAndTenantId(customerId, tenantId) } returns Optional.empty()

        assertThrows<CustomerNotFoundException> {
            customerService.getById(customerId)
        }
    }

    @Test
    fun `should toggle customer status`() {
        val customerId = UUID.randomUUID()
        val customer =
            Customer(
                id = customerId,
                tenantId = tenantId,
                document = "53066208000170",
                legalName = "Cliente Ativo",
                isActive = true,
            )
        every { customerRepository.findByIdAndTenantId(customerId, tenantId) } returns Optional.of(customer)
        every { customerRepository.save(any()) } answers { firstArg() }

        val response = customerService.toggleStatus(customerId, false)
        org.junit.jupiter.api.Assertions
            .assertFalse(response.isActive)
    }
}
