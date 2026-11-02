package org.opencontrol.erp.iam.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.opencontrol.erp.iam.service.JwtService
import org.opencontrol.erp.kernel.context.SecurityContextHolder
import org.opencontrol.erp.kernel.context.TenantContext
import org.opencontrol.erp.kernel.context.UserContext
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID
import org.springframework.security.core.context.SecurityContextHolder as SpringSecurityContextHolder

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)
        try {
            if (jwtService.isTokenValid(token)) {
                val claims = jwtService.parseAndValidateToken(token)
                val username = claims.subject
                val userId = claims["userId"]?.toString() ?: ""
                val tenantIdStr = claims["tenantId"]?.toString() ?: "00000000-0000-0000-0000-000000000000"
                val tenantId = UUID.fromString(tenantIdStr)

                @Suppress("UNCHECKED_CAST")
                val permissionsList = (claims["permissions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList()
                val authorities = permissionsList.map { SimpleGrantedAuthority(it) }

                val authToken = UsernamePasswordAuthenticationToken(username, null, authorities)
                SpringSecurityContextHolder.getContext().authentication = authToken

                SecurityContextHolder.setContext(
                    UserContext(
                        userId = userId,
                        username = username,
                        roles = permissionsList.toSet(),
                        tenant =
                            TenantContext(
                                tenantId = tenantId,
                                tenantCode = "DEFAULT",
                                jurisdiction = "BR",
                            ),
                    ),
                )
            }
        } catch (_: Exception) {
            SpringSecurityContextHolder.clearContext()
            SecurityContextHolder.clearContext()
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
