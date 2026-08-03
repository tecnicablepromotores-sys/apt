package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.PdfGenerator
import com.example.checkIsAdmin
import com.example.ui.components.*

@Composable
fun CensoProspectosScreen(viewModel: MainViewModel) {
    val prospectos by viewModel.censoProspectos.collectAsStateWithLifecycle()
    val activePerfil by viewModel.activePerfil.collectAsStateWithLifecycle()
    val isAdmin = activePerfil.checkIsAdmin()
    val context = LocalContext.current

    // Only show user's own census data unless they are the Admin
    val userFilteredProspectos = remember(prospectos, activePerfil, isAdmin) {
        if (isAdmin) {
            prospectos
        } else {
            val pName = activePerfil?.nombre?.trim() ?: ""
            val pUser = activePerfil?.usuario?.trim() ?: ""
            prospectos.filter { 
                it.usuarioGestor.trim().equals(pName, ignoreCase = true) ||
                it.usuarioGestor.trim().equals(pUser, ignoreCase = true)
            }
        }
    }

    // Form registration inputs
    var nombreCompleto by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    
    // Select lists
    val zonasDisponibles = listOf(
        "Antolín del Campo", "Arismendi", "Díaz", "García", "Gómez", 
        "Maneiro", "Marcano", "Mariño", "Península de Macanao", "Tubores", "Villalba"
    )
    var selectedZona by remember { mutableStateOf(zonasDisponibles.first()) }
    
    val estatusDisponibles = listOf("Pendiente", "Contactado", "Sin factibilidad", "Instalación programada")
    var selectedEstatus by remember { mutableStateOf(estatusDisponibles.first()) }

    var formMessage by remember { mutableStateOf<String?>(null) }
    var searchFilter by remember { mutableStateOf("") }
    var activeTabFilter by remember { mutableStateOf("Todos") }
    
    // Administrative filters requested: Municipio filter & Gestor filter
    var municipioFilter by remember { mutableStateOf("Todos") }
    var gestorFilter by remember { mutableStateOf("Todos") }
    
    val uniqueGestors = remember(userFilteredProspectos) {
        listOf("Todos") + userFilteredProspectos.map { it.usuarioGestor }.filter { it.isNotBlank() }.distinct()
    }

    // WhatsApp Mass Broadcast states
    var isWhatsAppExpanded by remember { mutableStateOf(false) }
    var mensajeMasivo by remember { mutableStateOf("Hola {nombre}, te escribimos de Tecnicable para saludarte e informarte que ya tenemos cobertura de Fibra Óptica en la zona de {zona}. ¿Te gustaría formalizar tu contrato?") }

    val filteredList = userFilteredProspectos.filter {
        val matchesSearch = (it.nombreCompleto.contains(searchFilter, ignoreCase = true) ||
                             it.cedula.contains(searchFilter, ignoreCase = true) ||
                             it.zona.contains(searchFilter, ignoreCase = true))
        val matchesEstatus = (activeTabFilter == "Todos" || it.estatus.equals(activeTabFilter, ignoreCase = true))
        val matchesMunicipio = (municipioFilter == "Todos" || it.zona.equals(municipioFilter, ignoreCase = true))
        val matchesGestor = (gestorFilter == "Todos" || it.usuarioGestor.equals(gestorFilter, ignoreCase = true))
        
        matchesSearch && matchesEstatus && matchesMunicipio && matchesGestor
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Title
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "📊 CENSO DE INTERESADOS Y PROSPECTOS",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Registrar personas interesadas y organizar prospectos de forma centralizada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Interest/Prospects Summary Metrics Card
        val totalInteresados = userFilteredProspectos.size
        val pendientesCount = userFilteredProspectos.count { it.estatus.equals("Pendiente", ignoreCase = true) }
        val contactadosCount = userFilteredProspectos.count { it.estatus.equals("Contactado", ignoreCase = true) }
        val instalacionProgCount = userFilteredProspectos.count { it.estatus.contains("Instalación", ignoreCase = true) || it.estatus.contains("Contratado", ignoreCase = true) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("📈 Resumen de Clientes Interesados en el Servicio:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Total Interesados: $totalInteresados", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("• Pendientes: $pendientesCount", fontSize = 12.sp)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("• Contactados: $contactadosCount", fontSize = 12.sp)
                    Text("• Instalación Programada: $instalacionProgCount", fontSize = 12.sp)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Form Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "✍️ Registrar Nuevo Prospecto en Censo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )

                        OutlinedTextField(
                            value = nombreCompleto,
                            onValueChange = { nombreCompleto = it },
                            label = { Text("Nombre y Apellido *") },
                            placeholder = { Text("Ej: Pedro Pérez") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("censo_nombre_input"),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cedula,
                                onValueChange = { cedula = it },
                                label = { Text("Cédula *") },
                                placeholder = { Text("V-12345678") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("censo_cedula_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                            OutlinedTextField(
                                value = telefono,
                                onValueChange = { telefono = it },
                                label = { Text("Teléfono *") },
                                placeholder = { Text("0414-1234567") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("censo_telefono_input"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }

                        // Selectors: Zona / Municipio
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Zona / Municipio *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                zonasDisponibles.forEach { z ->
                                    val active = selectedZona == z
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (active) Color(0xFF0A4E9B) else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedZona = z }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(z, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color(0xFF334155))
                                    }
                                }
                            }
                        }

                        // Selectors: Estatus
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Estatus Inicial *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                estatusDisponibles.forEach { est ->
                                    val active = selectedEstatus == est
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (active) Color(0xFF0A4E9B) else Color(0xFFE2E8F0),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .clickable { selectedEstatus = est }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(est, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (active) Color.White else Color(0xFF334155))
                                    }
                                }
                            }
                        }

                        if (formMessage != null) {
                            Text(formMessage!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (nombreCompleto.isBlank() || cedula.isBlank() || telefono.isBlank()) {
                                    formMessage = "Por favor complete todos los campos requeridos (*)"
                                } else {
                                    viewModel.registrarProspectoCenso(
                                        nombre = nombreCompleto.trim(),
                                        cedula = cedula.trim(),
                                        telefono = telefono.trim(),
                                        zona = selectedZona,
                                        estatus = selectedEstatus
                                    )
                                    nombreCompleto = ""
                                    cedula = ""
                                    telefono = ""
                                    formMessage = null
                                    Toast.makeText(context, "¡Prospecto guardado exitosamente!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E9B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("censo_save_button")
                        ) {
                            Text("GUARDAR PROSPECTO", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // WhatsApp Mass Marketing Broadcast (Acordeón desplegable)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isWhatsAppExpanded = !isWhatsAppExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("📢 Módulo Envio Masivo WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF15803D))
                                Box(
                                    modifier = Modifier.background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("${filteredList.size} destinatarios", fontSize = 9.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(if (isWhatsAppExpanded) "▲ Ocultar" else "▼ Abrir Herramienta", fontSize = 11.sp, color = Color(0xFF15803D), fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(
                            visible = isWhatsAppExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Plantilla de mensaje personalizada:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF166534))
                                OutlinedTextField(
                                    value = mensajeMasivo,
                                    onValueChange = { mensajeMasivo = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 4,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White,
                                        unfocusedContainerColor = Color.White
                                    )
                                )
                                Text("Variables disponibles: {nombre}, {cedula}, {zona}", fontSize = 10.sp, color = Color.Gray)
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (filteredList.isNotEmpty()) {
                                                val primerProspecto = filteredList.first()
                                                val msg = mensajeMasivo
                                                    .replace("{nombre}", primerProspecto.nombreCompleto)
                                                    .replace("{cedula}", primerProspecto.cedula)
                                                    .replace("{zona}", primerProspecto.zona)
                                                
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                    data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=${primerProspecto.telefono.replace(Regex("[^0-9]"), "")}&text=${android.net.Uri.encode(msg)}")
                                                }
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "No hay prospectos en la lista actual", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Enviar Cadena", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Divider and Title of the list
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 Censo (${userFilteredProspectos.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                            PdfGenerator.generateCensoPdf(context, filteredList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A4E9B)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Exportar PDF", 
                            tint = Color.White, 
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Exportar censo PDF", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Filter
            item {
                TecnicableSearchBar(
                    value = searchFilter,
                    onValueChange = { searchFilter = it },
                    hint = "Buscar por nombre, cédula o zona/municipio..."
                )
            }

            // Filters: Municipio & gestor
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📍 Filtrar por Municipio:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val muniFiltersList = listOf("Todos") + zonasDisponibles
                        muniFiltersList.forEach { muni ->
                            val active = municipioFilter == muni
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (active) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { municipioFilter = muni }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = muni,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            if (isAdmin) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("👤 Filtrar por Gestor / Registrador:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uniqueGestors.forEach { gst ->
                                val active = gestorFilter == gst
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (active) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { gestorFilter = gst }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = gst,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabs for statuses filter
            item {
                val filterTabs = listOf("Todos", "Pendiente", "Contactado", "Sin factibilidad", "Instalación programada")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterTabs.forEach { tabName ->
                        val active = activeTabFilter == tabName
                        Box(
                            modifier = Modifier
                                .background(
                                    if (active) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { activeTabFilter = tabName }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = tabName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (active) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // List of filtered prospects
            if (filteredList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No se encontraron registros de censo.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(items = filteredList, key = { it.id }) { p ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(p.nombreCompleto.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                
                                val badgeColors = when (p.estatus) {
                                    "Pendiente" -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                                    "Contactado" -> Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
                                    "Sin factibilidad" -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
                                    else -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeColors.first, shape = RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(p.estatus.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = badgeColors.second)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("🪪 Cédula: " + p.cedula, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("📞 Teléfono: " + p.telefono, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("📍 Zona: " + p.zona, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Cambiar estatus:", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        estatusDisponibles.forEach { est ->
                                            if (est != p.estatus) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp))
                                                        .clickable {
                                                            viewModel.actualizarEstatusProspecto(p.id, est)
                                                            Toast.makeText(context, "Estatus actualizado a $est", Toast.LENGTH_SHORT).show()
                                                        }
                                                        .padding(horizontal = 5.dp, vertical = 3.dp)
                                                ) {
                                                    val initial = when(est) {
                                                        "Pendiente" -> "PND"
                                                        "Contactado" -> "CNT"
                                                        "Sin factibilidad" -> "S/F"
                                                        else -> "PROG"
                                                    }
                                                    Text(initial, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(4.dp))
                                                .clickable {
                                                    viewModel.eliminarProspecto(p.id)
                                                    Toast.makeText(context, "Registro eliminado", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 5.dp, vertical = 3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
