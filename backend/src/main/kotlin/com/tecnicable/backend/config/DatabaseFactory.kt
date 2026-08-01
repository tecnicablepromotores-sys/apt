package com.tecnicable.backend.config

import com.tecnicable.backend.model.FormsTable
import com.tecnicable.backend.model.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val driverClassName = "org.postgresql.Driver"
        val jdbcUrl = System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/tecnicable_baas"
        val user = System.getenv("POSTGRES_USER") ?: "postgres"
        val password = System.getenv("POSTGRES_PASSWORD") ?: throw IllegalStateException("POSTGRES_PASSWORD environment variable is not set")

        val database = Database.connect(jdbcUrl, driverClassName, user, password)

        transaction(database) {
            // Crear tablas automáticamente en la base de datos si no existen
            SchemaUtils.create(UsersTable, FormsTable)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
