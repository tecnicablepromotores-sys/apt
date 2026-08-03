package com.tecnicable.backend.routes

import com.auth0.jwt.JWT
import com.tecnicable.backend.security.JwtConfig
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Collections
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

// Almacena las sesiones de WebSockets activas, organizadas por el UUID del usuario
object FormWebSocketSessionManager {
    private val sessions = ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    /**
     * Registra una nueva conexión WebSocket para un ID de usuario específico.
     */
    fun addSession(userId: String, session: DefaultWebSocketServerSession) {
        sessions.computeIfAbsent(userId) { Collections.synchronizedSet(LinkedHashSet()) }.add(session)
    }

    /**
     * Remueve la conexión WebSocket al cerrarse.
     */
    fun removeSession(userId: String, session: DefaultWebSocketServerSession) {
        sessions[userId]?.remove(session)
        if (sessions[userId]?.isEmpty() == true) {
            sessions.remove(userId)
        }
    }

    /**
     * Envía un mensaje en tiempo real a todas las conexiones activas asociadas a ese userId (Broadcast).
     */
    suspend fun emitToUser(userId: String, actionType: String, resourceId: String, payloadData: String) {
        val userSessions = sessions[userId] ?: return
        val messageText = buildJsonObject {
            put("action", actionType) // "CREATE" o "UPDATE"
            put("id", resourceId)
            put("data", payloadData)
            put("timestamp", System.currentTimeMillis())
        }.toString()

        synchronized(userSessions) {
            userSessions.forEach { session ->
                // Coroutine scope del propio servidor WebSocket para enviar el frame
                io.ktor.utils.io.CancellationException::class // Ensure class resolution safely 
                try {
                    session.send(Frame.Text(messageText))
                } catch (e: Exception) {
                    // Si ocurre un error, probablemente se desconectó: se limpiará solo o en el bloque del socket
                }
            }
        }
    }
}

/**
 * Endpoint WebSocket: WS /ws/forms?token=<jwt_token>
 * Permite mantener intercomunicados en tiempo real los dispositivos del usuario técnico en campo.
 */
fun Route.webSocketRouting() {
    webSocket("/ws/forms") {
        val token = call.parameters["token"]
        if (token.isNullOrBlank()) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token JWT es obligatorio en los query parameters."))
            return@webSocket
        }

        val userId: String = try {
            val decodedJWT = JwtConfig.verifier.verify(token)
            decodedJWT.getClaim("userId").asString()
        } catch (e: Exception) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Token JWT inválido o expirado."))
            return@webSocket
        }

        // Registrar la sesión de forma segura
        FormWebSocketSessionManager.addSession(userId, this)
        println("Conexión WebSocket establecida con éxito. Usuario: $userId")

        try {
            // Mantener el socket abierto escuchando (ping/pong y mensajes entrantes opcionales)
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    // Si el cliente envía algo, el backend puede procesarlo, responder o mantener un eco.
                    send(Frame.Text("Eco recibido: $text"))
                }
            }
        } catch (e: Exception) {
            println("Error de red o desconexión en WebSocket para el usuario $userId: ${e.message}")
        } finally {
            FormWebSocketSessionManager.removeSession(userId, this)
            println("Conexión WebSocket cerrada para el usuario: $userId")
        }
    }
}
