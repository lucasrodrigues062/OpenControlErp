package org.opencontrol.erp.rules.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.rules.api.dto.RuleRequestDto
import org.opencontrol.erp.rules.api.dto.RuleResponseDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationRequestDto
import org.opencontrol.erp.rules.api.dto.RuleSimulationResponseDto
import org.opencontrol.erp.rules.api.dto.RuleSummaryDto
import org.opencontrol.erp.rules.api.dto.RuleTraceResponseDto
import org.opencontrol.erp.rules.service.RuleExecutionService
import org.opencontrol.erp.rules.service.RuleService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Custom Rules", description = "Dynamic Multi-Tenant Custom Business Rules Engine")
class RuleController(
    private val ruleService: RuleService,
    private val executionService: RuleExecutionService,
) {
    private fun resolveTenantId(tenantHeader: UUID?): UUID = tenantHeader ?: SecurityContextHolder.getCurrentTenantId()

    @GetMapping
    @PreAuthorize("hasAuthority('RULES_READ') or hasAuthority('RULES_MANAGE')")
    @Operation(summary = "List all custom rules for current tenant")
    fun listRules(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<RuleSummaryDto>> {
        val tenantId = resolveTenantId(tenantHeader)
        val pageable = PageRequest.of(page, size, Sort.by("priority").ascending())
        val result = ruleService.listRules(tenantId, pageable)
        return ResponseEntity.ok(result)
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RULES_MANAGE')")
    @Operation(summary = "Create a new custom business rule")
    fun createRule(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @Valid @RequestBody request: RuleRequestDto,
    ): ResponseEntity<RuleResponseDto> {
        val tenantId = resolveTenantId(tenantHeader)
        val created = ruleService.createRule(tenantId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RULES_READ') or hasAuthority('RULES_MANAGE')")
    @Operation(summary = "Get custom rule by ID")
    fun getRule(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @PathVariable id: UUID,
    ): ResponseEntity<RuleResponseDto> {
        val tenantId = resolveTenantId(tenantHeader)
        val rule = ruleService.getRule(tenantId, id)
        return ResponseEntity.ok(rule)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('RULES_MANAGE')")
    @Operation(summary = "Update an existing custom rule")
    fun updateRule(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @PathVariable id: UUID,
        @Valid @RequestBody request: RuleRequestDto,
    ): ResponseEntity<RuleResponseDto> {
        val tenantId = resolveTenantId(tenantHeader)
        val updated = ruleService.updateRule(tenantId, id, request)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RULES_MANAGE')")
    @Operation(summary = "Delete a custom rule")
    fun deleteRule(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        val tenantId = resolveTenantId(tenantHeader)
        ruleService.deleteRule(tenantId, id)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('RULES_MANAGE')")
    @Operation(summary = "Toggle rule active status")
    fun toggleRule(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @PathVariable id: UUID,
    ): ResponseEntity<RuleResponseDto> {
        val tenantId = resolveTenantId(tenantHeader)
        val toggled = ruleService.toggleRule(tenantId, id)
        return ResponseEntity.ok(toggled)
    }

    @PostMapping("/test")
    @PreAuthorize("hasAuthority('RULES_READ') or hasAuthority('RULES_MANAGE')")
    @Operation(summary = "Dry-run simulate a custom rule against context payload")
    fun simulateRule(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @RequestBody request: RuleSimulationRequestDto,
    ): ResponseEntity<RuleSimulationResponseDto> {
        val tenantId = resolveTenantId(tenantHeader)
        val result = executionService.simulateRule(tenantId, request)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/traces")
    @PreAuthorize("hasAuthority('RULES_READ') or hasAuthority('RULES_MANAGE')")
    @Operation(summary = "List chronological execution traces for rules")
    fun listTraces(
        @RequestHeader(name = "X-Tenant-Id", required = false) tenantHeader: UUID?,
        @RequestParam(required = false) ruleId: UUID?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<RuleTraceResponseDto>> {
        val tenantId = resolveTenantId(tenantHeader)
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
        val result = executionService.listTraces(tenantId, ruleId, pageable)
        return ResponseEntity.ok(result)
    }
}
