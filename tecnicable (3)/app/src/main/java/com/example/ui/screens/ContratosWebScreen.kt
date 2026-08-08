package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AppScreen
import com.example.MainViewModel
import com.example.checkIsAdmin
import com.example.ui.components.*

@Composable
fun ContratosWebScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val contratosWeb by viewModel.allContratosWeb.collectAsStateWithLifecycle()
    val allPerfiles by viewModel.allPerfiles.collectAsStateWithLifecycle()
    val activePerfil by viewModel.activePerfil.collectAsStateWithLifecycle()
    
    val isAdmin = activePerfil.checkIsAdmin()

    // Non-admin promoters see ONLY web contracts assigned to them
    val visibleContracts = remember(contratosWeb, activePerfil, isAdmin) {
        if (isAdmin) {
            contratosWeb
        } else {
            val pName = activePerfil?.nombre?.trim() ?: ""
            val pUser = activePerfil?.usuario?.trim() ?: ""
            contratosWeb.filter { c ->
                val tName = c.tecnicoNombre.trim()
                tName.equals(pName, ignoreCase = true) ||
                tName.equals(pUser, ignoreCase = true) ||
                tName.isBlank() ||
                tName.equals("Sin Asignar", ignoreCase = true) ||
                tName.equals("Soporte Web", ignoreCase = true) ||
                tName.equals("Web", ignoreCase = true)
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Todos") } // Todos, Pendiente, Procesado, Rechazado
    var selectedPromotorFilter by remember { mutableStateOf("Todos") } // Todos or specific promoter name
    var showPromoterWebDropdown by remember { mutableStateOf(false) }
    
    // Fullscreen image zoom dialog
    var zoomImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    
    val filteredContracts = remember(visibleContracts, searchQuery, selectedFilter, selectedPromotorFilter) {
        visibleContracts.filter { c ->
            val matchQuery = c.nombreCliente.contains(searchQuery, ignoreCase = true) || 
                             c.cedula.contains(searchQuery, ignoreCase = true) ||
                             c.nroInstalacion.contains(searchQuery, ignoreCase = true) ||
                             c.tecnicoNombre.contains(searchQuery, ignoreCase = true)
            
            val matchFilter = when (selectedFilter) {
                "Pendiente" -> c.estado.equals("Pendiente", ignoreCase = true)
                "Procesado" -> c.estado.equals("Procesado", ignoreCase = true) || c.estado.equals("Completado", ignoreCase = true) || c.estado.equals("Aprobado", ignoreCase = true) || c.estado.equals("Finalizado", ignoreCase = true)
                "Rechazado" -> c.estado.equals("Rechazado", ignoreCase = true) || c.estado.equals("Cancelado", ignoreCase = true)
                else -> true
            }

            val matchPromotor = if (selectedPromotorFilter == "Todos") {
                true
            } else if (selectedPromotorFilter == "Sin Asignar") {
                c.tecnicoNombre.isBlank() || c.tecnicoNombre.equals("Sin Asignar", ignoreCase = true) || c.tecnicoNombre.equals("Soporte Web", ignoreCase = true)
            } else {
                c.tecnicoNombre.equals(selectedPromotorFilter, ignoreCase = true)
            }

            matchQuery && matchFilter && matchPromotor
        }.sortedByDescending { it.timestamp }
    }
    
    if (zoomImageBitmap != null) {
        AlertDialog(
            onDismissRequest = { zoomImageBitmap = null },
            title = { Text("Visualización de Foto Frente", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = zoomImageBitmap!!,
                        contentDescription = "Foto Ampliada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { zoomImageBitmap = null }) {
                    Text("Cerrar")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Upper Header and live status badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Contratos Web y Notificaciones",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Asignación de Promotores y Notificaciones en Vivo",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Pulse icon for live connection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Text(
                    text = "EN VIVO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }

        // Summary Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val totalWebCount = visibleContracts.size
            val pendingWebCount = visibleContracts.count { it.estado.equals("Pendiente", ignoreCase = true) }
            val processedWebCount = visibleContracts.count { 
                it.estado.equals("Procesado", ignoreCase = true) || 
                it.estado.equals("Completado", ignoreCase = true) || 
                it.estado.equals("Aprobado", ignoreCase = true) || 
                it.estado.equals("Finalizado", ignoreCase = true) 
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Web", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    Text("$totalWebCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pendientes", fontSize = 10.sp, color = Color(0xFFD97706))
                    Text("$pendingWebCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD1FAE5)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Procesados", fontSize = 10.sp, color = Color(0xFF059669))
                    Text("$processedWebCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                }
            }
        }
        
        // Search control using TecnicableSearchBar
        TecnicableSearchBar(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            hint = "Buscar cliente, cédula, código o promotor..."
        )
        
        // Status Filter tabs using TecnicableStatusFilters
        TecnicableStatusFilters(
            statuses = listOf("Todos", "Pendiente", "Procesado", "Rechazado"),
            selected = selectedFilter,
            onTap = { selectedFilter = it }
        )

        // Promotor Filter Selector (Lista Desplegable)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Filtrar por Promotor Asignado (Lista Desplegable):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            val promotorOptions = remember(allPerfiles, visibleContracts) {
                val list = mutableListOf("Todos", "Sin Asignar")
                allPerfiles
                    .map { it.nombre.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .forEach {
                        if (!list.contains(it)) list.add(it)
                    }
                visibleContracts
                    .map { it.tecnicoNombre.trim() }
                    .filter { it.isNotBlank() && !it.equals("Sin Asignar", ignoreCase = true) }
                    .distinct()
                    .forEach {
                        if (!list.contains(it)) list.add(it)
                    }
                list
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedPromotorFilter,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Seleccionar Promotor") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        IconButton(onClick = { showPromoterWebDropdown = !showPromoterWebDropdown }) {
                            Icon(
                                imageVector = if (showPromoterWebDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Desplegar lista de promotores"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPromoterWebDropdown = !showPromoterWebDropdown },
                    shape = RoundedCornerShape(10.dp)
                )

                DropdownMenu(
                    expanded = showPromoterWebDropdown,
                    onDismissRequest = { showPromoterWebDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    promotorOptions.forEach { promotorOpt ->
                        val isSelected = selectedPromotorFilter == promotorOpt
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (promotorOpt == "Todos") Icons.Default.Group else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = promotorOpt,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            },
                            onClick = {
                                selectedPromotorFilter = promotorOpt
                                showPromoterWebDropdown = false
                            }
                        )
                    }
                }
            }
        }
        
        if (filteredContracts.isEmpty()) {
            TecnicableEmptyState(
                message = "No hay solicitudes web que coincidan",
                buttonText = "Registrar Nuevo Contrato",
                onButtonPressed = { viewModel.navigateTo(AppScreen.REGISTRATION_FORM) },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = filteredContracts, key = { it.uuid }) { contract ->
                    var showPromotorDropdown by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contract.nombreCliente,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Código: ${contract.nroInstalacion}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                val badgeColors = when (contract.estado.trim().lowercase()) {
                                    "pendiente" -> Pair(Color(0xFFFEF3C7), Color(0xFFD97706))
                                    "procesado", "completado", "aprobado", "finalizado" -> Pair(Color(0xFFD1FAE5), Color(0xFF059669))
                                    "rechazado", "cancelado" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                                    else -> Pair(Color(0xFFF3F4F6), Color(0xFF4B5563))
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .background(badgeColors.first, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = contract.estado.uppercase(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColors.second
                                    )
                                }
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "💼 Promotor / Vendedor Asignado:",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (contract.tecnicoNombre.isNotBlank()) contract.tecnicoNombre else "Sin Asignar",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Box {
                                    Button(
                                        onClick = { showPromotorDropdown = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("⚙️ Cambiar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }

                                    DropdownMenu(
                                        expanded = showPromotorDropdown,
                                        onDismissRequest = { showPromotorDropdown = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Sin Asignar", fontWeight = FontWeight.Bold, color = Color.Gray) },
                                            onClick = {
                                                showPromotorDropdown = false
                                                viewModel.updateContratoWebPromotor(contract, "Sin Asignar", context)
                                            }
                                        )
                                        allPerfiles.filter { it.nombre.isNotBlank() }.forEach { perfil ->
                                            DropdownMenuItem(
                                                text = {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Text(perfil.nombre, fontWeight = FontWeight.SemiBold)
                                                    }
                                                },
                                                onClick = {
                                                    showPromotorDropdown = false
                                                    viewModel.updateContratoWebPromotor(contract, perfil.nombre, context)
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1.3f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🪪 Cédula: ${contract.cedula}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("📞 Celular: ${contract.celular}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("✉️ Correo: ${contract.correo}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("🗺️ Plan: ${contract.plan}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("💵 Pago: ${contract.monto} - ${contract.metodoPago} (${contract.referenciaPago})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("📍 Dirección: ${contract.direccion}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                
                                Column(
                                    modifier = Modifier.weight(0.7f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val bitmap = remember(contract.foto_frente_base64) {
                                        try {
                                            val pathOrBase64 = contract.foto_frente_base64 ?: ""
                                            if (pathOrBase64.isEmpty()) null else {
                                                if (pathOrBase64.startsWith("/") || pathOrBase64.startsWith("file://") || pathOrBase64.startsWith("content://")) {
                                                    val cleanPath = pathOrBase64.replace("file://", "")
                                                    val file = java.io.File(cleanPath)
                                                    if (file.exists()) {
                                                        val options = android.graphics.BitmapFactory.Options().apply {
                                                            inJustDecodeBounds = true
                                                        }
                                                        android.graphics.BitmapFactory.decodeFile(file.absolutePath, options)
                                                        var scale = 1
                                                        val targetWidth = 150
                                                        val targetHeight = 150
                                                        while (options.outWidth / scale / 2 >= targetWidth && options.outHeight / scale / 2 >= targetHeight) {
                                                            scale *= 2
                                                        }
                                                        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                                                            inSampleSize = scale
                                                        }
                                                        val bitmapVal = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                                                        bitmapVal?.asImageBitmap()
                                                    } else {
                                                        null
                                                    }
                                                } else {
                                                    val limpiarTexto = pathOrBase64.substringAfter(",").trim()
                                                    val datosBinarios = try {
                                                        android.util.Base64.decode(limpiarTexto, android.util.Base64.DEFAULT)
                                                    } catch (e1: Exception) {
                                                        try {
                                                            android.util.Base64.decode(limpiarTexto, android.util.Base64.NO_WRAP)
                                                        } catch (e2: Exception) {
                                                            try {
                                                                android.util.Base64.decode(limpiarTexto, android.util.Base64.URL_SAFE)
                                                            } catch (e3: Exception) {
                                                                android.util.Base64.decode(limpiarTexto, android.util.Base64.NO_PADDING)
                                                            }
                                                        }
                                                    }
                                                    val options = android.graphics.BitmapFactory.Options().apply {
                                                        inJustDecodeBounds = true
                                                    }
                                                    android.graphics.BitmapFactory.decodeByteArray(datosBinarios, 0, datosBinarios.size, options)
                                                    var scale = 1
                                                    val targetWidth = 150
                                                    val targetHeight = 150
                                                    while (options.outWidth / scale / 2 >= targetWidth && options.outHeight / scale / 2 >= targetHeight) {
                                                        scale *= 2
                                                    }
                                                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                                                        inSampleSize = scale
                                                    }
                                                    val bitmapVal = android.graphics.BitmapFactory.decodeByteArray(datosBinarios, 0, datosBinarios.size, decodeOptions)
                                                    bitmapVal?.asImageBitmap()
                                                }
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(85.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF1F5F9))
                                            .clickable {
                                                if (bitmap != null) {
                                                    zoomImageBitmap = bitmap
                                                } else {
                                                    Toast.makeText(context, "No hay foto frente disponible para ampliar", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap,
                                                contentDescription = "Foto Frente",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                                        }
                                    }
                                    Text(
                                        text = "Foto Frente (Zoom)",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recibido: " + try {
                                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                        sdf.format(java.util.Date(contract.timestamp))
                                    } catch(e: Exception) { contract.fecha },
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (contract.estado == "Pendiente") {
                                        Button(
                                            onClick = {
                                                viewModel.autoFillFromContratoWeb(context, contract) {
                                                    viewModel.updateContratoWebEstado(contract, "Procesado")
                                                }
                                                Toast.makeText(context, "Formulario cargado con datos de ${contract.nombreCliente}", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("✍️ Autorellenar", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                viewModel.updateContratoWebEstado(contract, "Rechazado")
                                                Toast.makeText(context, "Contrato Web marcado como rechazado", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Rechazar", fontSize = 11.sp, color = Color.White)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updateContratoWebEstado(contract, "Pendiente")
                                                Toast.makeText(context, "Estatus restablecido a Pendiente", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text("Reabrir", fontSize = 11.sp)
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteContratoWeb(contract)
                                            Toast.makeText(context, "Contrato Web de ${contract.nombreCliente} eliminado de la bandeja", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(18.dp)
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
