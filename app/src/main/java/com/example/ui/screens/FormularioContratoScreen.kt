package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.*

@Composable
fun FormularioContratoScreen(
    viewModel: MainViewModel,
    state: UiState,
    onPhotoClick: () -> Unit,
    onPhotoCajaClick: () -> Unit,
    onLocationClick: () -> Unit,
    onLocationCajaClick: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 0: Código de Contrato / Instalación
        DatosInstalacionCard(
            state = state,
            onValueChange = { viewModel.onNroInstalacionChange(it) }
        )

        // Section 1: Datos Personales
        DatosPersonalesCard(
            state = state,
            onNombreChange = { viewModel.onNombreChange(it) },
            onCedulaPrefixChange = { viewModel.onCedulaPrefixChange(it) },
            onCedulaNumeroChange = { viewModel.onCedulaNumeroChange(it) },
            onCorreoChange = { viewModel.onCorreoChange(it) },
            onTelefonoCelularChange = { viewModel.onTelefonoCelularChange(it) },
            onRepresentanteChange = { viewModel.onRepresentanteLegalChange(it) },
            onCedulaRepChange = { viewModel.onCedulaRepresentanteChange(it) }
        )

        // Section: Actions (GPS & Photo)
        AccionesDeAltaSection(
            state = state,
            onPhotoClick = onPhotoClick,
            onPhotoCajaClick = onPhotoCajaClick,
            onLocationClick = onLocationClick,
            onLocationCajaClick = onLocationCajaClick,
            onRemovePhoto = { viewModel.removePhoto() },
            onRemovePhotoCaja = { viewModel.removePhotoCaja() },
            onManualLocationSet = { lat, lng -> viewModel.setLocationManually(lat, lng) },
            onManualLocationCajaSet = { lat, lng -> viewModel.setLocationCajaManually(lat, lng) }
        )

        // Section 2: Ubicación / Dirección Física
        UbicacionCard(
            state = state,
            onDireccionChange = { viewModel.onDireccionChange(it) },
            onPuntoReferenciaChange = { viewModel.onPuntoReferenciaChange(it) }
        )

        // Section 3: Selección de Plan y ONU
        PlanSelectionCard(
            state = state,
            planes = viewModel.listadoPlanes,
            onPlanSelected = { viewModel.onPlanChange(it) },
            onTipoOnuSelected = { viewModel.onTipoOnuChange(it) }
        )

        // Section 4: Método de Pago
        MetodoPagoCard(
            state = state,
            onMetodoSelected = { viewModel.onMetodoPagoChange(it) },
            onMontoChange = { viewModel.onMontoPagoChange(it) },
            onReferenciaChange = { viewModel.onReferenciaPagoChange(it) }
        )

        // Section 5: Signature (Firma Digital) Card
        SignatureCard(
            state = state,
            viewModel = viewModel
        )

        // Validation Alerts
        if (state.isValidationTriggered && hasAnyErrors(state.errors)) {
            ValidationErrorBanner(errors = state.errors)
        }

        // Action Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    keyboardController?.hide()
                    if (viewModel.saveCurrentFormAsContract()) {
                        Toast.makeText(
                            context,
                            "¡Contrato guardado con éxito y enviado a Telegram!",
                            Toast.LENGTH_LONG
                        ).show()
                        viewModel.resetForm()
                    } else {
                        Toast.makeText(
                            context,
                            "Por favor complete todos los campos obligatorios.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(26.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar Registro Local",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Integration & Architecture Helper Guide (Expandable Panel)
        IntegrationGuidePanel(viewModel)

        Spacer(modifier = Modifier.height(24.dp))
    }
}
