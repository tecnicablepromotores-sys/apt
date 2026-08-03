#!/bin/bash
sed -i 's/viewModel.signInWithFirebaseCredential(credential)/viewModel.loginWithGoogle(credential)/g' app/src/main/java/com/example/ui/screens/TecnicableLoginScreen.kt
sed -i 's/viewModel.attemptLoginAsync(usuario, clave)/viewModel.login(usuario, clave)/g' app/src/main/java/com/example/ui/screens/TecnicableLoginScreen.kt
