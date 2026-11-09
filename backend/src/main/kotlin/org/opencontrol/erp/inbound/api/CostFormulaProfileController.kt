package org.opencontrol.erp.inbound.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.inbound.api.dto.CostFormulaProfileResponse
import org.opencontrol.erp.inbound.api.dto.CreateCostFormulaProfileRequest
import org.opencontrol.erp.inbound.api.dto.EvaluateFormulaTestRequest
import org.opencontrol.erp.inbound.api.dto.EvaluateFormulaTestResponse
import org.opencontrol.erp.inbound.service.CostFormulaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inbound/cost-formula-profiles")
@Tag(name = "Inbound - Cost Formulas", description = "Extensible Excel-like landed cost formula calculation engine and profiles")
class CostFormulaProfileController(
    private val service: CostFormulaService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "List cost formula profiles")
    fun list(): ResponseEntity<List<CostFormulaProfileResponse>> =
        ResponseEntity.ok(service.listProfiles())

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Get cost formula profile by ID")
    fun getById(@PathVariable id: UUID): ResponseEntity<CostFormulaProfileResponse> =
        ResponseEntity.ok(service.getProfile(id))

    @PostMapping
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Create a new cost formula profile")
    fun create(
        @Valid @RequestBody request: CreateCostFormulaProfileRequest,
    ): ResponseEntity<CostFormulaProfileResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createProfile(request))

    @PutMapping("/{id}/default")
    @PreAuthorize("hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Set cost formula profile as default")
    fun setDefault(
        @PathVariable id: UUID,
    ): ResponseEntity<CostFormulaProfileResponse> =
        ResponseEntity.ok(service.setDefault(id))

    @PostMapping("/evaluate-test")
    @PreAuthorize("hasAuthority('INBOUND_READ') or hasAuthority('INBOUND_WRITE')")
    @Operation(summary = "Test evaluate formula with simulated variables")
    fun evaluateTest(
        @Valid @RequestBody request: EvaluateFormulaTestRequest,
    ): ResponseEntity<EvaluateFormulaTestResponse> =
        ResponseEntity.ok(service.testEvaluate(request))
}
