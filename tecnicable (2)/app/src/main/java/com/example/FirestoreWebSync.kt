package com.example

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
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

    private fun ensureAuth(onReady: () -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onReady()
                } else {
                    Log.w(TAG, "Autenticación anónima previa a Firestore no completada: ${task.exception?.message}")
                    onReady() // Intentar de todos modos con reintento offline de Firestore
                }
            }
        } else {
            onReady()
        }
    }

    /**
     * Sube un contrato/cliente cerrado por un promotor de la app.
     * Va a "clientes_app" (misma colección que la web usa para marcar origen = "App").
     */
    fun subirClienteApp(contrato: ContratoDiario) {
        ensureAuth {
            val data = hashMapOf(
                "cliente" to contrato.nombreCliente,
                "nombre" to contrato.nombreCliente,
                "cedula" to contrato.cedula,
                "telefono" to contrato.celular,
                "correo" to contrato.correo,
                "email" to contrato.correo,
                "sector" to contrato.direccion,
                "direccion" to contrato.direccion,
                "plan" to contrato.plan,
                "estado" to if (contrato.cerrado) "Completado" else "Pendiente",
                "metodo" to contrato.metodoPago,
                "metodoPago" to contrato.metodoPago,
                "monto" to contrato.monto,
                "referenciaPago" to contrato.referenciaPago,
                "promotor" to contrato.tecnicoNombre,
                "tecnicoNombre" to contrato.tecnicoNombre,
                "fecha" to contrato.fecha,
                "origen" to "App",
                "plataforma_registro" to "app_kotlin",
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("clientes_app")
                .document(contrato.uuid)
                .set(data)
                .addOnSuccessListener { Log.d(TAG, "clientes_app sincronizado: ${contrato.uuid}") }
                .addOnFailureListener { e -> Log.w(TAG, "Aviso sincronización clientes_app (${contrato.uuid}): ${e.message}") }
        }
    }

    /**
     * Sube/actualiza un prospecto de censo. Va a "censo_prospectos" (origen = "Censo").
     */
    fun subirProspectoCenso(prospecto: ProspectoCenso) {
        ensureAuth {
            val data = hashMapOf(
                "nombre" to prospecto.nombreCompleto,
                "cliente" to prospecto.nombreCompleto,
                "ciudadano" to prospecto.nombreCompleto,
                "cedula" to prospecto.cedula,
                "telefono" to prospecto.telefono,
                "sector" to prospecto.zona,
                "zona" to prospecto.zona,
                "direccion" to prospecto.zona,
                "estado" to prospecto.estatus,
                "estatus" to prospecto.estatus,
                "promotor" to prospecto.usuarioGestor,
                "usuarioGestor" to prospecto.usuarioGestor,
                "origen" to "Censo",
                "plataforma_registro" to "app_kotlin",
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("censo_prospectos")
                .document(prospecto.id)
                .set(data)
                .addOnSuccessListener { Log.d(TAG, "censo_prospectos sincronizado: ${prospecto.id}") }
                .addOnFailureListener { e -> Log.w(TAG, "Aviso sincronización censo_prospectos (${prospecto.id}): ${e.message}") }
        }
    }

    /**
     * Actualiza la ubicación GPS en vivo del promotor autenticado.
     */
    fun actualizarUbicacionPromotor(promotorId: String, nombrePromotor: String, lat: Double, lng: Double) {
        ensureAuth {
            val data = hashMapOf(
                "promotor" to nombrePromotor,
                "usuario" to nombrePromotor,
                "lat" to lat,
                "lng" to lng,
                "latitude" to lat,
                "longitude" to lng,
                "plataforma_registro" to "app_kotlin",
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("promotores_ubicacion")
                .document(promotorId)
                .set(data)
                .addOnFailureListener { e -> Log.w(TAG, "Aviso actualizando ubicación: ${e.message}") }
        }
    }
}

