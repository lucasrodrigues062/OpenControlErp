package org.opencontrol.erp.kernel.plugin.loader

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class PluginDependencyDescriptor(
    val pluginId: String,
    val versionRange: String,
    val optional: Boolean = false,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExtensionPointDescriptor(
    val extensionPoint: String,
    val implementationClass: String,
    val priority: Int = 100,
    val contextConstraints: Map<String, Any>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventSubscriberDescriptor(
    val eventType: String,
    val subscriberClass: String,
    val mode: String = "ASYNC_POST_COMMIT",
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PluginManifest(
    val id: String,
    val name: String,
    val version: String,
    val vendor: String,
    val minKernelVersion: String,
    val maxKernelVersion: String? = null,
    val description: String? = null,
    val entryPointClass: String? = null,
    val dependencies: List<PluginDependencyDescriptor> = emptyList(),
    val extensions: List<ExtensionPointDescriptor> = emptyList(),
    val eventSubscribers: List<EventSubscriberDescriptor> = emptyList(),
)
