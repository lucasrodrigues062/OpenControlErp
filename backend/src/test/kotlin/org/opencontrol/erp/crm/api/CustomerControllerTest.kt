package org.opencontrol.erp.crm.api

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opencontrol.erp.crm.api.dto.CreateCustomerCategoryRequest
import org.opencontrol.erp.crm.api.dto.CreateCustomerRequest
import org.opencontrol.erp.crm.api.dto.CreateEconomicGroupRequest
import org.opencontrol.erp.crm.api.dto.CustomerCategoryResponse
import org.opencontrol.erp.crm.api.dto.CustomerResponse
import org.opencontrol.erp.crm.api.dto.EconomicGroupResponse
import org.opencontrol.erp.crm.api.dto.UpdateCustomerRequest
import org.opencontrol.erp.crm.domain.model.CreditStatus
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.model.IcmsTaxpayerType
import org.opencontrol.erp.crm.service.CustomerCategoryService
import org.opencontrol.erp.crm.service.CustomerService
import org.opencontrol.erp.crm.service.EconomicGroupService
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class CustomerControllerTest {
    private val customerService: CustomerService = mockk()
    private val categoryService: CustomerCategoryService = mockk()
    private val economicGroupService: EconomicGroupService = mockk()

    private lateinit var customerController: CustomerController
    private lateinit var categoryController: CustomerCategoryController
    private lateinit var economicGroupController: EconomicGroupController

    private val sampleCustomer =
        CustomerResponse(
            id = UUID.randomUUID(),
            tenantId = UUID.randomUUID(),
            branchId = null,
            type = CustomerType.LEGAL_ENTITY,
            document = "53066208000170",
            formattedDocument = "53.066.208/0001-70",
            legalName = "Sym Soluções em Tecnologia Ltda",
            tradeName = "Sym Tech",
            stateRegistration = "123456789",
            isStateRegistrationExempt = false,
            municipalRegistration = null,
            icmsTaxpayerType = IcmsTaxpayerType.CONTRIBUTOR,
            mainCnae = null,
            birthOrFoundingDate = null,
            phone1 = "11988887777",
            phone2 = null,
            email = "contato@symtech.com.br",
            contactPerson = "Lucas",
            website = null,
            categoryId = null,
            categoryName = null,
            categoryColorHex = null,
            economicGroupId = null,
            economicGroupName = null,
            creditLimit = BigDecimal("50000.00"),
            creditStatus = CreditStatus.APPROVED,
            notes = null,
            isActive = true,
            postalCode = "01310100",
            street = "Avenida Paulista",
            number = "1000",
            complement = null,
            neighborhood = "Bela Vista",
            cityName = "São Paulo",
            cityIbgeCode = 3550308,
            stateCode = "SP",
            countryCode = "BRA",
            billingPostalCode = null,
            billingStreet = null,
            billingNumber = null,
            billingComplement = null,
            billingNeighborhood = null,
            billingCityName = null,
            billingStateCode = null,
            deliveryPostalCode = null,
            deliveryStreet = null,
            deliveryNumber = null,
            deliveryComplement = null,
            deliveryNeighborhood = null,
            deliveryCityName = null,
            deliveryStateCode = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    @BeforeEach
    fun setUp() {
        customerController = CustomerController(customerService)
        categoryController = CustomerCategoryController(categoryService)
        economicGroupController = EconomicGroupController(economicGroupService)
    }

    @Test
    fun `should list customers with pagination`() {
        val pageable = PageRequest.of(0, 20)
        val page = PageImpl(listOf(sampleCustomer), pageable, 1)

        every {
            customerService.listCustomers(
                query = null,
                type = null,
                categoryId = null,
                economicGroupId = null,
                branchId = null,
                activeOnly = false,
                pageable = any(),
            )
        } returns page

        val response = customerController.list(null, null, null, null, null, false, pageable)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1, response.body?.totalElements)
        assertEquals(
            "Sym Soluções em Tecnologia Ltda",
            response.body
                ?.content
                ?.first()
                ?.legalName,
        )
    }

    @Test
    fun `should get customer by id`() {
        every { customerService.getById(sampleCustomer.id) } returns sampleCustomer

        val response = customerController.getById(sampleCustomer.id)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(sampleCustomer.id, response.body?.id)
    }

    @Test
    fun `should create customer`() {
        val request =
            CreateCustomerRequest(
                type = CustomerType.LEGAL_ENTITY,
                document = "53066208000170",
                legalName = "Sym Soluções em Tecnologia Ltda",
            )

        every { customerService.createCustomer(request) } returns sampleCustomer

        val response = customerController.create(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("53066208000170", response.body?.document)
    }

    @Test
    fun `should update customer`() {
        val request = UpdateCustomerRequest(legalName = "Novo Nome")
        val updated = sampleCustomer.copy(legalName = "Novo Nome")

        every { customerService.updateCustomer(sampleCustomer.id, request) } returns updated

        val response = customerController.update(sampleCustomer.id, request)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("Novo Nome", response.body?.legalName)
    }

    @Test
    fun `should toggle customer status`() {
        val deactivated = sampleCustomer.copy(isActive = false)
        every { customerService.toggleStatus(sampleCustomer.id, false) } returns deactivated

        val response = customerController.toggleStatus(sampleCustomer.id, mapOf("active" to false))

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(false, response.body?.isActive)
    }

    @Test
    fun `should manage customer categories`() {
        val categoryId = UUID.randomUUID()
        val catResp =
            CustomerCategoryResponse(
                id = categoryId,
                code = "ATACADO",
                name = "Vendas Atacado",
                description = null,
                colorHex = "#3B82F6",
                defaultDiscountPct = BigDecimal("5.00"),
                isActive = true,
            )

        every { categoryService.listCategories(false) } returns listOf(catResp)
        every { categoryService.getById(categoryId) } returns catResp

        val listResp = categoryController.list(false)
        assertEquals(HttpStatus.OK, listResp.statusCode)
        assertEquals(1, listResp.body?.size)

        val createReq = CreateCustomerCategoryRequest(code = "ATACADO", name = "Vendas Atacado")
        every { categoryService.createCategory(createReq) } returns catResp

        val createdResp = categoryController.create(createReq)
        assertEquals(HttpStatus.CREATED, createdResp.statusCode)
    }

    @Test
    fun `should manage economic groups`() {
        val groupId = UUID.randomUUID()
        val grpResp =
            EconomicGroupResponse(
                id = groupId,
                code = "GRP-SIM",
                name = "Holding Sim",
                holdingCnpj = "53066208000170",
                consolidatedCreditLimit = BigDecimal("200000.00"),
                isActive = true,
            )

        every { economicGroupService.listGroups(false) } returns listOf(grpResp)
        val listResp = economicGroupController.list(false)
        assertEquals(HttpStatus.OK, listResp.statusCode)
        assertEquals(1, listResp.body?.size)

        val createReq = CreateEconomicGroupRequest(code = "GRP-SIM", name = "Holding Sim")
        every { economicGroupService.createGroup(createReq) } returns grpResp
        val created = economicGroupController.create(createReq)
        assertEquals(HttpStatus.CREATED, created.statusCode)
    }
}
