package org.opencontrol.erp.iam.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.opencontrol.erp.iam.api.dto.CreateProfileRequest
import org.opencontrol.erp.iam.api.dto.PermissionCatalogItem
import org.opencontrol.erp.iam.api.dto.ProfileResponse
import org.opencontrol.erp.iam.api.dto.UpdateProfileRequest
import org.opencontrol.erp.iam.repository.UserRepository
import org.opencontrol.erp.iam.service.ProfileService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/iam")
@Tag(name = "Profiles", description = "Access Profile and Permission Management")
class ProfileController(
    private val profileService: ProfileService,
    private val userRepository: UserRepository,
) {
    @GetMapping("/profiles")
    @PreAuthorize("hasAuthority('PROFILES_READ') or hasAuthority('PROFILES_MANAGE')")
    @Operation(summary = "List all access profiles")
    fun listProfiles(): ResponseEntity<List<ProfileResponse>> {
        val profiles =
            profileService.listProfiles().map { profile ->
                val userCount = userRepository.countByProfileId(profile.id)
                ProfileResponse(
                    id = profile.id,
                    name = profile.name,
                    description = profile.description,
                    isSystem = profile.isSystem,
                    permissions = profile.permissions.map { it.id.permission }.sorted(),
                    userCount = userCount,
                    createdAt = profile.createdAt,
                    updatedAt = profile.updatedAt,
                )
            }
        return ResponseEntity.ok(profiles)
    }

    @PostMapping("/profiles")
    @PreAuthorize("hasAuthority('PROFILES_MANAGE')")
    @Operation(summary = "Create a new access profile")
    fun createProfile(
        @Valid @RequestBody request: CreateProfileRequest,
    ): ResponseEntity<ProfileResponse> {
        val created =
            profileService.createProfile(
                name = request.name,
                description = request.description,
                permissions = request.permissions.toSet(),
            )
        val response =
            ProfileResponse(
                id = created.id,
                name = created.name,
                description = created.description,
                isSystem = created.isSystem,
                permissions = created.permissions.map { it.id.permission }.sorted(),
                userCount = 0,
                createdAt = created.createdAt,
                updatedAt = created.updatedAt,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @PutMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('PROFILES_MANAGE')")
    @Operation(summary = "Update an access profile")
    fun updateProfile(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProfileRequest,
    ): ResponseEntity<ProfileResponse> {
        val updated =
            profileService.updateProfile(
                id = id,
                name = request.name,
                description = request.description,
                permissions = request.permissions.toSet(),
            )
        val userCount = userRepository.countByProfileId(updated.id)
        val response =
            ProfileResponse(
                id = updated.id,
                name = updated.name,
                description = updated.description,
                isSystem = updated.isSystem,
                permissions = updated.permissions.map { it.id.permission }.sorted(),
                userCount = userCount,
                createdAt = updated.createdAt,
                updatedAt = updated.updatedAt,
            )
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/profiles/{id}")
    @PreAuthorize("hasAuthority('PROFILES_MANAGE')")
    @Operation(summary = "Delete an access profile")
    fun deleteProfile(
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        profileService.deleteProfile(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('PROFILES_READ') or hasAuthority('PROFILES_MANAGE')")
    @Operation(summary = "Get full catalog of available system permissions")
    fun getPermissionCatalog(): ResponseEntity<List<PermissionCatalogItem>> {
        val catalog =
            profileService.getPermissionCatalog().map { perm ->
                PermissionCatalogItem(
                    code = perm.code,
                    category = perm.category,
                    label = perm.label,
                    description = perm.description,
                )
            }
        return ResponseEntity.ok(catalog)
    }
}
