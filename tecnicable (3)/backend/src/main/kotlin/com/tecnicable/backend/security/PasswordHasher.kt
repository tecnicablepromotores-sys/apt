package com.tecnicable.backend.security

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    
    /**
     * Genera un hash Bcrypt robusto de la contraseña del usuario.
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(12))
    }

    /**
     * Verifica si la contraseña en texto plano corresponde al hash almacenado.
     */
    fun checkPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(password, hash)
        } catch (e: Exception) {
            false
        }
    }
}
