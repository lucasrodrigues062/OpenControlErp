package org.opencontrol.erp.iam.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class CreateProfileRequest(
    @field:NotBlank(message = "Profile name is required")
    @field:Size(max = 100, message = "Profile name cannot exceed 100 characters")
    val name: String,
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String? = null,
    val permissions: List<String> = emptyList(),
)

data class UpdateProfileRequest(
    @field:NotBlank(message = "Profile name is required")
    @field:Size(max = 100, message = "Profile name cannot exceed 100 characters")
    val name: String,
    @field:Size(max = 255, message = "Description cannot exceed 255 characters")
    val description: String? = null,
    val permissions: List<String> = emptyList(),
)

data class ProfileResponse(
    val id: UUID,
    val name: String,
    val description: String?,
    val isSystem: Boolean,
    val permissions: List<String>,
    val userCount: Long = 0,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class PermissionCatalogItem(
    val code: String,
    val category: String,
    val label: String,
    val description: String,
)
