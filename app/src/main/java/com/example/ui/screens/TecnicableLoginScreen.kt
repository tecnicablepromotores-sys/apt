package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.R
import com.example.ui.components.TecnicableBrandLogo
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun TecnicableLoginScreen(viewModel: MainViewModel) {
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    val isAuth by viewModel.isUserAuthenticated.collectAsState()
    var linkingNombre by remember { mutableStateOf("") }
    var linkingCedula by remember { mutableStateOf("") }
    var linkingError by remember { mutableStateOf<String?>(null) }
    if (isAuth) {
        AlertDialog(
            onDismissRequest = { viewModel.logout() },
            title = { Text("Vincular Cuenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Esta cuenta de Google no tiene un perfil asociado. Por favor, ingresa tu nombre y cédula para vincularla.")
                    OutlinedTextField(
                        value = linkingNombre,
                        onValueChange = { linkingNombre = it; linkingError = null },
                        label = { Text("Nombre Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkingCedula,
                        onValueChange = { linkingCedula = it; linkingError = null },
                        label = { Text("Cédula de Identidad") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (linkingError != null) {
                        Text(linkingError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (linkingNombre.isBlank() || linkingCedula.isBlank()) {
                            linkingError = "Ambos campos son obligatorios"
                        } else {
                            viewModel.vincularUsuario(linkingNombre, linkingCedula) { success ->
                                if (!success) {
                                    linkingError = "Error al vincular el usuario"
                                }
                            }
                        }
                    }
                ) { Text("Vincular") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.logout() }) { Text("Cancelar") }
            }
        )
        return
    }

    val context = LocalContext.current

    // Google Sign-In logic
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                viewModel.loginWithGoogle(credential) { success, msg ->
                    isLoggingIn = false
                    if (success) {
                        Toast.makeText(context, "¡Sesión iniciada con Google!", Toast.LENGTH_SHORT).show()
                    } else {
                        loginError = msg
                    }
                }
            } ?: run {
                isLoggingIn = false
                loginError = "Error: no se obtuvo token de Google"
            }
        } catch (e: ApiException) {
            isLoggingIn = false
            loginError = "Fallo el inicio con Google: ${e.message}"
        }
    }

    // Password Recovery Dialogue state variables
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var recoveryMode by remember { mutableStateOf(0) } // 0: Firebase Auth Email, 1: Direct Cedula
    var recoveryEmailOrCedula by remember { mutableStateOf("") }
    var recoveryCedula by remember { mutableStateOf("") }
    var recoveryClave by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf<String?>(null) }
    var recoverySuccessMsg by remember { mutableStateOf<String?>(null) }

    // Floating Password Recovery Dialog Component
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Column {
                    Text(
                        text = "Recuperar / Cambiar Contraseña",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0A4E9B),
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Recuperación de Cuenta",
                        fontSize = 12.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mode Selector Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = { recoveryMode = 0; recoveryError = null; recoverySuccessMsg = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recoveryMode == 0) Color(0xFF0A4E9B) else Color.Transparent,
                                contentColor = if (recoveryMode == 0) Color.White else Color(0xFF64748B)
                            ),
                            elevation = null,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Correo Electrónico", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { recoveryMode = 1; recoveryError = null; recoverySuccessMsg = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (recoveryMode == 1) Color(0xFF0A4E9B) else Color.Transparent,
                                contentColor = if (recoveryMode == 1) Color.White else Color(0xFF64748B)
                            ),
                            elevation = null,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cédula Directa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (recoveryMode == 0) {
                        Text(
                            text = "Se enviará un enlace de restablecimiento directamente a su correo electrónico.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        OutlinedTextField(
                            value = recoveryEmailOrCedula,
                            onValueChange = { recoveryEmailOrCedula = it; recoveryError = null; recoverySuccessMsg = null },
                            label = { Text("Correo Electrónico o Cédula *") },
                            placeholder = { Text("Ej: usuario@gmail.com o 12345678") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedLabelColor = Color(0xFF0A4E9B),
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedBorderColor = Color(0xFF0A4E9B),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_email_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    } else {
                        Text(
                            text = "Ingrese su número de cédula y la nueva contraseña para restablecer su acceso de forma directa.",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        OutlinedTextField(
                            value = recoveryCedula,
                            onValueChange = { recoveryCedula = it; recoveryError = null; recoverySuccessMsg = null },
                            label = { Text("Número de Cédula *") },
                            placeholder = { Text("Ej: 12345678") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedLabelColor = Color(0xFF0A4E9B),
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedBorderColor = Color(0xFF0A4E9B),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_cedula_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = recoveryClave,
                            onValueChange = { recoveryClave = it; recoveryError = null; recoverySuccessMsg = null },
                            label = { Text("Nueva Contraseña *") },
                            placeholder = { Text("Mín. 4 caracteres") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedLabelColor = Color(0xFF0A4E9B),
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedBorderColor = Color(0xFF0A4E9B),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedContainerColor = Color(0xFFF1F5F9),
                                unfocusedContainerColor = Color(0xFFF8FAFC)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_clave_input"),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    if (recoveryError != null) {
                        Text(
                            text = recoveryError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (recoverySuccessMsg != null) {
                        Text(
                            text = recoverySuccessMsg!!,
                            color = Color(0xFF16A34A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recoveryMode == 0) {
                            val input = recoveryEmailOrCedula.trim()
                            if (input.isBlank()) {
                                recoveryError = "Ingrese su correo o cédula"
                            } else {
                                viewModel.enviarCorreoRestablecimientoFirebaseAuth(input) { success, msg ->
                                    if (success) {
                                        recoverySuccessMsg = msg
                                        recoveryError = null
                                    } else {
                                        recoveryError = msg
                                        recoverySuccessMsg = null
                                    }
                                }
                            }
                        } else {
                            val ced = recoveryCedula.trim()
                            val clave = recoveryClave.trim()
                            if (ced.isBlank() || clave.isBlank()) {
                                recoveryError = "Todos los campos con (*) son obligatorios"
                            } else if (clave.length < 4) {
                                recoveryError = "La contraseña debe tener al menos 4 caracteres"
                            } else {
                                val err = viewModel.restablecerClaveConCedula(ced, clave)
                                if (err != null) {
                                    recoveryError = err
                                    recoverySuccessMsg = null
                                } else {
                                    Toast.makeText(
                                        context, 
                                        "¡Contraseña restablecida exitosamente!", 
                                        Toast.LENGTH_LONG
                                    ).show()
                                    showForgotPasswordDialog = false
                                    recoveryCedula = ""
                                    recoveryClave = ""
                                    recoveryError = null
                                    recoverySuccessMsg = null
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E9B))
                ) {
                    Text(if (recoveryMode == 0) "ENVIAR ENLACE DE RESTABLECIMIENTO" else "CAMBIAR CLAVE DE ACCESO", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("CANCELAR")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C2340)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color(0xFFB01A1A).copy(alpha = 0.12f),
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.15f, size.height * 0.2f)
            )
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = 0.08f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.8f)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 410.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TecnicableBrandLogo(
                    iconSize = 48.dp,
                    textSize = 28.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "SISTEMA INTEGRAL DE INSTALACIONES",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color(0xFF0C2340),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Ingrese su número de cédula y contraseña para ingresar al módulo de contratos y campo. (Administrador ingresa de la forma habitual)",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = usuario,
                    onValueChange = {
                        usuario = it
                        loginError = null
                    },
                    label = { Text("Cédula de Identidad") },
                    placeholder = { Text("Ej. 12345678 o correo") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0A4E9B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedLabelColor = Color(0xFF0A4E9B),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedBorderColor = Color(0xFF0A4E9B),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedPlaceholderColor = Color.LightGray,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_user_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = clave,
                    onValueChange = {
                        clave = it
                        loginError = null
                    },
                    label = { Text("Contraseña") },
                    placeholder = { Text("Clave de sistema") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0A4E9B)) },
                    trailingIcon = {
                        val iconImg = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(iconImg, contentDescription = "Mostrar contraseña", tint = Color(0xFF0A4E9B))
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedLabelColor = Color(0xFF0A4E9B),
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedBorderColor = Color(0xFF0A4E9B),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedPlaceholderColor = Color.LightGray,
                        unfocusedPlaceholderColor = Color.Gray,
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                if (loginError != null) {
                    Text(
                        text = loginError ?: "",
                        color = Color(0xFFB01A1A),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (usuario.isBlank() || clave.isBlank()) {
                            loginError = "Por favor, complete ambos campos"
                        } else {
                            isLoggingIn = true
                            loginError = null
                            viewModel.login(usuario, clave) { success, msg ->
                                isLoggingIn = false
                                if (success) {
                                    Toast.makeText(context, "¡Sesión iniciada correctamente!", Toast.LENGTH_SHORT).show()
                                } else {
                                    loginError = msg
                                }
                            }
                        }
                    },
                    enabled = !isLoggingIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E9B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_button")
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "ENTRAR AL SISTEMA",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                TextButton(
                    onClick = {
                        recoveryEmailOrCedula = usuario.trim()
                        recoveryCedula = ""
                        recoveryClave = ""
                        recoveryError = null
                        showForgotPasswordDialog = true
                    },
                    modifier = Modifier.padding(vertical = 1.dp)
                ) {
                    Text(
                        text = "¿Olvidó su contraseña? RESTABLECER POR CORREO O CÉDULA",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFB01A1A),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        isLoggingIn = true
                        loginError = null
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                    },
                    enabled = !isLoggingIn,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_login_button")
                ) {
                    if (isLoggingIn) {
                        CircularProgressIndicator(color = Color(0xFF0A4E9B), modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "ENTRAR CON GOOGLE",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "© 2026 Tecnicable Margarita C.A.\nRIF J-30715323-7. Todos los derechos reservados.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, color = Color.Gray),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
