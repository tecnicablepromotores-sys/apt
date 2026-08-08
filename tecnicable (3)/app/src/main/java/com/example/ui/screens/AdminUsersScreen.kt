package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.MainViewModel
import com.example.PerfilUsuario
import com.example.checkIsAdmin

data class UsuarioAdmin(
    val uid: String,
    val nombre: String,
    val email: String,
    val rol: String,
    val activo: Boolean
)

@Composable
fun AdminUsersScreen(viewModel: MainViewModel) {
    val activePerfil by viewModel.activePerfil.collectAsStateWithLifecycle()
    val allPerfiles by viewModel.allPerfiles.collectAsStateWithLifecycle()
    val isAdmin = activePerfil.checkIsAdmin()

    if (!isAdmin) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(48.dp))
                    Text("🔒 Panel de Usuarios Exclusivo para Administrador", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        return
    }

    val usuarios = remember(allPerfiles) {
        allPerfiles.map { p ->
            UsuarioAdmin(
                uid = p.uuid.ifBlank { p.id.toString() },
                nombre = p.nombre.ifBlank { p.usuario },
                email = p.correo.ifBlank { "${p.usuario}@tecnicable.com" },
                rol = p.rol.ifBlank { "Promotor(a)" },
                activo = p.esActivo
            )
        }
    }

    AdminUsersContent(
        usuarios = usuarios,
        onCambiarRol = { uid, nuevoRol ->
            val p = allPerfiles.firstOrNull { (it.uuid.ifBlank { it.id.toString() }) == uid }
            if (p != null) {
                val updated = p.copy(rol = nuevoRol)
                viewModel.syncPerfilToFirebase(updated)
            }
        },
        onCambiarEstado = { uid, nuevoEstado ->
            val p = allPerfiles.firstOrNull { (it.uuid.ifBlank { it.id.toString() }) == uid }
            if (p != null) {
                val updated = p.copy(esActivo = nuevoEstado)
                viewModel.syncPerfilToFirebase(updated)
            }
        },
        onEliminarUsuario = { uid ->
            val p = allPerfiles.firstOrNull { (it.uuid.ifBlank { it.id.toString() }) == uid }
            if (p != null) {
                viewModel.deletePerfilFromFirebase(p.id, p.usuario)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersContent(
    usuarios: List<UsuarioAdmin>,
    onCambiarRol: (String, String) -> Unit,
    onCambiarEstado: (String, Boolean) -> Unit,
    onEliminarUsuario: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(32.dp))
                Column {
                    Text("GESTIÓN ADMINISTRATIVA DE USUARIOS", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Administración de accesos, roles y estado de cuentas de la plataforma.", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (usuarios.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay usuarios registrados en el sistema.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(usuarios, key = { it.uid }) { usuario ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = usuario.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(text = usuario.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "Rol: ${usuario.rol}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(if (usuario.activo) "Activo" else "Inactivo", fontSize = 12.sp)
                                    Switch(
                                        checked = usuario.activo,
                                        onCheckedChange = { nuevoEstado ->
                                            onCambiarEstado(usuario.uid, nuevoEstado)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = {
                                    val nuevoRol = if (usuario.rol.lowercase().contains("admin")) "Promotor(a)" else "Administrador"
                                    onCambiarRol(usuario.uid, nuevoRol)
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Cambiar Rol")
                                }
                                IconButton(onClick = { onEliminarUsuario(usuario.uid) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
