package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppScreen
import com.example.ContratoDiario
import com.example.MainViewModel
import com.example.PdfGenerator
import com.example.UiState
import com.example.checkIsAdmin
import com.example.ui.components.*

@Composable
fun ContratosDiariosScreen(
    viewModel: MainViewModel,
    state: UiState,
    onNavigateToForm: () -> Unit
) {
    val context = LocalContext.current
    val contratos by viewModel.allContratos.collectAsStateWithLifecycle()
    
    // Zoom photo full-screen backup previewer state
    var zoomPhoto by remember { mutableStateOf<Any?>(null) }
    
    // Search query state
    var searchQuery by remember { mutableStateOf("") }
    
    // Time filter state ("Día", "Semana", "Mes", "Todos")
    var selectedTimeFilter by remember { mutableStateOf("Día") }
    
    // Metric stats visibility state
    var showMetrics by remember { mutableStateOf(false) }
    
    // Collapsible filters panel visibility state (retracted by default)
    var showFiltersPanel by remember { mutableStateOf(false) }
    
    // Selected day/date to filter by when selectedTimeFilter == "Día"
    var selectedDateForFilter by remember {
        mutableStateOf(
            try {
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            } catch (e: Exception) {
                ""
            }
        )
    }

    val calendar = java.util.Calendar.getInstance()
    val datePickerDialog = remember(context) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                selectedDateForFilter = try {
                    java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(cal.time)
                } catch (e: Exception) {
                    ""
                }
            },
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH),
            calendar.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    // Modal dialog state for confirmation of Daily Closure
    var showClosureConfirmDialog by remember { mutableStateOf(false) }
    
    val activePerfil by viewModel.activePerfil.collectAsStateWithLifecycle()
    val isAdmin = activePerfil.checkIsAdmin()

    if (!isAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "🔒 Módulo Exclusivo para Administrador",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "El apartado de Contratos Diarios y Verificación de Cierre de Caja está disponible únicamente para el administrador (luifred1998@gmail.com).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    var showGlobalByUserPreference by remember { mutableStateOf(true) }
    
    // Filtered list using calendar calculations
    val timeFiltered = remember(contratos, selectedTimeFilter, selectedDateForFilter, activePerfil, showGlobalByUserPreference) {
        val currentCal = java.util.Calendar.getInstance()
        val currentDay = currentCal.get(java.util.Calendar.DAY_OF_YEAR)
        val currentWeek = currentCal.get(java.util.Calendar.WEEK_OF_YEAR)
        val currentMonth = currentCal.get(java.util.Calendar.MONTH)
        val currentYear = currentCal.get(java.util.Calendar.YEAR)
        
        // Filter by user permissions
        val userFiltered = contratos.filter { item ->
            if (activePerfil == null) {
                true
            } else if (isAdmin) {
                if (showGlobalByUserPreference) {
                    true
                } else {
                    item.tecnicoNombre.trim().equals(activePerfil?.nombre?.trim(), ignoreCase = true)
                }
            } else {
                item.tecnicoNombre.trim().equals(activePerfil?.nombre?.trim(), ignoreCase = true)
            }
        }
        
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

        val timeFiltered = userFiltered.filter { item ->
            val dateStr = item.fecha.trim()
            if (dateStr.isBlank()) {
                true
            } else {
                try {
                    val parsedDate = sdf.parse(dateStr)
                    if (parsedDate != null) {
                        val itemCal = java.util.Calendar.getInstance()
                        itemCal.time = parsedDate
                        
                        val itemDay = itemCal.get(java.util.Calendar.DAY_OF_YEAR)
                        val itemWeek = itemCal.get(java.util.Calendar.WEEK_OF_YEAR)
                        val itemMonth = itemCal.get(java.util.Calendar.MONTH)
                        val itemYear = itemCal.get(java.util.Calendar.YEAR)
                        
                        when (selectedTimeFilter) {
                            "Día" -> {
                                if (selectedDateForFilter.isNotBlank()) {
                                    val selDateParsed = sdf.parse(selectedDateForFilter)
                                    if (selDateParsed != null) {
                                        val selCal = java.util.Calendar.getInstance()
                                        selCal.time = selDateParsed
                                        itemDay == selCal.get(java.util.Calendar.DAY_OF_YEAR) && itemYear == selCal.get(java.util.Calendar.YEAR)
                                    } else {
                                        itemDay == currentDay && itemYear == currentYear
                                    }
                                } else {
                                    itemDay == currentDay && itemYear == currentYear
                                }
                            }
                            "Semana" -> itemWeek == currentWeek && itemYear == currentYear
                            "Mes" -> itemMonth == currentMonth && itemYear == currentYear
                            else -> true
                        }
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            }
        }

        timeFiltered
    }

    var selectedTabFilter by remember { mutableStateOf("Por Instalar") }

    val pendingCount = remember(timeFiltered) { timeFiltered.count { !it.cerrado } }
    val completedCount = remember(timeFiltered) { timeFiltered.count { it.cerrado } }

    val filteredContratos = remember(timeFiltered, searchQuery, selectedTabFilter) {
        var list = timeFiltered.filter { item ->
            searchQuery.isBlank() ||
            item.nombreCliente.contains(searchQuery, ignoreCase = true) ||
            item.cedula.contains(searchQuery, ignoreCase = true) ||
            item.nroInstalacion.contains(searchQuery, ignoreCase = true) ||
            item.tecnicoNombre.contains(searchQuery, ignoreCase = true) ||
            item.plan.contains(searchQuery, ignoreCase = true)
        }

        when (selectedTabFilter) {
            "Por Instalar" -> list = list.filter { !it.cerrado }
            "Completados" -> list = list.filter { it.cerrado }
        }

        list.sortedByDescending { it.timestamp }
    }

    val totalRecaudadoUsd = remember(filteredContratos) {
        filteredContratos.sumOf { it.monto.toDoubleOrNull() ?: 0.0 }
    }

    if (showClosureConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClosureConfirmDialog = false },
            title = { Text("Confirmar Cierre de Caja", fontWeight = FontWeight.Bold) },
            text = { Text("¿Desea marcar los ${filteredContratos.size} contratos filtrados como consolidados/cerrados?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.consolidateDailyClosure(filteredContratos)
                        showClosureConfirmDialog = false
                        Toast.makeText(context, "¡Cierre de caja consolidado con éxito!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Confirmar Cierre", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClosureConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Contratos y Cierre Diario",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Gestión de contratos e instalaciones",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showMetrics = !showMetrics }) {
                Icon(
                    imageVector = if (showMetrics) Icons.Default.VisibilityOff else Icons.Default.Analytics,
                    contentDescription = "Métricas",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        TecnicableSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            hint = "Buscar por cliente, cédula o instalación..."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Día", "Semana", "Mes", "Todos").forEach { filter ->
                    val isSelected = selectedTimeFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTimeFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) }
                    )
                }
            }

            if (selectedTimeFilter == "Día") {
                TextButton(onClick = { datePickerDialog.show() }) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(selectedDateForFilter.ifBlank { "Elegir fecha" }, fontSize = 11.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedTabFilter == "Por Instalar",
                onClick = { selectedTabFilter = "Por Instalar" },
                label = { Text("Por Instalar ($pendingCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFEF3C7),
                    selectedLabelColor = Color(0xFFB45309),
                    selectedLeadingIconColor = Color(0xFFB45309)
                )
            )
            FilterChip(
                selected = selectedTabFilter == "Completados",
                onClick = { selectedTabFilter = "Completados" },
                label = { Text("Completados ($completedCount)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFD1FAE5),
                    selectedLabelColor = Color(0xFF047857),
                    selectedLeadingIconColor = Color(0xFF047857)
                )
            )
            FilterChip(
                selected = selectedTabFilter == "Todos",
                onClick = { selectedTabFilter = "Todos" },
                label = { Text("Todos (${timeFiltered.size})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold) }
            )
        }

        AnimatedVisibility(visible = showMetrics) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total Contratos", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${filteredContratos.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("Recaudado Estimado", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$$totalRecaudadoUsd USD", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                    Button(
                        onClick = { showClosureConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Cierre Caja", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filteredContratos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Text("No hay contratos registrados para este filtro", color = Color.Gray, fontSize = 14.sp)
                    Button(onClick = onNavigateToForm) {
                        Text("Registrar Nuevo Contrato")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = filteredContratos, key = { it.nroInstalacion }) { contrato ->
                    ContratoDiarioCard(
                        contrato = contrato,
                        onDeleteClick = { viewModel.deleteContract(contrato) },
                        onTelegramClick = {
                            viewModel.sendTelegramReport(contrato)
                            Toast.makeText(context, "Enviando reporte Telegram...", Toast.LENGTH_SHORT).show()
                        },
                        onViewFullscreen = { zoomPhoto = it },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun ContractImageThumbnail(
    title: String,
    uriStr: String?,
    base64Str: String?,
    onViewFullscreen: (Any) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable {
                val target = if (!uriStr.isNull_or_blank()) uriStr else base64Str
                if (target != null) onViewFullscreen(target)
            }
            .padding(4.dp)
    ) {
        Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            if (!uriStr.isNull_or_blank() || !base64Str.isNull_or_blank()) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Sin imagen",
                    modifier = Modifier.size(18.dp),
                    tint = Color.LightGray
                )
            }
        }
        Text(
            text = if (!uriStr.isNull_or_blank() || !base64Str.isNull_or_blank()) "Ver foto" else "Sin registro",
            fontSize = 7.5.sp,
            color = if (!uriStr.isNull_or_blank() || !base64Str.isNull_or_blank()) MaterialTheme.colorScheme.primary else Color.LightGray,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.isBlank()

fun generateEmailMessageForContract(c: ContratoDiario): String {
    return """
    =======================================================
    REGISTRO DE ALTA DE INSTALACIÓN - TECNICABLE
    =======================================================
    
    INFORMACIÓN DEL CONTRATO:
    - Nro de Instalación: ${c.nroInstalacion}
    - Fecha de Registro: ${c.fecha}
    
    DATOS DEL SUSCRIPTOR / CLIENTE:
    - Nombres y Apellido: ${c.nombreCliente}
    - Cédula de Identidad: ${c.cedula}
    - Teléfono Celular: ${c.celular}
    - Correo Electrónico: ${c.correo.ifBlank { "No registrado" }}
    
    DETALLES DE UBICACIÓN Y DIRECCIÓN:
    - Dirección Física: ${c.direccion}
    - Punto de Referencia: ${c.puntoReferencia.ifBlank { "Ninguno" }}
    - Coordenadas GPS Abonado: ${c.latitud ?: 0.0}, ${c.longitud ?: 0.0}
    - Coordenadas GPS Caja NAP: ${c.latitudCaja ?: 0.0}, ${c.longitudCaja ?: 0.0}
    
    PLAN DE INTERNET CONTRATADO:
    - Plan del Servicio: ${c.plan}
    - Forma de Pago: ${c.metodoPago}
    - Monto Cancelado: ${c.monto} USD
    
    FIRMA DIGITAL CERTIFICADA:
    - Estado de la Firma: REGISTRADA Y ACEPTADA BAJO CONSENTIMIENTO
    - Mensaje Legal: Se ha recibido la firma electrónica autorizando la activación y aceptación del servicio.
    
    =======================================================
    Soporte Técnico de Operaciones y Promotores Margarita.
    =======================================================
    """.trimIndent()
}

fun generateWhatsAppMessageForContract(c: ContratoDiario): String {
    return """
    🔴 *TECNICABLE - NUEVOS SERVICIOS GPON INTERNET*
    ========================================
    *FICHA DE INSTALACIÓN Y REGISTRO DE CONTRATO*
    ========================================
    
    *ℹ️ INFORMACIÓN DEL CONTRATO:*
    ----------------------------------------
    • *Nro de Instalación:* ${c.nroInstalacion}
    • *Fecha:* ${c.fecha}
    
    *👤 DATOS DEL SUSCRIPTOR:*
    ----------------------------------------
    • *Nombres:* ${c.nombreCliente}
    • *Cédula:* ${c.cedula}
    • *Celular:* ${c.celular}
    
    *📍 DETALLES DE DIRECCIÓN:*
    ----------------------------------------
    • *Física:* ${c.direccion}
    • *Pto Referencia:* ${c.puntoReferencia}
    
    *⚡ PLAN DE CONTEXTO:*
    ----------------------------------------
    • Plan seleccionado: ${c.plan}
    
    *💵 PARÁMETROS ADMINISTRATIVOS:*
    ----------------------------------------
    • Método de Pago: ${c.metodoPago}
    • Monto Cancelado: ${c.monto} USD
    
    ----------------------------------------
     *🖋️ FIRMA DIGITAL DEL CLIENTE*
    ----------------------------------------
    • *Estado de Firma:* [✓] Certificada y Aceptada por el suscriptor
    • *Términos:* Aceptación de términos de activación de fibra óptica.
    
    _Re-enviado desde Tecnicable Mobile Engine 2026_
    """.trimIndent()
}

@Composable
fun ContratoDiarioCard(
    contrato: ContratoDiario,
    onDeleteClick: () -> Unit,
    onTelegramClick: () -> Unit,
    onViewFullscreen: (Any) -> Unit,
    viewModel: MainViewModel
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(false) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Registro", fontWeight = FontWeight.Bold) },
            text = { Text("¿Está seguro de que desea eliminar este contrato de ${contrato.nombreCliente}? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(if (contrato.cerrado) Color(0xFF10B981) else Color(0xFFF59E0B))
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text(
                            text = contrato.nombreCliente.uppercase(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Instalación: ${contrato.nroInstalacion} • C.I: ${contrato.cedula}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (contrato.cerrado) Color(0xFFECFDF5) else Color(0xFFFFFBEB))
                            .border(1.dp, if (contrato.cerrado) Color(0xFFA7F3D0) else Color(0xFFFDE68A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(if (contrato.cerrado) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Text(
                                text = if (contrato.cerrado) "CONSOLIDADO" else "PENDIENTE",
                                color = if (contrato.cerrado) Color(0xFF047857) else Color(0xFFB45309),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PLAN DEL SERVICIO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contrato.plan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "FACTURACIÓN",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${contrato.monto} - ${contrato.metodoPago}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1.1f)) {
                                Text(
                                    text = "RESPONSABLE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = contrato.tecnicoNombre,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("TELÉFONO CLIENTE", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(contrato.celular.ifBlank { "No registrado" }, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            Column(modifier = Modifier.weight(1.2f)) {
                                Text("REGISTRADO EN SISTEMA", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(contrato.fecha, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("DIRECCIÓN Y REFERENCIA", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = buildString {
                                    append(contrato.direccion.ifBlank { "Sin dirección física grabada" })
                                    if (contrato.puntoReferencia.isNotBlank()) {
                                        append(" / Ref: ")
                                        append(contrato.puntoReferencia)
                                    }
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 14.sp
                            )
                            
                            val userCoords = if (contrato.latitud != null && contrato.longitud != null) {
                                String.format(java.util.Locale.getDefault(), "%.5f, %.5f", contrato.latitud, contrato.longitud)
                            } else "Sin GPS"
                            val boxCoords = if (contrato.latitudCaja != null && contrato.longitudCaja != null) {
                                String.format(java.util.Locale.getDefault(), "%.5f, %.5f", contrato.latitudCaja, contrato.longitudCaja)
                            } else "Sin GPS"

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("GPS Abonado: $userCoords", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                                Text("GPS Caja NAP: $boxCoords", fontSize = 9.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }
                        
                        Text(
                            text = "REGISTRO FOTOGRÁFICO Y COMPROBANTES",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0A4E9B),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ContractImageThumbnail(
                                title = "Doc Cliente",
                                uriStr = contrato.fotoClientUri,
                                base64Str = contrato.fotoClientBase64,
                                onViewFullscreen = onViewFullscreen
                            )
                            ContractImageThumbnail(
                                title = "Caja / Poste",
                                uriStr = contrato.fotoCajaUri,
                                base64Str = contrato.fotoCajaBase64,
                                onViewFullscreen = onViewFullscreen
                            )
                            ContractImageThumbnail(
                                title = "Firma Digital",
                                uriStr = contrato.firmaUri,
                                base64Str = contrato.firmaBase64,
                                onViewFullscreen = onViewFullscreen
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isExpanded = !isExpanded }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Ver menos" else "Ver más",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Menos detalles" else "Más detalles",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val ctx = LocalContext.current

                        Button(
                            onClick = {
                                viewModel.toggleContratoCerrado(contrato)
                                val statusMsg = if (contrato.cerrado) "Contrato marcado como pendiente de instalación." else "¡Contrato marcado como instalado/completado!"
                                Toast.makeText(ctx, statusMsg, Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (contrato.cerrado) Color(0xFF64748B) else Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = if (contrato.cerrado) Icons.Default.Undo else Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (contrato.cerrado) "Reabrir" else "Instalado",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val dest = contrato.correo.ifBlank { "tecnicablesedemargarita@gmail.com" }
                                if (contrato.correo.isBlank()) {
                                    Toast.makeText(ctx, "Cliente sin correo en sistema. Enviando copia a HQ.", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(ctx, "Enviando copia digital a $dest...", Toast.LENGTH_SHORT).show()
                                }
                                val mailSubject = "Ficha Digital Tecnicable - N° Instalación ${contrato.nroInstalacion}"
                                val mailHtml = viewModel.generateContractHtml(contrato)
                                viewModel.sendEmailSMTP(dest, mailSubject, mailHtml) { success, msg ->
                                    Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .background(Color(0xFFFFC107), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Enviar Correo",
                                tint = Color(0xFF1E293B),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                PdfGenerator.generateSingleContractPdf(ctx, contrato)
                            },
                            modifier = Modifier
                                .background(Color(0xFF0A4E9B), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir PDF",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        IconButton(
                            onClick = onTelegramClick,
                            modifier = Modifier
                                .background(Color(0xFFD32F2F), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar a Telegram",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
