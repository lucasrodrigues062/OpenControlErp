package org.opencontrol.erp.kernel.plugin

/**
 * Optional lifecycle interface implemented by an extension's main class to execute
 * custom startup and shutdown routines.
 */
interface PluginLifecycle {
    /**
     * Invoked when the extension is activated by the administrator.
     */
    fun onActivate(context: PluginContext)

    /**
     * Invoked when the extension is deactivated. Resources must be released.
     */
    fun onDeactivate(context: PluginContext)
}
