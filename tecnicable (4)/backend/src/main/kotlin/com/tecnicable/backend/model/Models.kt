package com.tecnicable.backend.model

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

// --- 1. Definición de Tablas con Exposed ORM ---

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val createdAt = datetime("fecha_creacion").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

object FormsTable : Table("forms") {
    val id = uuid("id")
    val userId = uuid("user_id").references(UsersTable.id)
    val formData = text("form_data") // Guardado como String JSON (o JSONB si se extiende Exposed)
    val updatedAt = datetime("fecha_actualizacion").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)
}

// --- 2. Modelos de Dominio (Domain Entities) ---

@Serializable
data class User(
    val id: String, // Representación en texto del UUID para compatibilidad con Serializers
    val email: String,
    val passwordHash: String,
    val createdAt: String
)

@Serializable
data class Form(
    val id: String,
    val userId: String,
    val formData: String, // String JSON conteniendo los detalles del contrato o formulario
    val updatedAt: String
)

// --- 3. DTOs (Data Transfer Objects) para las APIs ---

@Serializable
data class UserCredentials(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val email: String,
    val userId: String
)

@Serializable
data class FormPayload(
    val id: String? = null, // Dejar null para que el backend autogenere si es inserción
    val formData: String // JSON serializado que contiene los datos del formulario
)
