package org.opencontrol.erp.kernel.context

import java.util.UUID

data class TenantContext(
    val tenantId: UUID,
    val tenantCode: String,
    val jurisdiction: String = "BR",
)

data class UserContext(
    val userId: String,
    val username: String,
    val roles: Set<String> = emptySet(),
    val tenant: TenantContext? = null,
)

object SecurityContextHolder {
    private val currentUser = ThreadLocal<UserContext>()

    fun setContext(user: UserContext) {
        currentUser.set(user)
    }

    fun getContext(): UserContext? = currentUser.get()

    fun getCurrentUsername(): String = currentUser.get()?.username ?: "system"

    fun getCurrentTenantId(): UUID = currentUser.get()?.tenant?.tenantId
        ?: UUID.fromString("00000000-0000-0000-0000-000000000000")

    fun getCurrentJurisdiction(): String = currentUser.get()?.tenant?.jurisdiction ?: "BR"

    fun clearContext() {
        currentUser.remove()
    }
}
