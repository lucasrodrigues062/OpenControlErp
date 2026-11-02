package org.opencontrol.erp.kernel.api

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.opencontrol.erp.kernel.api.dto.ExtensionPluginDto
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.service.PluginLifecycleService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/v1/extensions")
@Tag(name = "Extension Management", description = "Endpoints for managing modular apps, extensions, and lifecycle")
class ExtensionController(
    private val pluginLifecycleService: PluginLifecycleService
) {

    @GetMapping
    @Operation(summary = "List all installed and discovered extensions")
    fun listExtensions(
        @RequestParam(required = false) status: PluginStatus?
    ): ResponseEntity<List<ExtensionPluginDto>> {
        val plugins = pluginLifecycleService.list(status).map { ExtensionPluginDto.fromEntity(it) }
        return ResponseEntity.ok(plugins)
    }

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload and install a new extension package")
    fun installExtension(
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<ExtensionPluginDto> {
        val plugin = pluginLifecycleService.install(file.inputStream, file.originalFilename ?: "plugin.jar")
        return ResponseEntity.status(HttpStatus.CREATED).body(ExtensionPluginDto.fromEntity(plugin))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get details of a specific extension")
    fun getExtension(@PathVariable id: UUID): ResponseEntity<ExtensionPluginDto> {
        val plugin = pluginLifecycleService.getById(id)
        return ResponseEntity.ok(ExtensionPluginDto.fromEntity(plugin))
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate an installed or inactive extension")
    fun activateExtension(@PathVariable id: UUID): ResponseEntity<ExtensionPluginDto> {
        val plugin = pluginLifecycleService.activate(id)
        return ResponseEntity.ok(ExtensionPluginDto.fromEntity(plugin))
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate an active extension")
    fun deactivateExtension(@PathVariable id: UUID): ResponseEntity<ExtensionPluginDto> {
        val plugin = pluginLifecycleService.deactivate(id)
        return ResponseEntity.ok(ExtensionPluginDto.fromEntity(plugin))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Uninstall an extension")
    fun uninstallExtension(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "false") purgeData: Boolean
    ): ResponseEntity<Void> {
        pluginLifecycleService.uninstall(id, purgeData)
        return ResponseEntity.noContent().build()
    }
}
