package org.opencontrol.erp.iam.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class LoginRequest(
    @field:NotBlank(message = "Username or email is required")
    val username: String,
    @field:NotBlank(message = "Password is required")
    val password: String,
)

data class ProfileSummaryDto(
    val id: UUID,
    val name: String,
)

data class UserResponse(
    val id: UUID,
    val username: String,
    val email: String,
    val fullName: String,
    val preferredLanguage: String = "en-US",
    val status: String,
    val profile: ProfileSummaryDto,
    val permissions: List<String>? = null,
    val createdAt: Instant,
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long = 86400,
    val user: UserResponse,
)

data class CreateUserRequest(
    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    val username: String,
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters")
    val password: String,
    @field:NotBlank(message = "Full name is required")
    val fullName: String,
    @field:NotNull(message = "Profile ID is required")
    val profileId: UUID,
)

data class UpdateUserRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email format")
    val email: String,
    @field:NotBlank(message = "Full name is required")
    val fullName: String,
    @field:NotBlank(message = "Status is required")
    val status: String,
    @field:NotNull(message = "Profile ID is required")
    val profileId: UUID,
)

data class UserPreferencesRequest(
    @field:NotBlank(message = "Preferred language is required")
    @field:Pattern(regexp = "^(en-US|pt-BR)$", message = "Supported languages are en-US or pt-BR")
    val preferredLanguage: String,
)

data class UserPreferencesResponse(
    val userId: UUID,
    val preferredLanguage: String,
    val updatedAt: Instant = Instant.now(),
)

data class UserPageResponse(
    val items: List<UserResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
)
