package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.checkIsAdmin
import com.example.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun ContratosPorPromotorScreen(viewModel: MainViewModel) {
    val contratos by viewModel.allContratos.collectAsStateWithLifecycle()
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
                        text = "Este apartado de desglose unificado por promotor está reservado únicamente para el administrador (luifred1998@gmail.com).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val currentUserName = activePerfil?.nombre?.trim() ?: ""

    var searchFilter by remember { mutableStateOf("") }
    var selectedPromotorFilter by remember { mutableStateOf("Todos") }

    val allPerfiles by viewModel.allPerfiles.collectAsStateWithLifecycle()

    val promotersList = remember(contratos, allPerfiles) {
        val list = mutableListOf("Todos", "Sin Asignar")
        val promotoresOnly = allPerfiles
            .map { it.nombre.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val namesFromContracts = contratos
            .map { it.tecnicoNombre.trim() }
            .filter { name ->
                name.isNotBlank() && !name.equals("Sin Asignar", ignoreCase = true)
            }
            .distinct()

        (list + promotoresOnly + namesFromContracts).distinct().sorted()
    }

    var showPromoterDropdown by remember { mutableStateOf(false) }

    val relevantContratos = remember(contratos, isAdmin, currentUserName) {
        if (isAdmin) {
            contratos
        } else {
            contratos.filter { it.tecnicoNombre.trim().equals(currentUserName, ignoreCase = true) }
        }
    }

    val filteredContratos = remember(relevantContratos, searchFilter, selectedPromotorFilter) {
        relevantContratos.filter { c ->
            val promotor = c.tecnicoNombre.ifBlank { "Sin Asignar" }
            val matchesPromotor = (selectedPromotorFilter == "Todos" || promotor.equals(selectedPromotorFilter, ignoreCase = true))
            val matchesSearch = c.nombreCliente.contains(searchFilter, ignoreCase = true) ||
                                c.cedula.contains(searchFilter, ignoreCase = true) ||
                                c.nroInstalacion.contains(searchFilter, ignoreCase = true) ||
                                promotor.contains(searchFilter, ignoreCase = true)
            matchesPromotor && matchesSearch
        }.sortedByDescending { it.timestamp }
    }

    val groupedByPromotor = remember(filteredContratos) {
        filteredContratos.groupBy { it.tecnicoNombre.ifBlank { "Sin Asignar" } }
    }

    val top3Ranking = remember(relevantContratos) {
        relevantContratos
            .groupBy { it.tecnicoNombre.ifBlank { "Sin Asignar" } }
            .map { (name, list) -> name to list.size }
            .sortedByDescending { it.second }
            .take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "👥 CONTRATOS DIARIOS POR PROMOTOR",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isAdmin) "Desglose unificado de instalaciones y recaudación por cada promotor / técnico." else "Mis contratos y reportes registrados en el sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        if (top3Ranking.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🏆 Top 3 Promotores del Mes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(top3Ranking.withIndex().toList()) { (index, pair) ->
                        val (promotorName, totalCount) = pair
                        val containerBg = when (index) {
                            0 -> Color(0xFFFFD700).copy(alpha = 0.25f) // Oro
                            1 -> Color(0xFFC0C0C0).copy(alpha = 0.25f) // Plata
                            else -> Color(0xFFCD7F32).copy(alpha = 0.25f) // Bronce
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = containerBg),
                            modifier = Modifier.width(160.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "#${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = promotorName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$totalCount Contratos",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TecnicableSearchBar(
                    value = searchFilter,
                    onValueChange = { searchFilter = it },
                    hint = "Buscar por cliente, cédula o promotor..."
                )

                if (isAdmin) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Seleccionar Promotor (Lista Desplegable):",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedPromotorFilter,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Elegir Promotor") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showPromoterDropdown = !showPromoterDropdown }) {
                                        Icon(
                                            imageVector = if (showPromoterDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                            contentDescription = "Desplegar lista de promotores"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPromoterDropdown = !showPromoterDropdown },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0F172A),
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )

                            DropdownMenu(
                                expanded = showPromoterDropdown,
                                onDismissRequest = { showPromoterDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                promotersList.forEach { promotorName ->
                                    val isSelected = selectedPromotorFilter == promotorName
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (promotorName == "Todos") Icons.Default.Group else Icons.Default.Badge,
                                                    contentDescription = null,
                                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(
                                                    text = promotorName,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedPromotorFilter = promotorName
                                            showPromoterDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (groupedByPromotor.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No se encontraron contratos registrados para este filtro.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                groupedByPromotor.forEach { (promotorName, listConns) ->
                    val totalPromotorUSD = listConns.sumOf { it.monto.toDoubleOrNull() ?: 0.0 }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = promotorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${listConns.size} contratos • $$totalPromotorUSD",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                listConns.forEach { contract ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Instalación N°: ${contract.nroInstalacion}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF0F172A)
                                                )
                                                val isCerrado = contract.cerrado
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isCerrado) Color(0xFFE2E8F0) else Color(0xFFFEF3C7),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (isCerrado) "CERRADO" else "ABIERTO",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCerrado) Color(0xFF475569) else Color(0xFFB45309)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Cliente: ${contract.nombreCliente} (Cédula: ${contract.cedula})",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Plan: ${contract.plan} • Monto: $${contract.monto}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF475569)
                                                )
                                                Text(
                                                    text = contract.fecha.ifBlank { "Fecha reciente" },
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            if (contract.direccion.isNotBlank()) {
                                                Text(
                                                    text = "Dirección: ${contract.direccion}",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF64748B)
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
}
