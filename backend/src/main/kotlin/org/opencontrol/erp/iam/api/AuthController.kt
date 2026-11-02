package org.opencontrol.erp.iam.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.iam.api.dto.LoginRequest
import org.opencontrol.erp.iam.api.dto.LoginResponse
import org.opencontrol.erp.iam.service.AuthenticationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User Authentication and Session Management")
class AuthController(
    private val authenticationService: AuthenticationService,
) {
    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials and issue JWT")
    fun login(
        @Valid @RequestBody request: LoginRequest,
    ): ResponseEntity<LoginResponse> {
        val response = authenticationService.login(request)
        return ResponseEntity.ok(response)
    }
}
