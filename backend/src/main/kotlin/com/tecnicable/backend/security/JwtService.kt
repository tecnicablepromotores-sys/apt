package com.tecnicable.backend.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

object JwtConfig {
    private val SECRET = System.getenv("JWT_SECRET") ?: throw IllegalStateException("JWT_SECRET environment variable is not set")
    const val ISSUER = "com.tecnicable"
    const val AUDIENCE = "com.tecnicable.app"
    private const val VALIDITY_MS = 24 * 60 * 60 * 1000L // 24 horas en milisegundos

    private val algorithm = Algorithm.HMAC256(SECRET)

    /**
     * Instancia del Verifier para validar y parsear tokens en Ktor
     */
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(ISSUER)
        .withAudience(AUDIENCE)
        .build()

    /**
     * Genera un token JWT firmado válido por 24 horas con el userId y email como Claims.
     */
    fun createToken(userId: UUID, email: String): String {
        return JWT.create()
            .withAudience(AUDIENCE)
            .withIssuer(ISSUER)
            .withClaim("userId", userId.toString())
            .withClaim("email", email)
            .withExpiresAt(Date(System.currentTimeMillis() + VALIDITY_MS))
            .sign(algorithm)
    }
}
