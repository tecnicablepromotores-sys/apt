package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun DataTestManagerScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                try {
                    viewModel.exportTestDataToUri(uri, context)
                    Toast.makeText(context, "Datos exportados con éxito", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            scope.launch {
                try {
                    viewModel.importTestDataFromUri(uri, context)
                    Toast.makeText(context, "Datos importados con éxito", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error al importar: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }

    // Getting counts from viewmodel
    val contratosCount = viewModel.allContratos.collectAsStateWithLifecycle().value.size
    val clientesCount = contratosCount // Same in this logic as they share data
    val perfilesCount = viewModel.allPerfiles.collectAsStateWithLifecycle().value.size
    val censoCount = viewModel.censoProspectos.collectAsStateWithLifecycle().value.size
    val contratosWebCount = viewModel.allContratosWeb.collectAsStateWithLifecycle().value.size
    val codigosCount = viewModel.codigosClientes.collectAsStateWithLifecycle().value.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Gestión de Datos de Prueba",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Exporta o importa un archivo JSON con los datos de Firebase.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resumen Actual (En memoria):", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Contratos Diarios: $contratosCount")
                    Text("Clientes Registrados: $clientesCount")
                    Text("Usuarios / Promotores: $perfilesCount")
                    Text("Censo Prospectos: $censoCount")
                    Text("Contratos Web: $contratosWebCount")
                    Text("Códigos Clientes: $codigosCount")
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { createDocumentLauncher.launch("tecnicable_test_data.json") },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting && !isImporting
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Exportar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportar")
                        }
                    }

                    Button(
                        onClick = { openDocumentLauncher.launch(arrayOf("application/json", "*/*")) },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting && !isImporting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = "Importar")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar")
                        }
                    }
                }

                var showSeedConfirm by remember { mutableStateOf(false) }

                if (showSeedConfirm) {
                    AlertDialog(
                        onDismissRequest = { showSeedConfirm = false },
                        title = { Text("Confirmar Carga de Datos de Prueba") },
                        text = { Text("¿Desea cargar datos de prueba (mock) en Firebase? Esto solo debe usarse en desarrollo/debug.") },
                        confirmButton = {
                            Button(onClick = {
                                showSeedConfirm = false
                                if (com.example.BuildConfig.DEBUG) {
                                    viewModel.seedTestData()
                                    Toast.makeText(context, "Datos de prueba cargados", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No permitido en producción", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Sí, cargar")
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showSeedConfirm = false }) {
                                Text("Cancelar")
                            }
                        }
                    )
                }

                Button(
                    onClick = { 
                        showSeedConfirm = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isExporting && !isImporting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E))
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Cargar datos de prueba")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cargar datos de prueba (Mock)")
                }
                
            }
        }
        
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "⚠ IMPORTANTE: Importar datos añadirá nuevos registros a Firebase (Firestore y Realtime Database). Úselo con precaución para evitar duplicar información en entornos de producción.",
                    color = Color(0xFF991B1B),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
