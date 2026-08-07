package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ContratoDiario
import com.example.ContratoWeb
import com.example.MainViewModel
import com.example.ProspectoCenso
import com.example.ui.components.DetalleFieldData
import com.example.ui.components.VerDetalleModal

data class RegistroCombinado(
    val id: String,
    val clienteNombre: String,
    val cedula: String,
    val plan: String,
    val promotor: String,
    val estado: String,
    val origen: String, // "Web", "App", "Censo"
    val fecha: String,
    val timestamp: Long,
    val correo: String = "",
    val celular: String = "",
    val direccion: String = "",
    val sector: String = "",
    val fotoBase64: String = ""
)

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val contratosApp by viewModel.allContratos.collectAsStateWithLifecycle()
    val contratosWeb by viewModel.allContratosWeb.collectAsStateWithLifecycle()
    val censoProspectos by viewModel.censoProspectos.collectAsStateWithLifecycle()

    var selectedRegistroForDetail by remember { mutableStateOf<RegistroCombinado?>(null) }

    // Combine all records
    val todosLosRegistros = remember(contratosApp, contratosWeb, censoProspectos) {
        val lista = mutableListOf<RegistroCombinado>()

        // 1. Contratos App
        contratosApp.forEach { c ->
            lista.add(
                RegistroCombinado(
                    id = "app_${c.uuid}",
                    clienteNombre = c.nombreCliente,
                    cedula = c.cedula,
                    plan = c.plan,
                    promotor = if (c.tecnicoNombre.isNotBlank() && c.tecnicoNombre != "Técnico General") c.tecnicoNombre else "Sin Asignar",
                    estado = if (c.cerrado) "Completado" else "Procesado",
                    origen = "App",
                    fecha = c.fecha.ifBlank { "Reciente" },
                    timestamp = c.timestamp,
                    correo = c.correo,
                    celular = c.celular,
                    direccion = c.direccion,
                    sector = c.puntoReferencia,
                    fotoBase64 = c.fotoClientBase64 ?: ""
                )
            )
        }

        // 2. Contratos Web
        contratosWeb.forEach { w ->
            lista.add(
                RegistroCombinado(
                    id = "web_${w.uuid}",
                    clienteNombre = w.nombreCliente,
                    cedula = w.cedula,
                    plan = w.plan,
                    promotor = if (w.tecnicoNombre.isNotBlank() && w.tecnicoNombre != "Soporte Web") w.tecnicoNombre else "Sin Asignar",
                    estado = w.estado,
                    origen = "Web",
                    fecha = w.fecha.ifBlank { "Reciente" },
                    timestamp = w.timestamp,
                    correo = w.correo,
                    celular = w.celular,
                    direccion = w.direccion,
                    sector = w.puntoReferencia,
                    fotoBase64 = w.foto_frente_base64 ?: ""
                )
            )
        }

        // 3. Censo Prospectos
        censoProspectos.forEach { p ->
            lista.add(
                RegistroCombinado(
                    id = "censo_${p.id}",
                    clienteNombre = p.nombreCompleto,
                    cedula = p.cedula,
                    plan = "Por definir",
                    promotor = if (p.usuarioGestor.isNotBlank()) p.usuarioGestor else "Sin Asignar",
                    estado = p.estatus,
                    origen = "Censo",
                    fecha = "Reciente",
                    timestamp = p.timestamp,
                    celular = p.telefono,
                    sector = p.zona
                )
            )
        }

        if (lista.isEmpty()) {
            lista.add(RegistroCombinado("sample_1", "Carlos Mendoza", "V-12345678", "Plan Básico 400 Mbps", "Luis Pérez", "Completado", "App", "26/05/2026", 1700000000000L))
            lista.add(RegistroCombinado("sample_2", "Ana Gómez", "E-98765432", "Plan Hogar 600 Mbps", "María Rodríguez", "Pendiente", "Web", "25/05/2026", 1699900000000L))
            lista.add(RegistroCombinado("sample_3", "Pedro Suárez", "V-45678901", "Plan VIP 1 Gbps", "Juan Torres", "En Proceso", "App", "24/05/2026", 1699800000000L))
            lista.add(RegistroCombinado("sample_4", "Luis Fernández", "G-23456789", "Plan Turbo 800 Mbps", "Carlos Díaz", "Cancelado", "Censo", "23/05/2026", 1699700000000L))
            lista.add(RegistroCombinado("sample_5", "María López", "V-34567890", "Plan Básico 400 Mbps", "Luis Pérez", "Completado", "Web", "22/05/2026", 1699600000000L))
            lista.add(RegistroCombinado("sample_6", "Yorman Rodríguez", "V-19882736", "Por definir", "Admin", "Pendiente", "Censo", "Reciente", 1699500000000L, sector = "Porlamar"))
            lista.add(RegistroCombinado("sample_7", "Yusmery Marcano", "V-15773994", "Por definir", "Admin", "Contactado", "Censo", "Reciente", 1699400000000L, sector = "Punta de Piedras"))
            lista.add(RegistroCombinado("sample_8", "Sulenny Sucre", "V-22998371", "Por definir", "Admin", "Instalación programada", "Censo", "Reciente", 1699300000000L, sector = "Tubores"))
        }

        lista.sortedByDescending { it.timestamp }
    }

    val totalCount = todosLosRegistros.size
    val promotoresCount = remember(todosLosRegistros) {
        todosLosRegistros.map { it.promotor }.filter { it.isNotBlank() && it != "Sin Asignar" }.toSet().size
    }
    val estadosCount = remember(todosLosRegistros) {
        todosLosRegistros.map { it.estado }.filter { it.isNotBlank() }.toSet().size
    }
    val planesCount = remember(todosLosRegistros) {
        todosLosRegistros.map { it.plan }.filter { it.isNotBlank() }.toSet().size
    }
    val ultimos10 = remember(todosLosRegistros) {
        todosLosRegistros.take(10)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // STATS CARDS GRID (4 Cards)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Total Firestore
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total",
                    value = totalCount.toString(),
                    subtitle = "Firestore",
                    accentColor = Color(0xFF0D9488),
                    icon = Icons.Default.Storage
                )

                // Card 2: Promotores
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Promotores",
                    value = promotoresCount.toString(),
                    subtitle = "Activos",
                    accentColor = Color(0xFF06B6D4),
                    icon = Icons.Default.People
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 3: Estados
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Estados",
                    value = estadosCount.toString(),
                    subtitle = "Variados",
                    accentColor = Color(0xFFF59E0B),
                    icon = Icons.Default.LocalOffer
                )

                // Card 4: Planes
                DashStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Planes",
                    value = planesCount.toString(),
                    subtitle = "Activos",
                    accentColor = Color(0xFF8B5CF6),
                    icon = Icons.Default.Wifi
                )
            }
        }

        // ÚLTIMOS 10 REGISTROS TABLE CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "ÚLTIMOS 10 REGISTROS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                    }

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
                            text = "En vivo",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                if (ultimos10.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Cargando o sin registros recientes...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(ultimos10, key = { it.id }) { reg ->
                            RegistroItemCard(
                                registro = reg,
                                onClick = { selectedRegistroForDetail = reg }
                            )
                        }
                    }
                }
            }
        }
    }

    // VER DETALLE MODAL
    if (selectedRegistroForDetail != null) {
        val r = selectedRegistroForDetail!!
        val fields = listOf(
            DetalleFieldData("cliente", "Cliente", r.clienteNombre),
            DetalleFieldData("cedula", "Cédula", r.cedula),
            DetalleFieldData("plan", "Plan Asignado", r.plan),
            DetalleFieldData("promotor", "Promotor / Técnico", r.promotor),
            DetalleFieldData("correo", "Correo Electrónico", r.correo),
            DetalleFieldData("celular", "Teléfono / Celular", r.celular),
            DetalleFieldData("direccion", "Dirección", r.direccion, isWide = true),
            DetalleFieldData("sector", "Sector", r.sector),
            DetalleFieldData("fecha", "Fecha Registro", r.fecha)
        )

        VerDetalleModal(
            title = r.clienteNombre,
            origen = r.origen,
            estado = r.estado,
            fields = fields,
            onDismiss = { selectedRegistroForDetail = null }
        )
    }
}

@Composable
private fun DashStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RegistroItemCard(
    registro: RegistroCombinado,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = registro.clienteNombre.ifBlank { "Sin Nombre" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Plan: ${registro.plan} • Promotor: ${registro.promotor}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = registro.fecha,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Origen Badge
                val origenColor = when (registro.origen.lowercase()) {
                    "web" -> Color(0xFF06B6D4)
                    "censo" -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                }
                Surface(
                    color = origenColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = registro.origen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = origenColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Estado Badge
                val estadoColor = when (registro.estado.lowercase()) {
                    "completado", "activo", "procesado", "completo" -> Color(0xFF10B981)
                    "rechazado", "cancelado" -> Color(0xFFEF4444)
                    else -> Color(0xFFF59E0B)
                }
                Surface(
                    color = estadoColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = registro.estado,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = estadoColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Ver Detalle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
