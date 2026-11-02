package org.opencontrol.erp.iam.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.iam.api.dto.CreateUserRequest
import org.opencontrol.erp.iam.api.dto.ProfileSummaryDto
import org.opencontrol.erp.iam.api.dto.UpdateUserRequest
import org.opencontrol.erp.iam.api.dto.UserPageResponse
import org.opencontrol.erp.iam.api.dto.UserResponse
import org.opencontrol.erp.iam.domain.User
import org.opencontrol.erp.iam.domain.UserStatus
import org.opencontrol.erp.iam.service.UserService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/iam/users")
@Tag(name = "Users", description = "User Account Management and Profile Association")
class UserController(
    private val userService: UserService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('USERS_READ') or hasAuthority('USERS_WRITE')")
    @Operation(summary = "List users with pagination, query, and status filtering")
    fun listUsers(
        @RequestParam(required = false) query: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<UserPageResponse> {
        val userStatus = status?.let { UserStatus.valueOf(it.uppercase()) }
        val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())

        val usersPage = userService.searchUsers(query, userStatus, pageable)

        val response =
            UserPageResponse(
                items = usersPage.content.map { toResponse(it) },
                totalElements = usersPage.totalElements,
                totalPages = usersPage.totalPages,
                currentPage = usersPage.number,
            )
        return ResponseEntity.ok(response)
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    @Operation(summary = "Create a new user with single-profile assignment")
    fun createUser(
        @Valid @RequestBody request: CreateUserRequest,
    ): ResponseEntity<UserResponse> {
        val created =
            userService.createUser(
                username = request.username,
                email = request.email,
                passwordRaw = request.password,
                fullName = request.fullName,
                profileId = request.profileId,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_READ') or hasAuthority('USERS_WRITE')")
    @Operation(summary = "Get user details by ID")
    fun getUser(
        @PathVariable id: UUID,
    ): ResponseEntity<UserResponse> {
        val user = userService.getById(id)
        return ResponseEntity.ok(toResponse(user))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USERS_WRITE')")
    @Operation(summary = "Update user details, status, or assigned profile")
    fun updateUser(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUserRequest,
    ): ResponseEntity<UserResponse> {
        val status = UserStatus.valueOf(request.status.uppercase())
        val updated =
            userService.updateUser(
                id = id,
                email = request.email,
                fullName = request.fullName,
                status = status,
                profileId = request.profileId,
            )
        return ResponseEntity.ok(toResponse(updated))
    }

    private fun toResponse(user: User): UserResponse =
        UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            fullName = user.fullName,
            status = user.status.name,
            profile =
                ProfileSummaryDto(
                    id = user.profile.id,
                    name = user.profile.name,
                ),
            permissions =
                user.profile.permissions
                    .map { it.id.permission }
                    .sorted(),
            createdAt = user.createdAt,
        )
}
