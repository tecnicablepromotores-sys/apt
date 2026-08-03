package com.example

import com.example.ui.components.*
import com.example.ui.screens.*
import android.Manifest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import java.io.File
import androidx.core.content.FileProvider
import android.util.Log

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // NOTA: se eliminó el listener de FirebaseAuth.getInstance() (proyecto por
        // defecto). La app autentica contra el proyecto secundario "formularioWeb",
        // no contra el proyecto por defecto (que ni siquiera está inicializado sin
        // google-services.json). Esa línea causaba un crash al abrir la app y además
        // forzaba logout() en cada arranque. La sesión ya se restaura sola desde
        // SharedPreferences dentro de MainViewModel.

        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    TecnicableAppContent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TecnicableAppContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    if (!isLoggedIn) {
        TecnicableLoginScreen(viewModel = viewModel)
        return
    }

    val context = LocalContext.current
    val activePerfil by viewModel.activePerfil.collectAsState()
    val isAdmin = activePerfil.checkIsAdmin()

    val keyboardController = LocalSoftwareKeyboardController.current
    val state by viewModel.uiState.collectAsState()
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var activePhotoTarget by remember { mutableStateOf("cedula") } // "cedula" or "caja"
    var activeLocationTarget by remember { mutableStateOf("client") } // "client" or "caja"

    // Activity Contract Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            if (activePhotoTarget == "cedula") {
                viewModel.onCameraPhotoCaptured()
            } else {
                viewModel.onCameraPhotoCajaCaptured()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            if (activePhotoTarget == "cedula") {
                viewModel.onPhotoSelected(context, uri)
            } else {
                viewModel.onPhotoCajaSelected(context, uri)
            }
        }
    }

    // Permission Launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            if (activeLocationTarget == "client") {
                viewModel.captureLocation(context)
            } else {
                viewModel.captureLocationCaja(context)
            }
        } else {
            Toast.makeText(
                context,
                "Permiso de ubicación denegado. Active el GPS manualmente.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = viewModel.prepareCameraUri(context)
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(
                context,
                "Permiso de cámara denegado necesario para capturar el DNI.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Periodic live GPS tracker updater while app is open
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            while (true) {
                kotlinx.coroutines.delay(90_000L) // cada 1.5 minutos
                val hasFine = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                val hasCoarse = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    viewModel.updatePromoterLocation(context)
                }
            }
        }
    }

    // Dialog for photo source selection when clicking a Photo Card
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            title = {
                Text(
                    text = if (activePhotoTarget == "cedula") "Foto de la Cédula" else "Foto de la Caja / Instalación",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (activePhotoTarget == "cedula") {
                        "Seleccione el origen para cargar o capturar la foto de la cédula del cliente:"
                    } else {
                        "Seleccione el origen para cargar o capturar la foto de la caja a realizar la instalación:"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoSourceDialog = false
                        val hasCamPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasCamPermission) {
                            val uri = viewModel.prepareCameraUri(context)
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Cámara")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPhotoSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Galería")
                    }
                }
            }
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(310.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TecnicableBrandLogo(modifier = Modifier.padding(horizontal = 8.dp))
                    }
                    
                    HorizontalDivider(color = Color(0xFFC4C6D0).copy(alpha = 0.4f))
                    
                    Text(
                        text = "SISTEMA INTERNO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AddBox, contentDescription = null) },
                        label = { Text("Registrar Contrato", fontWeight = FontWeight.Bold) },
                        selected = state.activeScreen == AppScreen.REGISTRATION_FORM,
                        onClick = {
                            viewModel.navigateTo(AppScreen.REGISTRATION_FORM)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    if (isAdmin) {
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.Assignment, contentDescription = null) },
                            label = { Text("Contratos para Instalar", fontWeight = FontWeight.Bold) },
                            selected = state.activeScreen == AppScreen.CONTRATOS_DIARIOS,
                            onClick = {
                                viewModel.navigateTo(AppScreen.CONTRATOS_DIARIOS)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        NavigationDrawerItem(
                            icon = { Icon(Icons.Default.Badge, contentDescription = null) },
                            label = { Text("Contratos por Promotor", fontWeight = FontWeight.Bold) },
                            selected = state.activeScreen == AppScreen.CONTRATOS_POR_PROMOTOR,
                            onClick = {
                                viewModel.navigateTo(AppScreen.CONTRATOS_POR_PROMOTOR)
                                scope.launch { drawerState.close() }
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text("Censo de Interesados", fontWeight = FontWeight.Bold) },
                        selected = state.activeScreen == AppScreen.CENSO_PROSPECTOS,
                        onClick = {
                            viewModel.navigateTo(AppScreen.CENSO_PROSPECTOS)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Cloud, contentDescription = null) },
                        label = { Text("Contratos Web", fontWeight = FontWeight.Bold) },
                        selected = state.activeScreen == AppScreen.CONTRATOS_WEB,
                        onClick = {
                            viewModel.navigateTo(AppScreen.CONTRATOS_WEB)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )

                    HorizontalDivider(color = Color(0xFFC4C6D0).copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión", tint = Color(0xFFB01A1A)) },
                        label = { Text("Cerrar Sesión", fontWeight = FontWeight.Bold, color = Color(0xFFB01A1A)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.logout()
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedContainerColor = Color.Transparent,
                            unselectedTextColor = Color(0xFFB01A1A),
                            unselectedIconColor = Color(0xFFB01A1A)
                        )
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Tecnicable Mobile v2.0",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Margarita RIF J-30715323-7",
                            fontSize = 9.sp,
                            color = Color.Gray.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // App header
            TecnicableHeader(
                viewModel = viewModel,
                onMenuClick = {
                    scope.launch { drawerState.open() }
                }
            )

            // Real-time Web Registration Notification Banner
            val webNotificationBanner by viewModel.webNotificationBanner.collectAsState()
            if (webNotificationBanner != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable {
                            viewModel.navigateTo(AppScreen.CONTRATOS_WEB)
                            viewModel.dismissWebNotification()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = webNotificationBanner!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Haz clic para ver la solicitud web en la bandeja",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { viewModel.dismissWebNotification() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (state.activeScreen == AppScreen.REGISTRATION_FORM) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    FormularioContratoScreen(
                        viewModel = viewModel,
                        state = state,
                        onPhotoClick = {
                            activePhotoTarget = "cedula"
                            showPhotoSourceDialog = true
                        },
                        onPhotoCajaClick = {
                            activePhotoTarget = "caja"
                            showPhotoSourceDialog = true
                        },
                        onLocationClick = {
                            activeLocationTarget = "client"
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                viewModel.captureLocation(context)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onLocationCajaClick = {
                            activeLocationTarget = "caja"
                            val hasFine = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            val hasCoarse = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasFine || hasCoarse) {
                                viewModel.captureLocationCaja(context)
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        }
                    )
                }
            } else if (state.activeScreen == AppScreen.CONTRATOS_DIARIOS) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ContratosDiariosScreen(
                viewModel = viewModel,
                state = state,
                onNavigateToForm = { viewModel.navigateTo(AppScreen.REGISTRATION_FORM) }
            )
        }
    } else if (state.activeScreen == AppScreen.CONTRATOS_POR_PROMOTOR) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ContratosPorPromotorScreen(viewModel = viewModel)
        }
    } else if (state.activeScreen == AppScreen.CODIGOS_CLIENTES) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CodigosClientesScreen(viewModel = viewModel)
        }
    } else if (state.activeScreen == AppScreen.CENSO_PROSPECTOS) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CensoProspectosScreen(viewModel = viewModel)
        }
    } else if (state.activeScreen == AppScreen.CONTRATOS_WEB) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            ContratosWebScreen(viewModel = viewModel)
        }
    }
}
}
}

