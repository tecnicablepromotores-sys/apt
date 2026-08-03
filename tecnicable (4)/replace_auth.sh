#!/bin/bash
cat app/src/main/java/com/example/MainViewModel.kt | head -n 125 > temp1.kt
cat << 'INNER_EOF' > temp2.kt
    fun login(email: String, clave: String, onResult: (Boolean, String) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, clave)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Login exitoso")
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Error desconocido"
                    onResult(false, errorMsg)
                }
            }
    }

    fun loginWithGoogle(credential: com.google.firebase.auth.AuthCredential, onResult: (Boolean, String) -> Unit) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, "Login con Google exitoso")
                } else {
                    val errorMsg = task.exception?.localizedMessage ?: "Error con Google"
                    onResult(false, errorMsg)
                }
            }
    }

    fun vincularUsuario(nombre: String, cedula: String, onResult: (Boolean) -> Unit) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            onResult(false)
            return
        }
        val uid = currentUser.uid
        val email = currentUser.email ?: ""
        val perfil = PerfilUsuario(
            id = 0,
            uuid = uid,
            nombre = nombre,
            usuario = email.substringBefore("@"),
            rol = "Promotor(a)",
            celular = "",
            esActivo = false,
            clave = "",
            cedula = cedula,
            correo = email
        )
        viewModelScope.launch {
            try {
                usuariosCollection.document(uid).set(perfil).await()
                _activePerfil.value = perfil
                _isUserLoggedIn.value = true
                onResult(true)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error al vincular usuario: ${e.message}")
                onResult(false)
            }
        }
    }

    fun logout() {
        firebaseAuth.signOut()
        prefs.edit().clear().apply()
        _isUserLoggedIn.value = false
        _activePerfil.value = null
        _allPerfiles.value = emptyList()
    }

INNER_EOF
cat app/src/main/java/com/example/MainViewModel.kt | sed -n '353,$p' > temp3.kt
cat temp1.kt temp2.kt temp3.kt > app/src/main/java/com/example/MainViewModel.kt
rm temp1.kt temp2.kt temp3.kt
