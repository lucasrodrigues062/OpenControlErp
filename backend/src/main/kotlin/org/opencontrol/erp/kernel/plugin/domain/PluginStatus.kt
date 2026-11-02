package org.opencontrol.erp.kernel.plugin.domain

enum class PluginStatus {
    DISCOVERED,
    INSTALLED,
    ACTIVE,
    INACTIVE,
    ERROR,
}

enum class PluginAction {
    INSTALL,
    ACTIVATE,
    DEACTIVATE,
    UNINSTALL,
    CONFIG_CHANGE,
    ERROR,
}