@Composable
fun TecnicableHeader(
    viewModel: MainViewModel,
    onMenuClick: () -> Unit
) {
    val activePerfil by viewModel.activePerfil.collectAsState()
    val allPerfiles by viewModel.allPerfiles.collectAsState()
    val allContratos by viewModel.allContratos.collectAsState()
    val allContratosWeb by viewModel.allContratosWeb.collectAsState()
    val censoProspectos by viewModel.censoProspectos.collectAsState()
    
    var showProfileDialog by remember { mutableStateOf(false) }
    
    if (showProfileDialog) {
        val localCtx = LocalContext.current
        val isAdmin = activePerfil.checkIsAdmin()
        
        var newName by remember { mutableStateOf("") }
        var newUsuario by remember { mutableStateOf("") }
        var newProfilePassword by remember { mutableStateOf("") }
        var newCedula by remember { mutableStateOf("") }
        var newCorreo by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf<String?>(null) }
        
        // Password change states
        var newPasswordInput by remember { mutableStateOf("") }
        var passwordError by remember { mutableStateOf<String?>(null) }
        
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isAdmin) Icons.Default.Settings else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isAdmin) "Administración y Seguridad" else "Mi Cuenta de Promotor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                var adminTab by remember { mutableStateOf(0) } // 0: Perfiles, 1: GPS, 2: Actividad, 3: Seguridad
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isAdmin) {
                        // Clean Segmented / Tab selector to avoid saturation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val tabs = listOf("👥 Perfiles", "🛰️ GPS", "📋 Actividad", "🔒 Clave")
                            tabs.forEachIndexed { idx, title ->
                                val selected = adminTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { adminTab = idx }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        when (adminTab) {
                            0 -> {
                                // Tab 0: Perfiles y Roles
                                Text(
                                    text = "Gestión de Perfiles y Roles:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    allPerfiles.forEach { perfil ->
                                        val isCurrent = perfil.id == activePerfil?.id
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            ),
                                            border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = perfil.nombre,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                        if (perfil.usuario.isNotBlank()) {
                                                            Text(
                                                                text = "C.I: ${perfil.cedula}",
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                        if (isCurrent) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text("Tú", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    
                                                    if (perfil.usuario != "tecnicable") {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = "Rol:",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = Color.Gray
                                                            )
                                                            val isPromotor = perfil.rol != "Administrador"
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isPromotor) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f))
                                                                    .clickable {
                                                                        viewModel.updatePerfilRol(perfil, "Promotor")
                                                                    }
                                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Promotor",
                                                                    color = if (isPromotor) Color.White else Color.DarkGray,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                            val isSelectedAdmin = perfil.rol == "Administrador"
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(6.dp))
                                                                    .background(if (isSelectedAdmin) Color(0xFFC2410C) else Color.LightGray.copy(alpha = 0.3f))
                                                                    .clickable {
                                                                        viewModel.updatePerfilRol(perfil, "Administrador")
                                                                    }
                                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                                            ) {
                                                                Text(
                                                                    text = "Administrador",
                                                                    color = if (isSelectedAdmin) Color.White else Color.DarkGray,
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    } else {
                                                        Text(
                                                            text = "Cargo: ${perfil.rol}",
                                                            fontSize = 11.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                
                                                if (allPerfiles.size > 1 && !isCurrent) {
                                                    IconButton(
                                                        onClick = { viewModel.deletePerfil(perfil) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Eliminar",
                                                            tint = Color.Red,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // Tab 1: Rastreo GPS en Tiempo Real
                                Text(
                                    text = "🛰️ Rastreo GPS de Promotores en Tiempo Real:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                val promotoresUbicacionMap by viewModel.promotoresUbicacion.collectAsState()

                                if (promotoresUbicacionMap.isEmpty()) {
                                    Text(
                                        text = "No hay ubicaciones GPS de promotores transmitiendo en este momento.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        promotoresUbicacionMap.values.forEach { ub ->
                                            val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(ub.timestamp))
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "📍 ${ub.nombre.ifBlank { ub.usuario }}",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = "Lat: ${String.format(java.util.Locale.US, "%.5f", ub.lat)}, Lon: ${String.format(java.util.Locale.US, "%.5f", ub.lon)}",
                                                            fontSize = 11.sp,
                                                            color = Color.DarkGray
                                                        )
                                                        Text(
                                                            text = "Actualizado: $timeStr",
                                                            fontSize = 10.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                    Button(
                                                        onClick = {
                                                            val uri = android.net.Uri.parse("geo:${ub.lat},${ub.lon}?q=${ub.lat},${ub.lon}(${ub.nombre})")
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                                            try {
                                                                localCtx.startActivity(intent)
                                                            } catch (e: Exception) {
                                                                Toast.makeText(localCtx, "No hay aplicación de mapas instalada", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        Text("Ver Mapa", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Tab 2: Actividad Reciente
                                Text(
                                    text = "Registro de Actividad Reciente:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )

                                val recentActivity = remember(allContratos, allContratosWeb, censoProspectos) {
                                    val combined = mutableListOf<Pair<Long, String>>()
                                    allContratos.forEach { c -> combined.add(c.timestamp to "Nuevo contrato: ${c.nombreCliente} (${c.tecnicoNombre})") }
                                    allContratosWeb.forEach { cw -> combined.add(cw.timestamp to "Nuevo contrato WEB: ${cw.nombreCliente}") }
                                    censoProspectos.forEach { cp -> combined.add(cp.timestamp to "Nuevo prospecto: ${cp.nombreCompleto} (${cp.usuarioGestor})") }
                                    
                                    combined.sortedByDescending { it.first }.take(30).map {
                                        val date = java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it.first))
                                        "[$date] ${it.second}"
                                    }
                                }

                                if (recentActivity.isEmpty()) {
                                    Text(
                                        text = "No hay actividad reciente registrada.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            recentActivity.forEach { log ->
                                                Row(
                                                    verticalAlignment = Alignment.Top,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                                    )
                                                    Text(
                                                        text = log,
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                                HorizontalDivider(modifier = Modifier.padding(top = 4.dp), color = Color.LightGray.copy(alpha = 0.4f))
                                            }
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // Tab 3: Seguridad / Clave Administrador
                                Text(
                                    text = "Seguridad de la Cuenta Administrador:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                OutlinedTextField(
                                    value = newPasswordInput,
                                    onValueChange = { newPasswordInput = it; passwordError = null },
                                    label = { Text("Nueva Contraseña de Administrador") },
                                    placeholder = { Text("Mínimo 4 caracteres") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("change_admin_password_input"),
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = PasswordVisualTransformation()
                                )
                                
                                if (passwordError != null) {
                                    Text(text = passwordError!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                
                                Button(
                                    onClick = {
                                        if (newPasswordInput.trim().length < 4) {
                                            passwordError = "La contraseña debe tener al menos 4 caracteres"
                                        } else {
                                            activePerfil?.let { p ->
                                                viewModel.updatePassword(p, newPasswordInput.trim()) { success, msg ->
                                                    if (success) {
                                                        newPasswordInput = ""
                                                    }
                                                    Toast.makeText(localCtx, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Guardar Nueva Contraseña", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        
                    } else {
                        // Standard User (Promotor) View - SIMPLE & Password change only
                        activePerfil?.let { p ->
                            // Section: User Details Info Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Datos de tu Cuenta:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Nombre:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                        Text(p.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cédula de Identidad:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                        Text(p.cedula, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Cargo:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                                        Text(p.rol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            // Section: Change password form
                            Text(
                                text = "Cambiar Mi Contraseña de Acceso:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            OutlinedTextField(
                                value = newPasswordInput,
                                onValueChange = { newPasswordInput = it; passwordError = null },
                                label = { Text("Nueva Contraseña de Acceso") },
                                placeholder = { Text("Mínimo 4 caracteres") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("change_user_password_input"),
                                shape = RoundedCornerShape(10.dp),
                                visualTransformation = PasswordVisualTransformation()
                            )
                            
                            if (passwordError != null) {
                                Text(text = passwordError!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                            
                            Button(
                                onClick = {
                                    if (newPasswordInput.trim().length < 4) {
                                        passwordError = "La contraseña debe tener al menos 4 caracteres"
                                    } else {
                                        viewModel.updatePassword(p, newPasswordInput.trim()) { success, msg ->
                                            if (success) {
                                                newPasswordInput = ""
                                            }
                                            Toast.makeText(localCtx, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().testTag("save_user_password_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Guardar Nueva Contraseña", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showProfileDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onMenuClick() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menú",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TecnicableBrandLogo()
            }

            // Firebase Realtime Sync Button
            val syncContext = LocalContext.current
            var isSyncingFirebase by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable(enabled = !isSyncingFirebase) {
                        isSyncingFirebase = true
                        viewModel.sincronizarClientesAFirebase { success, msg ->
                            isSyncingFirebase = false
                            Toast.makeText(syncContext, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSyncingFirebase) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                } else {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sincronizar Clientes a Firebase",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Notification Bell with Badge Count for Web Solicitudes
            val contratosWeb by viewModel.allContratosWeb.collectAsState()
            val pendingWebCount = remember(contratosWeb) { contratosWeb.count { it.estado == "Pendiente" } }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (pendingWebCount > 0) Color(0xFFFEE2E2) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { viewModel.navigateTo(AppScreen.CONTRATOS_WEB) }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notificaciones Web",
                        tint = if (pendingWebCount > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    if (pendingWebCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (pendingWebCount > 9) "9+" else "$pendingWebCount",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Right initials circle badge matching the unverified space-saving CircleAvatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary)
                    .clickable { showProfileDialog = true },
                contentAlignment = Alignment.Center
            ) {
                val initials = activePerfil?.nombre?.split(" ")?.mapNotNull { it.firstOrNull() }?.joinToString("")?.take(2)?.uppercase() ?: "TG"
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DatosInstalacionCard(
    state: UiState,
    onValueChange: (String) -> Unit
) {
    val errorText = if (state.errors.nroInstalacion?.contains("REPETIDO") == true) {
        state.errors.nroInstalacion
    } else if (state.isValidationTriggered && state.errors.nroInstalacion != null) {
        state.errors.nroInstalacion
    } else null
    val isError = errorText != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Código de Contrato / Instalación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Número de Contrato (Ingreso Manual) *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                OutlinedTextField(
                    value = state.fields.nroInstalacion,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nro_instalacion_input"),
                    placeholder = { Text("Ej: TC26-5541") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null
                        )
                    },
                    isError = isError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (isError) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DatosPersonalesCard(
    state: UiState,
    onNombreChange: (String) -> Unit,
    onCedulaPrefixChange: (String) -> Unit,
    onCedulaNumeroChange: (String) -> Unit,
    onFechaNacimientoChange: (String) -> Unit = {},
    onCorreoChange: (String) -> Unit,
    onTelefonoCelularChange: (String) -> Unit,
    onRepresentanteChange: (String) -> Unit,
    onCedulaRepChange: (String) -> Unit
) {
    val isJuridica = state.fields.cedulaPrefix == "J" || state.fields.cedulaPrefix == "G"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp), 
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isJuridica) "Datos de la Empresa (Persona Jurídica)" else "Datos Personales del Cliente",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Adaptive Banner Alert
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isJuridica) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f) 
                                     else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isJuridica) Icons.Default.Business else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isJuridica) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isJuridica) {
                            "Modo Persona Jurídica (J/G): Indique la Razón Social de la empresa, el RIF, y declare el Representante Legal autorizado."
                        } else {
                            "Modo Persona Natural (V/E): Recuerde que para contratos venezolanos, es obligatorio indicar un Nombre y un Apellido completo (Ej: Carlos Mendoza)."
                        },
                        fontSize = 12.sp,
                        color = if (isJuridica) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Nombre y Apellido / Razón Social
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (isJuridica) "Razón Social de la Empresa *" else "Nombres y Apellidos Completos *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                OutlinedTextField(
                    value = state.fields.nombre,
                    onValueChange = onNombreChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nombre_input"),
                    placeholder = { 
                        Text(if (isJuridica) "Ej: Tecnicable Digital C.A." else "Ej: Carlos Mendoza") 
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isJuridica) Icons.Default.Business else Icons.Default.AccountCircle,
                            contentDescription = null
                        )
                    },
                    isError = state.isValidationTriggered && state.errors.nombre != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.isValidationTriggered && state.errors.nombre != null) {
                    Text(
                        text = state.errors.nombre ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // If Persona Jurídica, show legal representative inputs here
            if (isJuridica) {
                // Representante Legal (Nombre y Apellido)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Representante Legal (Nombre y Apellido) *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = state.fields.representanteLegal,
                        onValueChange = onRepresentanteChange,
                        modifier = Modifier.fillMaxWidth().testTag("representante_input"),
                        placeholder = { Text("Ej: Andrés Gil Vega") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null
                            )
                        },
                        isError = state.isValidationTriggered && state.errors.representanteLegal != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (state.isValidationTriggered && state.errors.representanteLegal != null) {
                        Text(
                            text = state.errors.representanteLegal ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Cédula del Representante Legal
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Cédula del Representante Legal *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = state.fields.cedulaRepresentante,
                        onValueChange = onCedulaRepChange,
                        modifier = Modifier.fillMaxWidth().testTag("cedula_representante_input"),
                        placeholder = { Text("Ej: 15482390") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null
                            )
                        },
                        isError = state.isValidationTriggered && state.errors.cedulaRepresentante != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (state.isValidationTriggered && state.errors.cedulaRepresentante != null) {
                        Text(
                            text = state.errors.cedulaRepresentante ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            // Cédula de Identidad / RIF
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Cédula de Identidad y/o RIF *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prefix Toggle Selector (V / E / J / G)
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .width(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                val prefixes = listOf("V", "E", "J", "G")
                                val currentIndex = prefixes.indexOf(state.fields.cedulaPrefix).coerceAtLeast(0)
                                val nextPrefix = prefixes[(currentIndex + 1) % prefixes.size]
                                onCedulaPrefixChange(nextPrefix)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = state.fields.cedulaPrefix,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Cambiar tipo de documento",
                                tint = hintTintColors(),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Number Input
                    OutlinedTextField(
                        value = state.fields.cedulaNumero,
                        onValueChange = onCedulaNumeroChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("cedula_input"),
                        placeholder = { Text("Ej: 12345678") },
                        isError = state.isValidationTriggered && state.errors.cedula != null,
                        singleLine = true,
                        trailingIcon = {
                            if (state.isLookingUpCedula) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                if (state.lookupMessage != null) {
                    Text(
                        text = state.lookupMessage,
                        color = if (state.lookupMessage.contains("✓")) androidx.compose.ui.graphics.Color(0xFF16A34A) else MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                } else if (state.isValidationTriggered && state.errors.cedula != null) {
                    Text(
                        text = state.errors.cedula,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } else {
                    Text(
                        text = "Toque el cuadro [ V ] para rotar entre V, E, J, G.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Teléfono Celular
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Número de Celular *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                OutlinedTextField(
                    value = state.fields.telefonoCelular,
                    onValueChange = onTelefonoCelularChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("telefono_input"),
                    placeholder = { Text("Ej: 0412-1234567") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null
                        )
                    },
                    isError = state.isValidationTriggered && state.errors.telefonoCelular != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.isValidationTriggered && state.errors.telefonoCelular != null) {
                    Text(
                        text = state.errors.telefonoCelular,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Correo Electrónico
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Correo Electrónico *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                OutlinedTextField(
                    value = state.fields.correo,
                    onValueChange = onCorreoChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("correo_input"),
                    placeholder = { Text("Ej: cliente@ejemplo.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null
                        )
                    },
                    isError = state.isValidationTriggered && state.errors.correo != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.isValidationTriggered && state.errors.correo != null) {
                    Text(
                        text = state.errors.correo,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AccionesDeAltaSection(
    state: UiState,
    onPhotoClick: () -> Unit,
    onPhotoCajaClick: () -> Unit,
    onLocationClick: () -> Unit,
    onLocationCajaClick: () -> Unit,
    onRemovePhoto: () -> Unit,
    onRemovePhotoCaja: () -> Unit,
    onManualLocationSet: (Double, Double) -> Unit,
    onManualLocationCajaSet: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    var showManualGpsDialog by remember { mutableStateOf(false) }
    var showManualGpsCajaDialog by remember { mutableStateOf(false) }

    if (showManualGpsDialog) {
        var latInput by remember { mutableStateOf(state.fields.latitud?.toString() ?: "") }
        var lngInput by remember { mutableStateOf(state.fields.longitud?.toString() ?: "") }
        var inputError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showManualGpsDialog = false },
            title = {
                Text(
                    text = "🏡 Coordenadas Casa manuales",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingrese la latitud y la longitud de la casa del suscriptor de forma manual:",
                        fontSize = 13.sp
                    )
                    
                    OutlinedTextField(
                        value = latInput,
                        onValueChange = {
                            latInput = it
                            inputError = null
                        },
                        label = { Text("Latitud") },
                        placeholder = { Text("Ej: 10.9575") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lngInput,
                        onValueChange = {
                            lngInput = it
                            inputError = null
                        },
                        label = { Text("Longitud") },
                        placeholder = { Text("Ej: -63.8694") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (inputError != null) {
                        Text(
                            text = inputError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latNum = latInput.toDoubleOrNull()
                        val lngNum = lngInput.toDoubleOrNull()
                        if (latNum == null || lngNum == null) {
                            inputError = "Por favor ingrese valores numéricos válidos"
                        } else {
                            onManualLocationSet(latNum, lngNum)
                            showManualGpsDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualGpsDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showManualGpsCajaDialog) {
        var latCajaInput by remember { mutableStateOf(state.fields.latitudCaja?.toString() ?: "") }
        var lngCajaInput by remember { mutableStateOf(state.fields.longitudCaja?.toString() ?: "") }
        var inputCajaError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showManualGpsCajaDialog = false },
            title = {
                Text(
                    text = "🔌 Coordenadas Caja NAP manuales",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Ingrese la latitud y la longitud de la caja NAP de forma manual:",
                        fontSize = 13.sp
                    )
                    
                    OutlinedTextField(
                        value = latCajaInput,
                        onValueChange = {
                            latCajaInput = it
                            inputCajaError = null
                        },
                        label = { Text("Latitud") },
                        placeholder = { Text("Ej: 10.9575") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lngCajaInput,
                        onValueChange = {
                            lngCajaInput = it
                            inputCajaError = null
                        },
                        label = { Text("Longitud") },
                        placeholder = { Text("Ej: -63.8694") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (inputCajaError != null) {
                        Text(
                            text = inputCajaError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latNum = latCajaInput.toDoubleOrNull()
                        val lngNum = lngCajaInput.toDoubleOrNull()
                        if (latNum == null || lngNum == null) {
                            inputCajaError = "Por favor ingrese valores numéricos válidos"
                        } else {
                            onManualLocationCajaSet(latNum, lngNum)
                            showManualGpsCajaDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualGpsCajaDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row of 2 Photos (Cédula and Box/ONU/Installation)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Photo Soportes / Cédula (Left)
            val photoLoaded = state.fields.photoUri != null
            val photoBgColor = if (photoLoaded) Color(0xFFD8E2FF) else Color(0xFFE0E2EC)
            val photoBorderColor = if (photoLoaded) Color(0xFFADC6FF) else Color(0xFFC4C6D0)
            val photoBadgeBg = if (photoLoaded) Color(0xFF0061A4) else Color(0xFF44474E)
            val photoBadgeText = if (photoLoaded) "Cargada" else "Pendiente"
            val photoTextColor = if (photoLoaded) Color(0xFF001D36) else Color(0xFF44474E)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(116.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(photoBgColor)
                    .border(1.dp, photoBorderColor, RoundedCornerShape(20.dp))
                    .clickable { onPhotoClick() }
                    .testTag("action_photo_card"),
                contentAlignment = Alignment.Center
            ) {
                if (photoLoaded) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = state.fields.photoUri,
                            contentDescription = "Foto Cédula",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Cambiar Cédula", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Delete corner button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(alpha = 0.85f))
                                .clickable { onRemovePhoto() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("📷", fontSize = 24.sp)
                        Text(
                            text = "Foto Cédula",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = photoTextColor
                        )
                        Box(
                            modifier = Modifier
                                .background(photoBadgeBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(photoBadgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Photo de la Caja / ONU / Instalación (Right)
            val photoCajaLoaded = state.fields.photoCajaUri != null
            val photoCajaBgColor = if (photoCajaLoaded) Color(0xFFD8E2FF) else Color(0xFFE0E2EC)
            val photoCajaBorderColor = if (photoCajaLoaded) Color(0xFFADC6FF) else Color(0xFFC4C6D0)
            val photoCajaBadgeBg = if (photoCajaLoaded) Color(0xFF0061A4) else Color(0xFF44474E)
            val photoCajaBadgeText = if (photoCajaLoaded) "Cargada" else "Opcional"
            val photoCajaTextColor = if (photoCajaLoaded) Color(0xFF001D36) else Color(0xFF44474E)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(116.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(photoCajaBgColor)
                    .border(1.dp, photoCajaBorderColor, RoundedCornerShape(20.dp))
                    .clickable { onPhotoCajaClick() }
                    .testTag("action_photo_caja_card"),
                contentAlignment = Alignment.Center
            ) {
                if (photoCajaLoaded) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = state.fields.photoCajaUri,
                            contentDescription = "Foto Caja",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Cambiar Caja", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Delete corner button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(alpha = 0.85f))
                                .clickable { onRemovePhotoCaja() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("📦", fontSize = 24.sp)
                        Text(
                            text = "Foto de la Caja",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = photoCajaTextColor
                        )
                        Box(
                            modifier = Modifier
                                .background(photoCajaBadgeBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(photoCajaBadgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // GPS Action Box 1: Suscriptor (Underneath, full horizontal width)
        val gpsLoaded = state.fields.latitud != null && state.fields.longitud != null
        val gpsBgColor = if (gpsLoaded) Color(0xFFD8E2FF) else Color(0xFFE0E2EC)
        val gpsBorderColor = if (gpsLoaded) Color(0xFFADC6FF) else Color(0xFFC4C6D0)
        val gpsBadgeBg = if (gpsLoaded) Color(0xFF0061A4) else Color(0xFF44474E)
        val gpsBadgeText = if (gpsLoaded) "Capturada" else "Pendiente"
        val gpsTextColor = if (gpsLoaded) Color(0xFF001D36) else Color(0xFF44474E)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(gpsBgColor)
                .border(1.dp, gpsBorderColor, RoundedCornerShape(20.dp))
                .clickable { onLocationClick() }
                .testTag("action_gps_card"),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLocating) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF0061A4), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Obteniendo ubicación GPS Suscriptor...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("🏡", fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GPS del Suscriptor (Casa) *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = gpsTextColor
                        )
                        Text(
                            text = if (gpsLoaded) "Lat: ${state.fields.latitud}, Lng: ${state.fields.longitud}" else "Toque para capturar coordenadas de la Casa",
                            fontSize = 11.sp,
                            color = gpsTextColor.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(gpsBadgeBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(gpsBadgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showManualGpsDialog = true },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ingresar coordenadas manualmente", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isValidationTriggered && state.errors.ubicacion != null) {
            Text(
                text = state.errors.ubicacion,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // GPS Action Box 2: Caja NAP (Poste / Outdoor Box)
        val gpsCajaLoaded = state.fields.latitudCaja != null && state.fields.longitudCaja != null
        val gpsCajaBgColor = if (gpsCajaLoaded) Color(0xFFD8E2FF) else Color(0xFFE0E2EC)
        val gpsCajaBorderColor = if (gpsCajaLoaded) Color(0xFFADC6FF) else Color(0xFFC4C6D0)
        val gpsCajaBadgeBg = if (gpsCajaLoaded) Color(0xFF0061A4) else Color(0xFF44474E)
        val gpsCajaBadgeText = if (gpsCajaLoaded) "Capturada" else "Pendiente"
        val gpsCajaTextColor = if (gpsCajaLoaded) Color(0xFF001D36) else Color(0xFF44474E)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(gpsCajaBgColor)
                .border(1.dp, gpsCajaBorderColor, RoundedCornerShape(20.dp))
                .clickable { onLocationCajaClick() }
                .testTag("action_gps_caja_card"),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLocatingCaja) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = Color(0xFF0061A4), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Obteniendo ubicación GPS Caja NAP...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0061A4))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text("🔌", fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "GPS de la Caja NAP (Poste) *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = gpsCajaTextColor
                        )
                        Text(
                            text = if (gpsCajaLoaded) "Lat: ${state.fields.latitudCaja}, Lng: ${state.fields.longitudCaja}" else "Toque para capturar coordenadas de la Caja NAP",
                            fontSize = 11.sp,
                            color = gpsCajaTextColor.copy(alpha = 0.7f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(gpsCajaBadgeBg, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(gpsCajaBadgeText, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showManualGpsCajaDialog = true },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Ingresar coordenadas manualmente", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isValidationTriggered && state.errors.ubicacionCaja != null) {
            Text(
                text = state.errors.ubicacionCaja,
                color = MaterialTheme.colorScheme.error,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun UbicacionCard(
    state: UiState,
    onDireccionChange: (String) -> Unit,
    onPuntoReferenciaChange: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Ubicación de Instalación",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Dirección física
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Dirección Física Detallada *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                OutlinedTextField(
                    value = state.fields.direccionCompleta,
                    onValueChange = onDireccionChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .testTag("direccion_input"),
                    placeholder = { Text("Calle, Edf, Nro Apto...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null
                        )
                    },
                    isError = state.isValidationTriggered && state.errors.direccion != null,
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.isValidationTriggered && state.errors.direccion != null) {
                    Text(
                        text = state.errors.direccion,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Punto de Referencia
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Punto de Referencia *",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                OutlinedTextField(
                    value = state.fields.puntoReferencia,
                    onValueChange = onPuntoReferenciaChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("punto_referencia_input"),
                    placeholder = { Text("Ej: Frente a la plaza, casa color azul con portón blanco") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null
                        )
                    },
                    isError = state.isValidationTriggered && state.errors.puntoReferencia != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                if (state.isValidationTriggered && state.errors.puntoReferencia != null) {
                    Text(
                        text = state.errors.puntoReferencia,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // Coordenadas GPS display
            val hasCoords = state.fields.latitud != null && state.fields.longitud != null
            if (hasCoords) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFF0061A4).copy(alpha = 0.04f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = Color(0xFFADC6FF).copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF0061A4), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Coordenadas capturadas con éxito",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                        }

                        // Map Action button on the right
                        IconButton(
                            onClick = {
                                val mapUri = Uri.parse("geo:${state.fields.latitud},${state.fields.longitud}?q=${state.fields.latitud},${state.fields.longitud}(Instalación)")
                                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                                try {
                                    context.startActivity(mapIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF0061A4).copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = "Ver en Google Maps",
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "Latitud: ${state.fields.latitud}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF44474E)
                        )
                        Text(
                            text = "Longitud: ${state.fields.longitud}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF44474E)
                        )
                    }
                }
            } else if (state.isValidationTriggered && state.errors.ubicacion != null) {
                Text(
                    text = state.errors.ubicacion!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@Composable
fun PlanSelectionCard(
    state: UiState,
    planes: List<String>,
    onPlanSelected: (String) -> Unit,
    onTipoOnuSelected: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Tipo de Servicio y Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // 1. Selector de Equipamiento ONU
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Equipamiento ONU *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                val onuTypes = listOf("ONU SOLO INTERNET", "ONU INTERNET + TV")

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    onuTypes.forEach { item ->
                        val selected = state.fields.tipoOnu == item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) Color(0xFF0061A4) else Color(0xFFF0F4F9))
                                .border(1.dp, if (selected) Color(0xFF0061A4) else Color(0xFFC4C6D0), RoundedCornerShape(10.dp))
                                .clickable { onTipoOnuSelected(item) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else Color(0xFF1A1C1E)
                            )
                        }
                    }
                }
            }

            // 3. Planes de Velocidad de Internet
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Plan de Velocidad de Internet *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanButton(
                            title = "BÁSICO",
                            speed = "400 Mbps",
                            priceDivisas = "$30 / mes",
                            isSelected = state.fields.planSeleccionado.contains("400"),
                            onClick = { onPlanSelected("Plan Básico 400 Mbps (Tarifa US$ 30)") },
                            modifier = Modifier.weight(1f).testTag("plan_basico")
                        )
                        PlanButton(
                            title = "HOGAR",
                            speed = "600 Mbps",
                            priceDivisas = "$35 / mes",
                            isSelected = state.fields.planSeleccionado.contains("600"),
                            onClick = { onPlanSelected("Plan Hogar 600 Mbps (Tarifa US$ 35)") },
                            modifier = Modifier.weight(1f).testTag("plan_hogar")
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlanButton(
                            title = "TURBO",
                            speed = "800 Mbps",
                            priceDivisas = "$40 / mes",
                            isSelected = state.fields.planSeleccionado.contains("800"),
                            onClick = { onPlanSelected("Plan Turbo 800 Mbps (Tarifa US$ 40)") },
                            modifier = Modifier.weight(1f).testTag("plan_turbo")
                        )
                        PlanButton(
                            title = "VIP",
                            speed = "1 Gbps",
                            priceDivisas = "$45 / mes",
                            isSelected = state.fields.planSeleccionado.contains("1 Gbps") || state.fields.planSeleccionado.contains("VIP"),
                            onClick = { onPlanSelected("Plan VIP 1 Gbps (Tarifa US$ 45)") },
                            modifier = Modifier.weight(1f).testTag("plan_vip")
                        )
                    }
                }
            }

            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0061A4).copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF0061A4),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Servicio con Fibra Óptica simétrica de alta velocidad y soporte prioritario.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PlanButton(
    title: String,
    speed: String,
    priceDivisas: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFD8E2FF) else Color.White
    val borderColor = if (isSelected) Color(0xFF0061A4) else Color(0xFFC4C6D0)
    val borderStrokeWidth = if (isSelected) 2.dp else 1.dp
    val primaryColor = Color(0xFF0061A4)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(borderStrokeWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = speed,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C1E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Precio: $priceDivisas",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0061A4)
            )
        }
    }
}

@Composable
fun ValidationErrorBanner(errors: FormErrors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Alerta de validación",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = "Campos obligatorios requeridos:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                errors.nombre?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.cedula?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.fechaNacimiento?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.telefonoCelular?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.correo?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.direccion?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.puntoReferencia?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.montoPago?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.referenciaPago?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.photo?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.photoCaja?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
                errors.ubicacion?.let { Text(text = "• $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun IntegrationGuidePanel(viewModel: MainViewModel) {
    val activePerfil by viewModel.activePerfil.collectAsState()
    val isAdmin = activePerfil.checkIsAdmin()
    if (!isAdmin) return

    var expanded by remember { mutableStateOf(false) }
    var testEmail by remember { mutableStateOf("") }
    var testInProgress by remember { mutableStateOf(false) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var testResultSuccess by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Asistente de Integración de Backend",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Colapsar" else "Expandir",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Text(
                        text = "Esta aplicación móvil está diseñada para trabajar sin fricción en el campo laboral:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "1. Envío Nivel 1 (Mensajería WhatsApp): Al presionar el botón, el sistema genera de forma automática un mensaje con formato enriquecido (*negritas*, viñetas, emojis e hipervínculos de geolocalización) y adjunta la fotografía de la cédula de identidad del cliente directamente como pie de foto en el chat de WhatsApp.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Text(
                        text = "2. Integración Nivel 2 (Túnel HTTP Google Apps Script Bypass): Toda comunicación asíncrona hacia correo electrónico se despacha mediante infraestructura nativa y segura de Google de manera transparente empleando un túnel de reenvío HTTP sin depender del puerto SMTP local del teléfono.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    // Code Block GAS
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = """
                            // Estructura de Petición HTTP POST enviada al script
                            Endpoint: https://script.google.com/macros/s/AKfycbzNk-t.../exec
                            Payload JSON:
                            {
                              "token": "Tecnicable2026*SecureKey",
                              "para": "correo-del-cliente@gmail.com",
                              "asunto": "Notificación Automática de Sistema",
                              "cuerpo": "... Plantilla HTML de Ficha Digital ..."
                            }
                            """.trimIndent(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 12.sp
                        )
                    }

                    // Interactive Verification Console
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    Text(
                        text = "🧪 Panel de Pruebas de Envío de Email",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Pruebe el despliegue del Puente de Google de forma interactiva desde la app. Se despachará un reporte muestra.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = testEmail,
                            onValueChange = { testEmail = it },
                            placeholder = { Text("ejemplo@gmail.com", fontSize = 11.sp) },
                            label = { Text("Correo del Destinatario", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        val localContext = androidx.compose.ui.platform.LocalContext.current
                        Button(
                            onClick = {
                                if (testEmail.isBlank() || !testEmail.contains("@")) {
                                    testResultMsg = "Ingrese un correo electrónico válido"
                                    testResultSuccess = false
                                    return@Button
                                }
                                testInProgress = true
                                testResultMsg = null
                                
                                val customTestHtml = """
                                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.05);">
                                    <div style="background: linear-gradient(135deg, #4A154B 0%, #2C0B2E 100%); padding: 25px; text-align: center; color: white;">
                                        <h1 style="margin: 0; font-size: 22px; letter-spacing: 1px;">TECNICABLE - PRUEBA DE ENLACE</h1>
                                        <p style="margin: 5px 0 0 0; opacity: 0.8; font-size: 14px;">Túnel HTTP Apps Script Exitoso</p>
                                    </div>
                                    <div style="padding: 30px; color: #333333; line-height: 1.6;">
                                        <p style="font-size: 16px; margin-top: 0;">Estimado(a) <b>Colaborador de Tecnicable</b>,</p>
                                        <p>¡Felicitaciones! Este correo electrónico confirma que el puente de comunicación HTTP de Google está correctamente configurado y el sistema asíncrono está funcionando.</p>
                                        
                                        <table style="width: 100%; border-collapse: collapse; margin: 20px 0; font-size: 14px;">
                                            <tr style="background-color: #f9f9f9;">
                                                <td style="padding: 10px; border: 1px solid #eeeeee; font-weight: bold; width: 30%;">Canal:</td>
                                                <td style="padding: 10px; border: 1px solid #eeeeee;">Túnel de Redirección HTTP</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 10px; border: 1px solid #eeeeee; font-weight: bold;">Token:</td>
                                                <td style="padding: 10px; border: 1px solid #eeeeee; font-family: monospace; color: #4A154B;">Tecnicable2026*SecureKey</td>
                                            </tr>
                                        </table>
                                        
                                        <div style="background-color: #fff8f8; border-left: 4px solid #4A154B; padding: 12px; margin: 20px 0; font-size: 13px; color: #555;">
                                            <b>Diagnóstico:</b> Solicitud web procesada limpiamente con OkHttp desde el terminal Android y enviada exitosamente por bypass de red en la nube.
                                        </div>
                                    </div>
                                    <div style="background-color: #f5f5f5; padding: 15px; text-align: center; font-size: 11px; color: #777777; border-top: 1px solid #eeeeee;">
                                        &copy; 2026 Tecnicable C.A. Todos los derechos reservados.
                                    </div>
                                </div>
                                """.trimIndent()

                                viewModel.sendEmailSMTP(
                                    recipient = testEmail.trim(),
                                    subject = "Tecnicable - Prueba de Enlace de Google Apps Script",
                                    bodyHtml = customTestHtml
                                ) { success, msg ->
                                    testInProgress = false
                                    testResultSuccess = success
                                    testResultMsg = msg
                                }
                            },
                            enabled = !testInProgress,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A154B)),
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (testInProgress) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Probar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    testResultMsg?.let { msg ->
                        Text(
                            text = if (testResultSuccess) "✅ $msg" else "❌ $msg",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (testResultSuccess) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }

                    Text(
                        text = "3. Estándar de Privacidad (GDPR/DNI): La fotografía de la cédula del cliente se maneja enteramente en la sandbox privada del almacenamiento local de la app. Al depararse por la red, las imágenes se codifican directamente en formato Base64 comprimido y seguro, viajando exclusivamente mediante canales HTTPS encriptados.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

fun hasAnyErrors(errors: FormErrors): Boolean {
    return errors.nombre != null ||
            errors.cedula != null ||
            errors.photo != null ||
            errors.direccion != null ||
            errors.ubicacion != null ||
            errors.correo != null ||
            errors.telefonoCelular != null ||
            errors.montoPago != null ||
            errors.referenciaPago != null ||
            errors.puntoReferencia != null
}

private fun sendWhatsAppIntent(
    context: Context,
    messageText: String,
    attachmentUri: Uri?
) {
    val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (attachmentUri != null) "image/jpeg" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, messageText)
        if (attachmentUri != null) {
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setPackage("com.whatsapp")
    }

    val whatsappBusinessIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (attachmentUri != null) "image/jpeg" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, messageText)
        if (attachmentUri != null) {
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        setPackage("com.whatsapp.w4b")
    }

    val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (attachmentUri != null) "image/jpeg" else "text/plain"
        putExtra(Intent.EXTRA_TEXT, messageText)
        if (attachmentUri != null) {
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    try {
        context.startActivity(whatsappIntent)
    } catch (e: Exception) {
        try {
            context.startActivity(whatsappBusinessIntent)
        } catch (ex: Exception) {
            try {
                val chooserIntent = Intent.createChooser(fallbackIntent, "Compartir registro via WhatsApp o Mensajería")
                context.startActivity(chooserIntent)
            } catch (e3: Exception) {
                Toast.makeText(context, "No se encontró ninguna aplicación compatible para enviar.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun MetodoPagoCard(
    state: UiState,
    onMetodoSelected: (String) -> Unit,
    onMontoChange: (String) -> Unit,
    onReferenciaChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Método de Pago",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            // Selector Chips - 2 accessible lines
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Line 1: Divisas, Bolívares
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val line1Options = listOf("Divisas", "Bolívares")
                    line1Options.forEach { option ->
                        val isSelected = if (option == "Bolívares") {
                            state.fields.metodoPago.startsWith("Bolívares")
                        } else {
                            state.fields.metodoPago == option
                        }
                        MetodoPagoChip(
                            option = option,
                            isSelected = isSelected,
                            onClick = {
                                if (option == "Bolívares") {
                                    onMetodoSelected("Bolívares (Pago Móvil)")
                                } else {
                                    onMetodoSelected(option)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Line 2: Zelle, PayPal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val line2Options = listOf("Zelle", "PayPal")
                    line2Options.forEach { option ->
                        MetodoPagoChip(
                            option = option,
                            isSelected = state.fields.metodoPago == option,
                            onClick = { onMetodoSelected(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (state.fields.metodoPago.startsWith("Bolívares")) {
                val subSelectedIsPagoMovil = state.fields.metodoPago.contains("Pago Móvil")
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Tipo de pago en Bolívares *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pago Móvil Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (subSelectedIsPagoMovil) Color(0xFFD8E2FF) else Color(0xFFF4F5F8))
                                .border(if (subSelectedIsPagoMovil) 2.dp else 1.dp, if (subSelectedIsPagoMovil) Color(0xFF0061A4) else Color(0xFFC4C6D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onMetodoSelected("Bolívares (Pago Móvil)") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (subSelectedIsPagoMovil) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0061A4))
                                }
                                Text(
                                    text = "Pago Móvil",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (subSelectedIsPagoMovil) Color(0xFF0061A4) else Color(0xFF44474E)
                                )
                            }
                        }
                        
                        // Efectivo Option
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (!subSelectedIsPagoMovil) Color(0xFFD8E2FF) else Color(0xFFF4F5F8))
                                .border(if (!subSelectedIsPagoMovil) 2.dp else 1.dp, if (!subSelectedIsPagoMovil) Color(0xFF0061A4) else Color(0xFFC4C6D0).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { onMetodoSelected("Bolívares (Efectivo)") },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (!subSelectedIsPagoMovil) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF0061A4))
                                }
                                Text(
                                    text = "Efectivo Bs.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (!subSelectedIsPagoMovil) Color(0xFF0061A4) else Color(0xFF44474E)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dynamic Input Fields based on the selected method
            val currentMethod = state.fields.metodoPago
            val isDollars = currentMethod == "Divisas" || currentMethod == "Zelle" || currentMethod == "PayPal"
            val needsReference = currentMethod.contains("Pago Móvil") || currentMethod == "Zelle" || currentMethod == "PayPal"

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount Field
                val montoLabel = if (isDollars) "Monto Cobrado (Dólares US$) *" else "Monto Cobrado (Bolívares Bs.) *"
                val montoPlaceholder = if (isDollars) "Ej: 30" else "Ej: 1100"
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = montoLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    OutlinedTextField(
                        value = state.fields.montoPago,
                        onValueChange = onMontoChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monto_pago_input"),
                        placeholder = { Text(montoPlaceholder) },
                        leadingIcon = {
                            Text(
                                text = if (isDollars) "$" else "Bs.",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        isError = state.isValidationTriggered && state.errors.montoPago != null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = if (needsReference) ImeAction.Next else ImeAction.Done
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (state.isValidationTriggered && state.errors.montoPago != null) {
                        Text(
                            text = state.errors.montoPago!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // Reference Field (Pago Móvil, Zelle, PayPal)
                if (needsReference) {
                    val refLabel = "Número de Referencia ($currentMethod) *"
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = refLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        OutlinedTextField(
                            value = state.fields.referenciaPago,
                            onValueChange = onReferenciaChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("referencia_pago_input"),
                            placeholder = { Text("Ej: 48923948") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ConfirmationNumber,
                                    contentDescription = null
                                )
                            },
                            isError = state.isValidationTriggered && state.errors.referenciaPago != null,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        if (state.isValidationTriggered && state.errors.referenciaPago != null) {
                            Text(
                                text = state.errors.referenciaPago!!,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetodoPagoChip(
    option: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFD8E2FF) else Color.White
    val borderColor = if (isSelected) Color(0xFF0061A4) else Color(0xFFC4C6D0)
    val borderStrokeWidth = if (isSelected) 2.dp else 1.dp
    val primaryColor = Color(0xFF0061A4)

    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(borderStrokeWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("metodo_pago_${option.lowercase().replace("ó", "o").replace("í", "i").replace(" ", "_")}"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) primaryColor else Color(0xFF1A1C1E)
        )
    }
}

private fun sendEmailIntent(
    context: Context,
    recipient: String,
    subject: String,
    body: String,
    attachmentUri: Uri?
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (attachmentUri != null) "image/jpeg" else "text/plain"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        
        if (attachmentUri != null) {
            putExtra(Intent.EXTRA_STREAM, attachmentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    try {
        val chooserIntent = Intent.createChooser(intent, "Seleccione un cliente de correo para Tecnicable")
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        try {
            val fallbackIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipient")
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(fallbackIntent)
            Toast.makeText(context, "Se abrió el cliente de correo. Adjunte la foto manualmente si el cliente de correo no la cargó automáticamente.", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(context, "No se encontró ningún cliente de correo instalado en el sistema.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun hintTintColors() = LocalContentColor.current.copy(alpha = 0.45f)

@Composable
fun SignatureCard(
    state: UiState,
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val completedStrokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke = remember { mutableStateListOf<Offset>() }
    
    // Track if signature has been stored
    val isSaved = state.fields.signatureUri != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC4C6D0).copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Gesture,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Firma Digital Autorizada *",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Solicite al suscriptor plasmar su firma digital directamente en el recuadro para certificar la solicitud de instalación y aceptación de términos:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                lineHeight = 16.sp
            )

            if (!isSaved) {
                // Interactive Signature Draw Board
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF1F4FA))
                        .border(1.dp, Color(0xFFC4C6D0), RoundedCornerShape(16.dp))
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentStroke.add(offset)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentStroke.add(change.position)
                                    },
                                    onDragEnd = {
                                        if (currentStroke.isNotEmpty()) {
                                            completedStrokes.add(currentStroke.toList())
                                            // Reset current stroke gracefully
                                            currentStroke = mutableStateListOf()
                                        }
                                    }
                                )
                            }
                    ) {
                        // Draw completed paths
                        for (stroke in completedStrokes) {
                            if (stroke.size > 1) {
                                val path = Path().apply {
                                    moveTo(stroke[0].x, stroke[0].y)
                                    for (i in 1 until stroke.size) {
                                        lineTo(stroke[i].x, stroke[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(
                                        width = 6f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                        // Draw active path
                        if (currentStroke.size > 1) {
                            val path = Path().apply {
                                moveTo(currentStroke[0].x, currentStroke[0].y)
                                for (i in 1 until currentStroke.size) {
                                    lineTo(currentStroke[i].x, currentStroke[i].y)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color.Blue,
                                style = Stroke(
                                    width = 6f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (completedStrokes.isEmpty() && currentStroke.isEmpty()) {
                        Text(
                            text = "Plasmase la firma aquí usando su dedo",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Row of Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Button 1: Limpiar / Clear
                    OutlinedButton(
                        onClick = {
                            completedStrokes.clear()
                            currentStroke.clear()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Limpiar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    // Button 2: Guardar / Save Signature
                    Button(
                        onClick = {
                            if (completedStrokes.isEmpty()) {
                                Toast.makeText(context, "Por favor, firme la pantalla antes de guardar.", Toast.LENGTH_SHORT).show()
                            } else {
                                // Save Strokes to PNG bitmap
                                val signatureUri = saveSignatureBitmap(
                                    context = context,
                                    strokes = completedStrokes,
                                    width = 600,
                                    height = 300
                                )
                                if (signatureUri != null) {
                                    viewModel.onSignatureSaved(signatureUri)
                                    Toast.makeText(context, "¡Firma registrada exitosamente!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Error al generar imagen de firma.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guardar Firma", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Signature Captured State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8F5E9), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFA5D6A7), RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Text("Firma Guardada con Éxito", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }

                    // Render preview of saved signature image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFC4C6D0), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = state.fields.signatureUri,
                            contentDescription = "Vista Previa Firma",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    TextButton(
                        onClick = {
                            completedStrokes.clear()
                            currentStroke.clear()
                            viewModel.removeSignature()
                        }
                    ) {
                        Text("Reestablecer / Volver a Firmar", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (state.isValidationTriggered && state.errors.firma != null) {
                Text(
                    text = state.errors.firma,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

fun saveSignatureBitmap(context: Context, strokes: List<List<androidx.compose.ui.geometry.Offset>>, width: Int, height: Int): Uri? {
    try {
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 6f
            isAntiAlias = true
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        
        for (stroke in strokes) {
            if (stroke.isEmpty()) continue
            val path = android.graphics.Path()
            path.moveTo(stroke[0].x, stroke[0].y)
            for (i in 1 until stroke.size) {
                path.lineTo(stroke[i].x, stroke[i].y)
            }
            canvas.drawPath(path, paint)
        }
        
        val file = File(context.cacheDir, "client_signature_${System.currentTimeMillis()}.png")
        file.outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        
        val authority = "${context.packageName}.fileprovider"
        return androidx.core.content.FileProvider.getUriForFile(context, authority, file)
    } catch(e: Exception) {
        android.util.Log.e("SignatureSave", "Error transforming strokes to bitmap file", e)
        return null
    }
}

