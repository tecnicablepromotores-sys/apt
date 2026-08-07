package com.example

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class TecnicableSync(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val TAG = "TecnicableSync"

    private fun ensureAuth(onReady: () -> Unit) {
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { onReady() }
        } else onReady()
    }

    // ---------- CLIENTES (App) -> colección "clientes_app" ----------

    fun subirClienteApp(contrato: ContratoDiario, promotorId: String = "") = ensureAuth {
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
        firestore.collection("clientes_app").document(contrato.uuid).set(data)
            .addOnFailureListener { e -> Log.w(TAG, "clientes_app (${contrato.uuid}): ${e.message}") }
    }

    fun escucharClientesApp(onChange: (List<ContratoDiario>) -> Unit): ListenerRegistration =
        firestore.collection("clientes_app").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            onChange(snap.documents.mapNotNull { it.toObject(ContratoDiario::class.java) })
        }

    // ---------- CENSO -> colección "censo_prospectos" ----------

    fun subirProspectoCenso(prospecto: ProspectoCenso) = ensureAuth {
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
        firestore.collection("censo_prospectos").document(prospecto.id).set(data)
            .addOnFailureListener { e -> Log.w(TAG, "censo_prospectos (${prospecto.id}): ${e.message}") }
    }

    fun escucharCensoProspectos(onChange: (List<ProspectoCenso>) -> Unit): ListenerRegistration =
        firestore.collection("censo_prospectos").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            onChange(snap.documents.mapNotNull { it.toObject(ProspectoCenso::class.java) })
        }

    fun eliminarProspectoCenso(id: String) = ensureAuth {
        firestore.collection("censo_prospectos").document(id).delete()
    }

    // ---------- CONTRATOS WEB -> colección "portal_web" ----------

    fun escucharContratosWeb(onChange: (List<ContratoWeb>) -> Unit): ListenerRegistration =
        firestore.collection("portal_web").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            onChange(snap.documents.mapNotNull { it.toObject(ContratoWeb::class.java) })
        }

    fun actualizarEstadoContratoWeb(uuid: String, nuevoEstado: String) = ensureAuth {
        firestore.collection("portal_web").document(uuid).update("estado", nuevoEstado)
    }

    // ---------- CÓDIGOS DE CLIENTES ----------

    fun subirCodigoCliente(codigo: CodigoCliente) = ensureAuth {
        val data = hashMapOf(
            "codigo" to codigo.codigo,
            "clienteNombre" to codigo.clienteNombre,
            "planAsignado" to codigo.planAsignado,
            "estado" to codigo.estado,
            "plataforma_registro" to "app_kotlin",
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("codigos_clientes").document(codigo.id.ifBlank { codigo.codigo })
            .set(data)
            .addOnFailureListener { e -> Log.w(TAG, "codigos_clientes (${codigo.id}): ${e.message}") }
    }

    fun escucharCodigosClientes(onChange: (List<CodigoCliente>) -> Unit): ListenerRegistration =
        firestore.collection("codigos_clientes").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            onChange(snap.documents.mapNotNull { doc ->
                doc.toObject(CodigoCliente::class.java)?.copy(id = doc.id)
            })
        }

    // ---------- GPS EN VIVO -> colección "promotores_ubicacion" ----------

    fun actualizarUbicacionPromotor(promotorId: String, nombrePromotor: String, lat: Double, lng: Double) = ensureAuth {
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
        firestore.collection("promotores_ubicacion").document(promotorId).set(data)
            .addOnFailureListener { e -> Log.w(TAG, "ubicación ($promotorId): ${e.message}") }
    }

    fun escucharUbicaciones(onChange: (List<PromotorUbicacion>) -> Unit): ListenerRegistration =
        firestore.collection("promotores_ubicacion").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            onChange(snap.documents.mapNotNull { it.toObject(PromotorUbicacion::class.java) })
        }

    // ---------- USUARIOS -> colección "usuarios" ----------

    fun escucharUsuarios(onChange: (List<PerfilUsuario>) -> Unit): ListenerRegistration =
        firestore.collection("usuarios").addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            onChange(snap.documents.mapNotNull { doc ->
                doc.toObject(PerfilUsuario::class.java)?.copy(uuid = doc.id)
            })
        }

    fun activarDesactivarUsuario(uuid: String, activo: Boolean, solicitante: PerfilUsuario?) {
        if (!solicitante.checkIsAdmin()) {
            Log.w(TAG, "Intento de modificar usuario sin permisos de administrador")
            return
        }
        firestore.collection("usuarios").document(uuid).update("esActivo", activo)
    }

    fun cambiarRolUsuario(uuid: String, nuevoRol: String, solicitante: PerfilUsuario?) {
        if (!solicitante.checkIsAdmin()) {
            Log.w(TAG, "Intento de cambiar rol sin permisos de administrador")
            return
        }
        firestore.collection("usuarios").document(uuid).update("rol", nuevoRol)
    }

    // ---------- LOGIN SEGURO ----------

    fun loginSeguro(
        email: String,
        clave: String,
        onResult: (Boolean, String, PerfilUsuario?) -> Unit
    ) {
        val correo = email.trim().lowercase()
        if (correo.isBlank() || !correo.contains("@") || clave.isBlank()) {
            onResult(false, "Ingrese un correo electrónico y contraseña válidos.", null)
            return
        }

        auth.signInWithEmailAndPassword(correo, clave)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                firestore.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { doc ->
                        val perfil = doc.toObject(PerfilUsuario::class.java)?.copy(uuid = uid)
                            ?: PerfilUsuario(
                                uuid = uid,
                                correo = correo,
                                nombre = correo.substringBefore("@"),
                                usuario = correo.substringBefore("@"),
                                rol = "Promotor(a)",
                                esActivo = true
                            )
                        if (!perfil.esActivo) {
                            onResult(false, "Este usuario está desactivado. Contacte a un administrador.", null)
                        } else {
                            onResult(true, "¡Bienvenido, ${perfil.nombre}!", perfil)
                        }
                    }
                    .addOnFailureListener {
                        onResult(false, "No se pudo cargar el perfil del usuario.", null)
                    }
            }
            .addOnFailureListener { e ->
                onResult(false, "Correo o contraseña incorrectos.", null)
                Log.w(TAG, "Login fallido para $correo: ${e.message}")
            }
    }

    fun crearUsuarioPromotor(
        nombre: String,
        correo: String,
        claveTemporal: String,
        rol: String = "Promotor(a)",
        solicitante: PerfilUsuario?,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!solicitante.checkIsAdmin()) {
            onResult(false, "Solo un Administrador puede crear usuarios.")
            return
        }
        if (claveTemporal.length < 6) {
            onResult(false, "La contraseña temporal debe tener al menos 6 caracteres.")
            return
        }
        auth.createUserWithEmailAndPassword(correo.trim().lowercase(), claveTemporal)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: return@addOnSuccessListener
                val perfil = hashMapOf(
                    "nombre" to nombre,
                    "usuario" to correo.substringBefore("@"),
                    "correo" to correo.trim().lowercase(),
                    "rol" to rol,
                    "esActivo" to true
                )
                firestore.collection("usuarios").document(uid).set(perfil)
                    .addOnSuccessListener { onResult(true, "Usuario creado correctamente.") }
                    .addOnFailureListener { e -> onResult(false, "Usuario creado en Auth pero falló al guardar perfil: ${e.message}") }
            }
            .addOnFailureListener { e -> onResult(false, "No se pudo crear el usuario: ${e.message}") }
    }
}

class FirestoreWebSync(private val firestore: FirebaseFirestore) {
    private val sync = TecnicableSync(firestore)

    fun subirClienteApp(contrato: ContratoDiario) {
        sync.subirClienteApp(contrato)
    }

    fun subirProspectoCenso(prospecto: ProspectoCenso) {
        sync.subirProspectoCenso(prospecto)
    }

    fun actualizarUbicacionPromotor(promotorId: String, nombrePromotor: String, lat: Double, lng: Double) {
        sync.actualizarUbicacionPromotor(promotorId, nombrePromotor, lat, lng)
    }
}


