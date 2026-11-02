package org.opencontrol.erp.iam.service

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(
    @param:Value("\${opencontrol.security.jwt.secret:opencontrol-default-secure-secret-key-for-development-must-be-at-least-256-bits!}")
    private val secret: String,
    @param:Value("\${opencontrol.security.jwt.expiration-seconds:86400}")
    private val expirationSeconds: Long,
) {
    private val signingKey: SecretKey by lazy {
        val keyBytes = secret.toByteArray(StandardCharsets.UTF_8)
        if (keyBytes.size < 32) {
            Keys.hmacShaKeyFor(secret.padEnd(32, '0').toByteArray(StandardCharsets.UTF_8))
        } else {
            Keys.hmacShaKeyFor(keyBytes)
        }
    }

    fun generateToken(
        userId: UUID,
        username: String,
        tenantId: UUID,
        profileName: String,
        permissions: List<String>,
    ): String {
        val now = Instant.now()
        val expiry = now.plusSeconds(expirationSeconds)

        return Jwts
            .builder()
            .subject(username)
            .claim("userId", userId.toString())
            .claim("tenantId", tenantId.toString())
            .claim("profile", profileName)
            .claim("permissions", permissions)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact()
    }

    fun parseAndValidateToken(token: String): Claims =
        Jwts
            .parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload

    fun isTokenValid(token: String): Boolean =
        try {
            val claims = parseAndValidateToken(token)
            !claims.expiration.before(Date())
        } catch (_: Exception) {
            false
        }
}
