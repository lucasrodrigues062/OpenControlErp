package org.opencontrol.erp.crm.service

import jakarta.persistence.criteria.Predicate
import org.opencontrol.erp.audit.service.AuditService
import org.opencontrol.erp.crm.api.dto.CreateCustomerRequest
import org.opencontrol.erp.crm.api.dto.CustomerResponse
import org.opencontrol.erp.crm.api.dto.UpdateCustomerRequest
import org.opencontrol.erp.crm.domain.events.CustomerCreatedEvent
import org.opencontrol.erp.crm.domain.events.CustomerUpdatedEvent
import org.opencontrol.erp.crm.domain.model.Customer
import org.opencontrol.erp.crm.domain.model.CustomerType
import org.opencontrol.erp.crm.domain.validation.BrazilianDocumentValidator
import org.opencontrol.erp.crm.exception.CustomerCategoryNotFoundException
import org.opencontrol.erp.crm.exception.CustomerNotFoundException
import org.opencontrol.erp.crm.exception.DuplicateCustomerDocumentException
import org.opencontrol.erp.crm.exception.EconomicGroupNotFoundException
import org.opencontrol.erp.crm.exception.InvalidDocumentException
import org.opencontrol.erp.crm.repository.CustomerCategoryRepository
import org.opencontrol.erp.crm.repository.CustomerRepository
import org.opencontrol.erp.crm.repository.EconomicGroupRepository
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
@Transactional
class CustomerService(
    private val customerRepository: CustomerRepository,
    private val categoryRepository: CustomerCategoryRepository,
    private val economicGroupRepository: EconomicGroupRepository,
    private val documentValidator: BrazilianDocumentValidator,
    private val auditService: AuditService,
    private val eventPublisher: ApplicationEventPublisher,
) {
    private fun getCurrentTenantId(): UUID = SecurityContextHolder.getCurrentTenantId()

    @Transactional(readOnly = true)
    fun listCustomers(
        query: String? = null,
        type: CustomerType? = null,
        categoryId: UUID? = null,
        economicGroupId: UUID? = null,
        branchId: UUID? = null,
        activeOnly: Boolean = false,
        pageable: Pageable = Pageable.unpaged(),
    ): Page<CustomerResponse> {
        val tenantId = getCurrentTenantId()
        val spec =
            Specification<Customer> { root, _, cb ->
                val predicates = mutableListOf<Predicate>()
                predicates.add(cb.equal(root.get<UUID>("tenantId"), tenantId))

                if (activeOnly) {
                    predicates.add(cb.isTrue(root.get("isActive")))
                }
                if (type != null) {
                    predicates.add(cb.equal(root.get<CustomerType>("type"), type))
                }
                if (categoryId != null) {
                    predicates.add(cb.equal(root.get<UUID>("categoryId"), categoryId))
                }
                if (economicGroupId != null) {
                    predicates.add(cb.equal(root.get<UUID>("economicGroupId"), economicGroupId))
                }
                if (branchId != null) {
                    predicates.add(
                        cb.or(
                            cb.isNull(root.get<UUID>("branchId")),
                            cb.equal(root.get<UUID>("branchId"), branchId),
                        ),
                    )
                }
                if (!query.isNullOrBlank()) {
                    val cleanQuery = documentValidator.clean(query)
                    val likeQuery = "%${query.trim().lowercase()}%"

                    val docPredicate =
                        if (cleanQuery.isNotEmpty()) {
                            cb.like(root.get("document"), "%$cleanQuery%")
                        } else {
                            null
                        }

                    val namePredicate = cb.like(cb.lower(root.get("legalName")), likeQuery)
                    val tradePredicate = cb.like(cb.lower(root.get("tradeName")), likeQuery)

                    if (docPredicate != null) {
                        predicates.add(cb.or(namePredicate, tradePredicate, docPredicate))
                    } else {
                        predicates.add(cb.or(namePredicate, tradePredicate))
                    }
                }

                cb.and(*predicates.toTypedArray())
            }

        return customerRepository.findAll(spec, pageable).map {
            CustomerResponse.fromEntity(it, documentValidator.format(it.document))
        }
    }

    @Transactional(readOnly = true)
    fun getById(id: UUID): CustomerResponse {
        val tenantId = getCurrentTenantId()
        val customer =
            customerRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomerNotFoundException(id) }
        return CustomerResponse.fromEntity(customer, documentValidator.format(customer.document))
    }

    fun createCustomer(request: CreateCustomerRequest): CustomerResponse {
        val tenantId = getCurrentTenantId()
        val cleanDoc = documentValidator.clean(request.document)

        val isIndividual = request.type == CustomerType.INDIVIDUAL
        if (!documentValidator.isValid(cleanDoc, isIndividual)) {
            val docType = if (isIndividual) "CPF" else "CNPJ"
            throw InvalidDocumentException("Invalid $docType check digits (Modulo 11): ${request.document}")
        }

        if (customerRepository.existsByTenantIdAndDocument(tenantId, cleanDoc)) {
            throw DuplicateCustomerDocumentException(cleanDoc)
        }

        if (request.categoryId != null) {
            categoryRepository
                .findByIdAndTenantId(request.categoryId, tenantId)
                .orElseThrow { CustomerCategoryNotFoundException(request.categoryId) }
        }

        if (request.economicGroupId != null) {
            economicGroupRepository
                .findByIdAndTenantId(request.economicGroupId, tenantId)
                .orElseThrow { EconomicGroupNotFoundException(request.economicGroupId) }
        }

        val customer =
            Customer(
                tenantId = tenantId,
                branchId = request.branchId,
                type = request.type,
                document = cleanDoc,
                legalName = request.legalName.trim(),
                tradeName = request.tradeName?.trim(),
                stateRegistration = request.stateRegistration?.trim(),
                isStateRegistrationExempt = request.isStateRegistrationExempt,
                municipalRegistration = request.municipalRegistration?.trim(),
                icmsTaxpayerType = request.icmsTaxpayerType,
                mainCnae = request.mainCnae?.trim(),
                birthOrFoundingDate = request.birthOrFoundingDate,
                phone1 = request.phone1?.trim(),
                phone2 = request.phone2?.trim(),
                email = request.email?.trim()?.lowercase(),
                contactPerson = request.contactPerson?.trim(),
                website = request.website?.trim(),
                categoryId = request.categoryId,
                economicGroupId = request.economicGroupId,
                creditLimit = request.creditLimit,
                creditStatus = request.creditStatus,
                notes = request.notes?.trim(),
                isActive = true,
                postalCode = documentValidator.clean(request.postalCode),
                street = request.street?.trim(),
                number = request.number?.trim(),
                complement = request.complement?.trim(),
                neighborhood = request.neighborhood?.trim(),
                cityName = request.cityName?.trim(),
                cityIbgeCode = request.cityIbgeCode,
                stateCode = request.stateCode?.trim()?.uppercase(),
                billingPostalCode = documentValidator.clean(request.billingPostalCode),
                billingStreet = request.billingStreet?.trim(),
                billingNumber = request.billingNumber?.trim(),
                billingComplement = request.billingComplement?.trim(),
                billingNeighborhood = request.billingNeighborhood?.trim(),
                billingCityName = request.billingCityName?.trim(),
                billingStateCode = request.billingStateCode?.trim()?.uppercase(),
                deliveryPostalCode = documentValidator.clean(request.deliveryPostalCode),
                deliveryStreet = request.deliveryStreet?.trim(),
                deliveryNumber = request.deliveryNumber?.trim(),
                deliveryComplement = request.deliveryComplement?.trim(),
                deliveryNeighborhood = request.deliveryNeighborhood?.trim(),
                deliveryCityName = request.deliveryCityName?.trim(),
                deliveryStateCode = request.deliveryStateCode?.trim()?.uppercase(),
            )

        val saved = customerRepository.save(customer)

        auditService.recordAudit(
            entityType = "Customer",
            entityId = saved.id.toString(),
            action = "CREATE",
            details = "Created customer ${saved.legalName} (${documentValidator.format(saved.document)})",
            stateAfter = saved,
        )

        eventPublisher.publishEvent(
            CustomerCreatedEvent(
                customerId = saved.id,
                tenantId = tenantId,
                document = saved.document,
                legalName = saved.legalName,
            ),
        )

        return CustomerResponse.fromEntity(saved, documentValidator.format(saved.document))
    }

    fun updateCustomer(
        id: UUID,
        request: UpdateCustomerRequest,
    ): CustomerResponse {
        val tenantId = getCurrentTenantId()
        val customer =
            customerRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomerNotFoundException(id) }

        val beforeState = CustomerResponse.fromEntity(customer, documentValidator.format(customer.document))

        if (request.categoryId != null) {
            categoryRepository
                .findByIdAndTenantId(request.categoryId, tenantId)
                .orElseThrow { CustomerCategoryNotFoundException(request.categoryId) }
        }

        if (request.economicGroupId != null) {
            economicGroupRepository
                .findByIdAndTenantId(request.economicGroupId, tenantId)
                .orElseThrow { EconomicGroupNotFoundException(request.economicGroupId) }
        }

        customer.legalName = request.legalName.trim()
        customer.tradeName = request.tradeName?.trim()
        customer.stateRegistration = request.stateRegistration?.trim()
        customer.isStateRegistrationExempt = request.isStateRegistrationExempt
        customer.municipalRegistration = request.municipalRegistration?.trim()
        customer.icmsTaxpayerType = request.icmsTaxpayerType
        customer.mainCnae = request.mainCnae?.trim()
        customer.birthOrFoundingDate = request.birthOrFoundingDate
        customer.phone1 = request.phone1?.trim()
        customer.phone2 = request.phone2?.trim()
        customer.email = request.email?.trim()?.lowercase()
        customer.contactPerson = request.contactPerson?.trim()
        customer.website = request.website?.trim()
        customer.categoryId = request.categoryId
        customer.economicGroupId = request.economicGroupId
        if (request.creditLimit != null) customer.creditLimit = request.creditLimit
        if (request.creditStatus != null) customer.creditStatus = request.creditStatus
        customer.notes = request.notes?.trim()
        if (request.isActive != null) customer.isActive = request.isActive

        // Primary
        customer.postalCode = documentValidator.clean(request.postalCode)
        customer.street = request.street?.trim()
        customer.number = request.number?.trim()
        customer.complement = request.complement?.trim()
        customer.neighborhood = request.neighborhood?.trim()
        customer.cityName = request.cityName?.trim()
        customer.cityIbgeCode = request.cityIbgeCode
        customer.stateCode = request.stateCode?.trim()?.uppercase()

        // Billing
        customer.billingPostalCode = documentValidator.clean(request.billingPostalCode)
        customer.billingStreet = request.billingStreet?.trim()
        customer.billingNumber = request.billingNumber?.trim()
        customer.billingComplement = request.billingComplement?.trim()
        customer.billingNeighborhood = request.billingNeighborhood?.trim()
        customer.billingCityName = request.billingCityName?.trim()
        customer.billingStateCode = request.billingStateCode?.trim()?.uppercase()

        // Delivery
        customer.deliveryPostalCode = documentValidator.clean(request.deliveryPostalCode)
        customer.deliveryStreet = request.deliveryStreet?.trim()
        customer.deliveryNumber = request.deliveryNumber?.trim()
        customer.deliveryComplement = request.deliveryComplement?.trim()
        customer.deliveryNeighborhood = request.deliveryNeighborhood?.trim()
        customer.deliveryCityName = request.deliveryCityName?.trim()
        customer.deliveryStateCode = request.deliveryStateCode?.trim()?.uppercase()

        customer.updatedAt = Instant.now()
        val saved = customerRepository.save(customer)

        auditService.recordAudit(
            entityType = "Customer",
            entityId = saved.id.toString(),
            action = "UPDATE",
            details = "Updated customer ${saved.legalName}",
            stateBefore = beforeState,
            stateAfter = saved,
        )

        eventPublisher.publishEvent(
            CustomerUpdatedEvent(
                customerId = saved.id,
                tenantId = tenantId,
                legalName = saved.legalName,
            ),
        )

        return CustomerResponse.fromEntity(saved, documentValidator.format(saved.document))
    }

    fun toggleStatus(
        id: UUID,
        active: Boolean,
    ): CustomerResponse {
        val tenantId = getCurrentTenantId()
        val customer =
            customerRepository
                .findByIdAndTenantId(id, tenantId)
                .orElseThrow { CustomerNotFoundException(id) }

        customer.isActive = active
        customer.updatedAt = Instant.now()
        val saved = customerRepository.save(customer)

        auditService.recordAudit(
            entityType = "Customer",
            entityId = id.toString(),
            action = if (active) "ACTIVATE" else "DEACTIVATE",
            details = "Set customer ${customer.legalName} active = $active",
        )

        return CustomerResponse.fromEntity(saved, documentValidator.format(saved.document))
    }
}
