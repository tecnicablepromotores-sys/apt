#!/bin/bash
sed -i '/var isLoggingIn by remember { mutableStateOf(false) }/a \
    val isAuth by viewModel.isUserAuthenticated.collectAsState()\
    var linkingNombre by remember { mutableStateOf("") }\
    var linkingCedula by remember { mutableStateOf("") }\
    var linkingError by remember { mutableStateOf<String?>(null) }\
    if (isAuth) {\
        AlertDialog(\
            onDismissRequest = { viewModel.logout() },\
            title = { Text("Vincular Cuenta") },\
            text = {\
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {\
                    Text("Esta cuenta de Google no tiene un perfil asociado. Por favor, ingresa tu nombre y cédula para vincularla.")\
                    OutlinedTextField(\
                        value = linkingNombre,\
                        onValueChange = { linkingNombre = it; linkingError = null },\
                        label = { Text("Nombre Completo") },\
                        singleLine = true,\
                        modifier = Modifier.fillMaxWidth()\
                    )\
                    OutlinedTextField(\
                        value = linkingCedula,\
                        onValueChange = { linkingCedula = it; linkingError = null },\
                        label = { Text("Cédula de Identidad") },\
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),\
                        singleLine = true,\
                        modifier = Modifier.fillMaxWidth()\
                    )\
                    if (linkingError != null) {\
                        Text(linkingError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)\
                    }\
                }\
            },\
            confirmButton = {\
                Button(\
                    onClick = {\
                        if (linkingNombre.isBlank() || linkingCedula.isBlank()) {\
                            linkingError = "Ambos campos son obligatorios"\
                        } else {\
                            viewModel.vincularUsuario(linkingNombre, linkingCedula) { success ->\
                                if (!success) {\
                                    linkingError = "Error al vincular el usuario"\
                                }\
                            }\
                        }\
                    }\
                ) { Text("Vincular") }\
            },\
            dismissButton = {\
                TextButton(onClick = { viewModel.logout() }) { Text("Cancelar") }\
            }\
        )\
        return\
    }\
' app/src/main/java/com/example/ui/screens/TecnicableLoginScreen.kt
