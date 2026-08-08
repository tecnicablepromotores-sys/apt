package com.tecnicable.backend.routes

import com.tecnicable.backend.config.DatabaseFactory.dbQuery
import com.tecnicable.backend.model.AuthResponse
import com.tecnicable.backend.model.UserCredentials
import com.tecnicable.backend.model.UsersTable
import com.tecnicable.backend.security.JwtConfig
import com.tecnicable.backend.security.PasswordHasher
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import java.time.LocalDateTime
import java.util.UUID

fun Route.authRouting() {
    route("/auth") {
        
        // 1. REGISTRO DE USUARIO: POST /auth/register
        post("/register") {
            try {
                val credentials = call.receive<UserCredentials>()
                
                if (credentials.email.isBlank() || credentials.password.length < 6) {
                    call.respond(
                        HttpStatusCode.BadRequest, 
                        mapOf("error" to "Formato inválido. Email requerido y clave debe tener más de 6 caracteres.")
                    )
                    return@post
                }

                // Verificar si ya existe el correo
                val existingUser = dbQuery {
                    UsersTable.select { UsersTable.email eq credentials.email }.singleOrNull()
                }

                if (existingUser != null) {
                    call.respond(HttpStatusCode.Conflict, mapOf("error" to "El usuario ya se encuentra registrado."))
                    return@post
                }

                val newUserId = UUID.randomUUID()
                val hpw = PasswordHasher.hashPassword(credentials.password)
                
                dbQuery {
                    UsersTable.insert {
                        it[id] = newUserId
                        it[email] = credentials.email
                        it[passwordHash] = hpw
                        it[createdAt] = LocalDateTime.now()
                    }
                }

                val token = JwtConfig.createToken(newUserId, credentials.email)
                call.respond(
                    HttpStatusCode.Created, 
                    AuthResponse(token = token, email = credentials.email, userId = newUserId.toString())
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor: ${e.message}"))
            }
        }

        // 2. INICIO DE SESIÓN / INGRESO: POST /auth/login
        post("/login") {
            try {
                val credentials = call.receive<UserCredentials>()
                
                val userRow = dbQuery {
                    UsersTable.select { UsersTable.email eq credentials.email }.singleOrNull()
                }

                if (userRow == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Usuario o contraseña inválidos."))
                    return@post
                }

                val dbHash = userRow[UsersTable.passwordHash]
                val isValid = PasswordHasher.checkPassword(credentials.password, dbHash)

                if (!isValid) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Usuario o contraseña inválidos."))
                    return@post
                }

                val uId = userRow[UsersTable.id]
                val token = JwtConfig.createToken(uId, credentials.email)

                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse(token = token, email = credentials.email, userId = uId.toString())
                )
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error interno del servidor: ${e.message}"))
            }
        }
    }
}
