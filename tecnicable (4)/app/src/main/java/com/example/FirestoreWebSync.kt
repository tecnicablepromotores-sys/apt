package com.example

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Puente único de sincronización hacia Cloud Firestore del proyecto "tecnicable-e00d3".
 * Replica EXACTAMENTE los nombres de colección y de campo que usa index.html (la web),
 * para que cualquier registro creado desde la app aparezca de inmediato en el dashboard.
 *
 * Colecciones reales de la web:
 *   - usuarios              (perfil + rol: "administrador" | "promotor")
 *   - censo_prospectos      (origen = "Censo")
 *   - clientes_app          (origen = "App")
 *   - portal_web            (origen = "Web")
 *   - promotores_ubicacion  (rastreo GPS en vivo)
 */
class FirestoreWebSync(private val firestore: FirebaseFirestore) {

    private val TAG = "FirestoreWebSync"

    /**
     * Sube un contrato/cliente cerrado por un promotor de la app.
     * Va a "clientes_app" (misma colección que la web usa para marcar origen = "App").
     * Los nombres de campo siguen lo que lee la web: cliente/nombre, cedula, sector, plan,
     * estado, metodo, promotor, fecha, plataforma_registro.
     */
    fun subirClienteApp(contrato: ContratoDiario) {
        val data = hashMapOf(
            "cliente" to contrato.nombreCliente,
            "cedula" to contrato.cedula,
            "telefono" to contrato.celular,
            "correo" to contrato.correo,
            "sector" to contrato.direccion,
            "plan" to contrato.plan,
            "estado" to if (contrato.cerrado) "Instalado" else "Pendiente",
            "metodo" to contrato.metodoPago,
            "monto" to contrato.monto,
            "referenciaPago" to contrato.referenciaPago,
            "promotor" to contrato.tecnicoNombre,
            "fecha" to contrato.fecha,
            "origen" to "App",
            "plataforma_registro" to "app_kotlin",
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("clientes_app")
            .document(contrato.uuid)
            .set(data)
            .addOnSuccessListener { Log.d(TAG, "clientes_app sincronizado: ${contrato.uuid}") }
            .addOnFailureListener { e -> Log.e(TAG, "Error subiendo a clientes_app", e) }
    }

    /**
     * Sube/actualiza un prospecto de censo. Va a "censo_prospectos" (origen = "Censo"),
     * igual que la web lo clasifica.
     */
    fun subirProspectoCenso(prospecto: ProspectoCenso) {
        val data = hashMapOf(
            "nombre" to prospecto.nombreCompleto,
            "cedula" to prospecto.cedula,
            "telefono" to prospecto.telefono,
            "sector" to prospecto.zona,
            "estado" to prospecto.estatus,
            "promotor" to prospecto.usuarioGestor,
            "origen" to "Censo",
            "plataforma_registro" to "app_kotlin",
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("censo_prospectos")
            .document(prospecto.id)
            .set(data)
            .addOnSuccessListener { Log.d(TAG, "censo_prospectos sincronizado: ${prospecto.id}") }
            .addOnFailureListener { e -> Log.e(TAG, "Error subiendo a censo_prospectos", e) }
    }

    /**
     * Actualiza la ubicación GPS en vivo del promotor autenticado.
     * Un documento por promotor (usa el uid/nombre como id del doc), tal como espera
     * el mapa Leaflet de la web: promotor, lat, lng, timestamp.
     */
    fun actualizarUbicacionPromotor(promotorId: String, nombrePromotor: String, lat: Double, lng: Double) {
        val data = hashMapOf(
            "promotor" to nombrePromotor,
            "lat" to lat,
            "lng" to lng,
            "plataforma_registro" to "app_kotlin",
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("promotores_ubicacion")
            .document(promotorId)
            .set(data)
            .addOnFailureListener { e -> Log.e(TAG, "Error actualizando ubicación", e) }
    }
}
