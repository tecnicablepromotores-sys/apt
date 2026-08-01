package com.tecnicable.backend.routes

import com.tecnicable.backend.config.DatabaseFactory.dbQuery
import com.tecnicable.backend.model.Form
import com.tecnicable.backend.model.FormPayload
import com.tecnicable.backend.model.FormsTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.UUID

fun Route.formRouting() {
    // Proteger todas estas rutas con el middleware JWT de Ktor (RLS lógico automático)
    authenticate("auth-jwt") {
        route("/api/forms") {

            // 1. CREAR FORMULARIO: POST /api/forms
            post {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val authUserIdString = principal?.payload?.getClaim("userId")?.asString()
                    
                    if (authUserIdString == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "No se pudo identificar al usuario desde el token."))
                        return@post
                    }

                    val payload = call.receive<FormPayload>()
                    if (payload.formData.length > 10000) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El formulario excede el tamaño permitido (máx 10000 caracteres)"))
                        return@post
                    }
                    val formUuid = if (payload.id != null) UUID.fromString(payload.id) else UUID.randomUUID()
                    val userUuid = UUID.fromString(authUserIdString)

                    dbQuery {
                        FormsTable.insert {
                            it[id] = formUuid
                            it[userId] = userUuid
                            it[formData] = payload.formData
                            it[updatedAt] = LocalDateTime.now()
                        }
                    }

                    // Broker Broadcast en tiempo real para todos los clientes activos de este usuario
                    FormWebSocketSessionManager.emitToUser(
                        userId = authUserIdString,
                        actionType = "CREATE",
                        resourceId = formUuid.toString(),
                        payloadData = payload.formData
                    )

                    call.respond(
                        HttpStatusCode.Created,
                        Form(
                            id = formUuid.toString(),
                            userId = authUserIdString,
                            formData = payload.formData,
                            updatedAt = LocalDateTime.now().toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Error al deserializar o guardar datos: ${e.message}"))
                }
            }

            // 2. RECUPERAR FORMULARIOS DEL PROPIO USUARIO: GET /api/forms (Rutas RLS)
            get {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val authUserIdString = principal?.payload?.getClaim("userId")?.asString()
                    
                    if (authUserIdString == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Sesión inválida."))
                        return@get
                    }

                    val userUuid = UUID.fromString(authUserIdString)

                    // RLS (Row Level Security): Filtrar estrictamente solo lo correspondiente al usuario autenticado
                    val ownedForms = dbQuery {
                        FormsTable.select { FormsTable.userId eq userUuid }
                            .map { 
                                Form(
                                    id = it[FormsTable.id].toString(),
                                    userId = it[FormsTable.userId].toString(),
                                    formData = it[FormsTable.formData],
                                    updatedAt = it[FormsTable.updatedAt].toString()
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, ownedForms)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Error cargando la lista de formularios: ${e.message}"))
                }
            }

            // 3. EDITAR / ACTUALIZAR FORMULARIO: PUT /api/forms/{id}
            put("/{id}") {
                try {
                    val idParam = call.parameters["id"]
                    if (idParam == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "ID de formulario requerido en la ruta."))
                        return@put
                    }

                    val principal = call.principal<JWTPrincipal>()
                    val authUserIdString = principal?.payload?.getClaim("userId")?.asString()
                    
                    if (authUserIdString == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token de autenticación expirado."))
                        return@put
                    }

                    val currentFormUuid = UUID.fromString(idParam)
                    val currentUserUuid = UUID.fromString(authUserIdString)

                    // 1. Obtener contrato localmente para verificar que realmente pertenece al emisor
                    val existingForm = dbQuery {
                        FormsTable.select { (FormsTable.id eq currentFormUuid) and (FormsTable.userId eq currentUserUuid) }
                            .singleOrNull()
                    }

                    if (existingForm == null) {
                        // El formulario no existe o no le pertenece (Políticas RLS en acción)
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Acceso denegado o el formulario especificado no existe."))
                        return@put
                    }

                    val payload = call.receive<FormPayload>()
                    if (payload.formData.length > 10000) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "El formulario excede el tamaño permitido (máx 10000 caracteres)"))
                        return@put
                    }
                    val newUpdateTime = LocalDateTime.now()

                    // 2. Modificar con los nuevos atributos sanitizados
                    dbQuery {
                        FormsTable.update({ FormsTable.id eq currentFormUuid }) {
                            it[formData] = payload.formData
                            it[updatedAt] = newUpdateTime
                        }
                    }

                    // 3. Emitir el cambio a otros dispositivos del mismo usuario a través del socket
                    FormWebSocketSessionManager.emitToUser(
                        userId = authUserIdString,
                        actionType = "UPDATE",
                        resourceId = currentFormUuid.toString(),
                        payloadData = payload.formData
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        Form(
                            id = currentFormUuid.toString(),
                            userId = authUserIdString,
                            formData = payload.formData,
                            updatedAt = newUpdateTime.toString()
                        )
                    )
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Error actualizando el formulario: ${e.message}"))
                }
            }
        }
    }
}
