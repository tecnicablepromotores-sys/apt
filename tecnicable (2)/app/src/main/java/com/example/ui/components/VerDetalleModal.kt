package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class DetalleFieldData(
    val key: String,
    val label: String,
    val value: String,
    val isWide: Boolean = false
)

@Composable
fun VerDetalleModal(
    title: String,
    origen: String,
    estado: String,
    fields: List<DetalleFieldData>,
    photoBitmaps: List<androidx.compose.ui.graphics.ImageBitmap> = emptyList(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zoomImage by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // HERO BANNER
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF0D9488), Color(0xFF0F766E), Color(0xFF3B82F6))
                            )
                        )
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // AVATAR & HEADER CONTENT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .offset(y = (-35).dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Avatar Circle
                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(3.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoBitmaps.isNotEmpty()) {
                                Image(
                                    bitmap = photoBitmaps.first(),
                                    contentDescription = title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initials = title.split(" ")
                                    .filter { it.isNotBlank() }
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercase() }
                                    .joinToString("")
                                    .ifEmpty { "?" }
                                Text(
                                    text = initials,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = title.ifBlank { "Sin Nombre" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Badges Row
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Origen Badge
                                val origenColor = when (origen.lowercase()) {
                                    "web" -> Pair(Color(0xFF06B6D4), Color(0xFF67E8F9))
                                    "censo" -> Pair(Color(0xFFF59E0B), Color(0xFFFCD34D))
                                    else -> Pair(Color(0xFF10B981), Color(0xFF6EE7B7))
                                }
                                Surface(
                                    color = origenColor.first.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, origenColor.first.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = origen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = origenColor.second,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                // Estado Badge
                                val estadoColor = when (estado.lowercase()) {
                                    "completado", "activo", "procesado", "aprobado", "finalizado" -> Pair(Color(0xFF10B981), Color(0xFF6EE7B7))
                                    "rechazado", "cancelado", "no interesado" -> Pair(Color(0xFFEF4444), Color(0xFFFCA5A5))
                                    "en proceso", "proceso", "contactado" -> Pair(Color(0xFF3B82F6), Color(0xFF93BBFC))
                                    else -> Pair(Color(0xFFF59E0B), Color(0xFFFCD34D)) // Pendiente / Prospecto
                                }
                                Surface(
                                    color = estadoColor.first.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, estadoColor.first.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = estado,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = estadoColor.second,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // INFORMATION FIELDS GRID
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fields.forEach { field ->
                            DetalleFieldCard(field = field, context = context)
                        }
                    }

                    // ATTACHED PHOTOS GALLERY
                    if (photoBitmaps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "FOTOS ADJUNTAS (${photoBitmaps.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            photoBitmaps.forEachIndexed { index, bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable { zoomImage = bmp },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp,
                                        contentDescription = "Foto ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        }
    }

    // LIGHTBOX ZOOM DIALOG
    if (zoomImage != null) {
        Dialog(
            onDismissRequest = { zoomImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { zoomImage = null },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = zoomImage!!,
                    contentDescription = "Foto Ampliada",
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.85f),
                    contentScale = ContentScale.Fit
                )
                IconButton(
                    onClick = { zoomImage = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DetalleFieldCard(
    field: DetalleFieldData,
    context: Context
) {
    val icon = when {
        field.key.contains("correo") || field.key.contains("email") -> Icons.Default.Email
        field.key.contains("telefono") || field.key.contains("celular") -> Icons.Default.Phone
        field.key.contains("cedula") || field.key.contains("ci") || field.key.contains("dni") -> Icons.Default.Badge
        field.key.contains("direccion") || field.key.contains("sector") || field.key.contains("ubicacion") -> Icons.Default.LocationOn
        field.key.contains("coordenadas") -> Icons.Default.Place
        field.key.contains("plan") -> Icons.Default.Wifi
        field.key.contains("promotor") || field.key.contains("tecnico") -> Icons.Default.Person
        field.key.contains("pago") || field.key.contains("monto") || field.key.contains("referencia") -> Icons.Default.Payments
        field.key.contains("fecha") || field.key.contains("creado") -> Icons.Default.DateRange
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = field.label.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = field.value.ifBlank { "--" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Actions for phone, email, map
            if (field.value.isNotBlank() && field.value != "--") {
                if (field.key.contains("telefono") || field.key.contains("celular")) {
                    Row {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${field.value.trim()}"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Llamar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Teléfono", field.value)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Teléfono copiado", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                } else if (field.key.contains("correo") || field.key.contains("email")) {
                    Row {
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${field.value.trim()}"))
                                try { context.startActivity(intent) } catch (e: Exception) {}
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = "Correo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Correo", field.value)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Correo copiado", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                    }
                } else if (field.key.contains("coordenadas") || field.key.contains("ubicacion")) {
                    IconButton(
                        onClick = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(field.value)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Abrir mapa", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
