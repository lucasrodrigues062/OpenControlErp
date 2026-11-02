package org.opencontrol.erp.kernel.plugin.service

import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.exception.EntityNotFoundException
import org.opencontrol.erp.kernel.exception.PluginConflictException
import org.opencontrol.erp.kernel.exception.PluginValidationException
import org.opencontrol.erp.kernel.plugin.PluginContext
import org.opencontrol.erp.kernel.plugin.PluginLifecycle
import org.opencontrol.erp.kernel.plugin.domain.ExtensionAuditLog
import org.opencontrol.erp.kernel.plugin.domain.ExtensionDependency
import org.opencontrol.erp.kernel.plugin.domain.ExtensionPlugin
import org.opencontrol.erp.kernel.plugin.domain.PluginAction
import org.opencontrol.erp.kernel.plugin.domain.PluginStatus
import org.opencontrol.erp.kernel.plugin.loader.ManifestParser
import org.opencontrol.erp.kernel.plugin.loader.PluginClassLoader
import org.opencontrol.erp.kernel.plugin.repository.ExtensionAuditLogRepository
import org.opencontrol.erp.kernel.plugin.repository.ExtensionPluginRepository
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Service
class PluginLifecycleService(
    private val pluginRepository: ExtensionPluginRepository,
    private val auditLogRepository: ExtensionAuditLogRepository,
    private val manifestParser: ManifestParser,
    private val dependencyResolver: DependencyResolver,
    private val applicationContext: ApplicationContext,
    private val extensionRegistry: org.opencontrol.erp.kernel.spi.service.ExtensionRegistry,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val activeClassLoaders = ConcurrentHashMap<String, PluginClassLoader>()
    private val activeInstances = ConcurrentHashMap<String, PluginLifecycle>()

    @Transactional
    fun install(
        inputStream: InputStream,
        filename: String,
    ): ExtensionPlugin {
        val tempFile = File.createTempFile("opencontrol-plugin-", ".jar")
        try {
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            val checksum = calculateChecksum(tempFile)
            val manifest = manifestParser.parseFromJar(tempFile)

            if (pluginRepository.existsByPluginId(manifest.id)) {
                throw PluginConflictException("A plugin with identifier '${manifest.id}' is already installed")
            }

            val storageDir = File("./plugins")
            if (!storageDir.exists()) storageDir.mkdirs()
            val permanentFile = File(storageDir, "${manifest.id}-${manifest.version}.jar")
            Files.copy(tempFile.toPath(), permanentFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val plugin =
                ExtensionPlugin(
                    pluginId = manifest.id,
                    name = manifest.name,
                    description = manifest.description,
                    version = manifest.version,
                    vendor = manifest.vendor,
                    minKernelVersion = manifest.minKernelVersion,
                    maxKernelVersion = manifest.maxKernelVersion,
                    status = PluginStatus.INSTALLED,
                    archiveChecksum = checksum,
                    filePath = permanentFile.absolutePath,
                )

            manifest.dependencies.forEach { dep ->
                plugin.dependencies.add(
                    ExtensionDependency(
                        plugin = plugin,
                        dependsOnPluginId = dep.pluginId,
                        versionRange = dep.versionRange,
                        optional = dep.optional,
                    ),
                )
            }

            val savedPlugin = pluginRepository.save(plugin)

            recordAuditLog(
                pluginFk = savedPlugin.id,
                action = PluginAction.INSTALL,
                fromStatus = null,
                toStatus = PluginStatus.INSTALLED,
                details = "Installed package: $filename (checksum: $checksum)",
            )

            logger.info("Successfully installed plugin '{}' version {}", savedPlugin.pluginId, savedPlugin.version)
            return savedPlugin
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Transactional
    fun activate(id: UUID): ExtensionPlugin {
        val plugin =
            pluginRepository.findById(id).orElseThrow {
                EntityNotFoundException("Plugin not found with ID: $id")
            }

        if (plugin.status == PluginStatus.ACTIVE) {
            return plugin
        }

        // Validate dependencies
        val allPlugins = pluginRepository.findAll()
        dependencyResolver.resolveActivationOrder(allPlugins.filter { it.status == PluginStatus.ACTIVE || it.id == plugin.id })

        val previousStatus = plugin.status
        try {
            val jarFile = File(plugin.filePath ?: throw PluginValidationException("Plugin archive path missing"))
            val classLoader = PluginClassLoader(plugin.pluginId, jarFile, javaClass.classLoader)
            activeClassLoaders[plugin.pluginId] = classLoader

            val manifest = manifestParser.parseFromJar(jarFile)
            manifest.entryPointClass?.let { entryClassName ->
                val clazz = classLoader.loadClass(entryClassName)
                if (PluginLifecycle::class.java.isAssignableFrom(clazz)) {
                    val lifecycleInstance = clazz.getDeclaredConstructor().newInstance() as PluginLifecycle
                    val context =
                        PluginContext(
                            plugin = plugin,
                            classLoader = classLoader,
                            applicationContext = applicationContext,
                            pluginDirectory = jarFile.parentFile,
                        )
                    lifecycleInstance.onActivate(context)
                    activeInstances[plugin.pluginId] = lifecycleInstance
                }
            }

            plugin.status = PluginStatus.ACTIVE
            plugin.errorMessage = null
            plugin.updatedAt = Instant.now()
            val saved = pluginRepository.save(plugin)

            recordAuditLog(
                pluginFk = saved.id,
                action = PluginAction.ACTIVATE,
                fromStatus = previousStatus,
                toStatus = PluginStatus.ACTIVE,
            )

            logger.info("Activated plugin '{}'", plugin.pluginId)
            return saved
        } catch (e: Exception) {
            plugin.status = PluginStatus.ERROR
            plugin.errorMessage = e.message
            plugin.updatedAt = Instant.now()
            pluginRepository.save(plugin)

            recordAuditLog(
                pluginFk = plugin.id,
                action = PluginAction.ERROR,
                fromStatus = previousStatus,
                toStatus = PluginStatus.ERROR,
                details = "Activation failure: ${e.message}",
            )

            activeClassLoaders.remove(plugin.pluginId)?.close()
            activeInstances.remove(plugin.pluginId)
            throw PluginValidationException("Failed to activate plugin '${plugin.pluginId}': ${e.message}", cause = e)
        }
    }

    @Transactional
    fun deactivate(id: UUID): ExtensionPlugin {
        val plugin =
            pluginRepository.findById(id).orElseThrow {
                EntityNotFoundException("Plugin not found with ID: $id")
            }

        if (plugin.status != PluginStatus.ACTIVE) {
            return plugin
        }

        val allActive = pluginRepository.findAllByStatus(PluginStatus.ACTIVE)
        dependencyResolver.validateCanDeactivate(plugin, allActive)

        val previousStatus = plugin.status
        activeInstances.remove(plugin.pluginId)?.let { instance ->
            val context =
                PluginContext(
                    plugin = plugin,
                    classLoader = activeClassLoaders[plugin.pluginId] ?: javaClass.classLoader,
                    applicationContext = applicationContext,
                    pluginDirectory = File(plugin.filePath ?: "."),
                )
            try {
                instance.onDeactivate(context)
            } catch (e: Exception) {
                logger.error("Error during deactivation of plugin '{}'", plugin.pluginId, e)
            }
        }

        activeClassLoaders.remove(plugin.pluginId)?.close()
        extensionRegistry.unregisterProvidersByPluginId(plugin.pluginId)

        plugin.status = PluginStatus.INACTIVE
        plugin.updatedAt = Instant.now()
        val saved = pluginRepository.save(plugin)

        recordAuditLog(
            pluginFk = saved.id,
            action = PluginAction.DEACTIVATE,
            fromStatus = previousStatus,
            toStatus = PluginStatus.INACTIVE,
        )

        logger.info("Deactivated plugin '{}'", plugin.pluginId)
        return saved
    }

    @Transactional
    fun uninstall(
        id: UUID,
        purgeData: Boolean,
    ) {
        val plugin =
            pluginRepository.findById(id).orElseThrow {
                EntityNotFoundException("Plugin not found with ID: $id")
            }

        if (plugin.status == PluginStatus.ACTIVE) {
            deactivate(id)
        }

        plugin.filePath?.let { path ->
            val file = File(path)
            if (file.exists()) file.delete()
        }

        recordAuditLog(
            pluginFk = plugin.id,
            action = PluginAction.UNINSTALL,
            fromStatus = plugin.status,
            toStatus = PluginStatus.INACTIVE,
            details = "Uninstalled plugin '${plugin.pluginId}'. Purge data: $purgeData",
        )

        pluginRepository.delete(plugin)
        logger.info("Uninstalled plugin '{}'", plugin.pluginId)
    }

    @Transactional(readOnly = true)
    fun list(status: PluginStatus?): List<ExtensionPlugin> =
        if (status != null) {
            pluginRepository.findAllByStatus(status)
        } else {
            pluginRepository.findAll()
        }

    @Transactional(readOnly = true)
    fun getById(id: UUID): ExtensionPlugin =
        pluginRepository.findById(id).orElseThrow {
            EntityNotFoundException("Plugin not found with ID: $id")
        }

    fun getClassLoader(pluginId: String): PluginClassLoader? = activeClassLoaders[pluginId]

    private fun recordAuditLog(
        pluginFk: UUID,
        action: PluginAction,
        fromStatus: PluginStatus?,
        toStatus: PluginStatus,
        details: String? = null,
    ) {
        auditLogRepository.save(
            ExtensionAuditLog(
                pluginFk = pluginFk,
                action = action,
                performedBy = SecurityContextHolder.getCurrentUsername(),
                fromStatus = fromStatus,
                toStatus = toStatus,
                details = details,
            ),
        )
    }

    private fun calculateChecksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var read: Int
            while (stream.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
