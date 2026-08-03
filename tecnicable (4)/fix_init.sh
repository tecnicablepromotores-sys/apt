#!/bin/bash
sed -i '/init {/a \
        firebaseAuth.addAuthStateListener { auth -> \
            val currentUser = auth.currentUser \
            if (currentUser != null) { \
                viewModelScope.launch { \
                    try { \
                        val doc = usuariosCollection.document(currentUser.uid).get().await() \
                        if (doc.exists()) { \
                            val perfil = doc.toObject(PerfilUsuario::class.java) \
                            _activePerfil.value = perfil \
                            _isUserLoggedIn.value = true \
                        } else { \
                            _activePerfil.value = null \
                            _isUserLoggedIn.value = false \
                        } \
                    } catch (e: Exception) { \
                        _activePerfil.value = null \
                        _isUserLoggedIn.value = false \
                    } \
                } \
            } else { \
                _activePerfil.value = null \
                _isUserLoggedIn.value = false \
            } \
        } \
' app/src/main/java/com/example/MainViewModel.kt
