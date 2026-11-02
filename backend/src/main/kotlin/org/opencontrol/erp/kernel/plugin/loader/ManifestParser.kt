package org.opencontrol.erp.kernel.plugin.loader

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.opencontrol.erp.kernel.exception.PluginValidationException
import org.springframework.stereotype.Component
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

@Component
class ManifestParser(
    private val objectMapper: ObjectMapper = jacksonObjectMapper(),
) {
    companion object {
        const val MANIFEST_PATH = "META-INF/opencontrol-plugin.json"
        private val SEMVER_REGEX =
            Regex(
                "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$",
            )
        private val PLUGIN_ID_REGEX = Regex("^[a-z0-9]+(\\.[a-z0-9]+)*$")
    }

    fun parse(inputStream: InputStream): PluginManifest {
        val manifest: PluginManifest =
            try {
                objectMapper.readValue(inputStream)
            } catch (e: Exception) {
                throw PluginValidationException("Failed to parse plugin manifest JSON: ${e.message}", cause = e)
            }

        validate(manifest)
        return manifest
    }

    fun parseFromJar(jarFile: File): PluginManifest {
        if (!jarFile.exists() || !jarFile.canRead()) {
            throw PluginValidationException("Plugin archive file does not exist or cannot be read: ${jarFile.absolutePath}")
        }

        JarFile(jarFile).use { jar ->
            val entry =
                jar.getJarEntry(MANIFEST_PATH)
                    ?: throw PluginValidationException("Manifest file '$MANIFEST_PATH' not found in plugin archive")

            jar.getInputStream(entry).use { stream ->
                return parse(stream)
            }
        }
    }

    fun validate(manifest: PluginManifest) {
        if (!PLUGIN_ID_REGEX.matches(manifest.id)) {
            throw PluginValidationException(
                "Invalid plugin ID format: '${manifest.id}'. Must be lowercase reverse-domain notation (e.g. org.opencontrol.fiscal.br)",
            )
        }
        if (manifest.name.isBlank()) {
            throw PluginValidationException("Plugin manifest 'name' must not be blank")
        }
        if (!SEMVER_REGEX.matches(manifest.version)) {
            throw PluginValidationException(
                "Invalid plugin version format: '${manifest.version}'. Must follow Semantic Versioning (e.g. 1.0.0)",
            )
        }
        if (manifest.vendor.isBlank()) {
            throw PluginValidationException("Plugin manifest 'vendor' must not be blank")
        }
        if (!SEMVER_REGEX.matches(manifest.minKernelVersion)) {
            throw PluginValidationException(
                "Invalid minKernelVersion format: '${manifest.minKernelVersion}'. Must follow Semantic Versioning",
            )
        }
    }
}
