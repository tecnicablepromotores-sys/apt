package com.tecnicable.backend

import com.tecnicable.backend.config.DatabaseFactory
import com.tecnicable.backend.routes.authRouting
import com.tecnicable.backend.routes.formRouting
import com.tecnicable.backend.routes.webSocketRouting
import com.tecnicable.backend.security.JwtConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import java.time.Duration

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 1. Inicialización de la base de datos PostgreSQL y Exposed ORM
    try {
        DatabaseFactory.init()
        println("Base de datos PostgreSQL inicializada con Exposed correctamente.")
    } catch (e: Exception) {
        log.error("Fallo grave al conectar a base de datos. Verifique si PostgreSQL está activo: ${e.message}")
    }

    // 2. Control de CORS (Cross-Origin Resource Sharing)
    install(CORS) {
        anyHost() // Idealmente configurar dominios específicos en producción
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

    // 3. Negociación de Contenido (Content Negotiation) para habilitar soporte JSON nativo
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // 4. Módulo de WebSockets para comunicación persistente en tiempo real
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // 5. Middleware de Autenticación JWT incorporado en Ktor
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "Tecnicable Backend Access"
            verifier(JwtConfig.verifier)
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            challenge { defaultScheme, realm ->
                call.respond(
                    io.ktor.http.HttpStatusCode.Unauthorized, 
                    mapOf("error" to "Token JWT inválido, expirado o malformado.")
                )
            }
        }
    }

    // 6. Registro de Rutas Generales de la Aplicación
    routing {
        // Endpoint raíz de verificación para comprobaciones de estado/ping
        get("/") {
            call.respond(mapOf(
                "app" to "Tecnicable BaaS",
                "version" to "1.0.0",
                "status" to "ONLINE",
                "real_time_supported" to "WebSockets"
            ))
        }

        authRouting()       // POST /auth/register y POST /auth/login
        formRouting()       // POST /api/forms, GET /api/forms, PUT /api/forms/{id} (Protegidas)
        webSocketRouting()  // WS /ws/forms?token=JWT-JWT
    }
}
