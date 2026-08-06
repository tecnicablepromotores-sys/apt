package com.example

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contratos")
data class ContratoDiario(
    val id: Long = 0,
    @PrimaryKey val uuid: String = java.util.UUID.randomUUID().toString(),
    val nroInstalacion: String = "",
    val nombreCliente: String = "",
    val cedula: String = "",
    val celular: String = "",
    val correo: String = "",
    val fechaNacimiento: String = "",
    val plan: String = "",
    val tipoOnu: String = "",
    val tipoServicio: String = "",
    val metodoPago: String = "",
    val monto: String = "",
    val referenciaPago: String = "",
    val puntoReferencia: String = "",
    val direccion: String = "",
    val fecha: String = "",
    val latitud: Double? = null,
    val longitud: Double? = null,
    val latitudCaja: Double? = null,
    val longitudCaja: Double? = null,
    val firmaUri: String? = null,
    val fotoClientUri: String? = null,
    val fotoCajaUri: String? = null,
    val firmaBase64: String? = null,
    val fotoClientBase64: String? = null,
    val fotoCajaBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tecnicoNombre: String = "Técnico General",
    val cerrado: Boolean = false
)

@Entity(tableName = "perfiles")
data class PerfilUsuario(
    var id: Long = 0,
    @PrimaryKey var uuid: String = "",
    var nombre: String = "",
    var usuario: String = "",
    var rol: String = "Promotor(a)",
    var celular: String = "",
    var esActivo: Boolean = false,
    var clave: String = "tecnicable",
    var cedula: String = "",
    var correo: String = ""
)

fun PerfilUsuario?.checkIsAdmin(): Boolean {
    if (this == null) return false
    val c = correo.trim().lowercase()
    val u = usuario.trim().lowercase()
    val n = nombre.trim().lowercase()
    val r = rol.trim()
    return c == "luifred1998@gmail.com" || 
           c == "tecnicablesedemargarita@gmail.com" || 
           c == "admin@tecnicable.com" || 
           c.contains("luifred") || 
           c.contains("tecnicablesede") ||
           u.contains("luifred") ||
           u.contains("tecnicablesede") ||
           u == "tecnicable" ||
           n.contains("luifred") ||
           n.contains("luifred lópez") ||
           n.contains("luifred lopez") ||
           cedula.trim() == "26625329" ||
           uuid.trim() == "f9286c30-4a28-40e6-9e47-8490c65e03b" ||
           r.equals("Administrador", ignoreCase = true) ||
           r.equals("Admin", ignoreCase = true)
}

fun PerfilUsuario?.checkIsLuifred(): Boolean {
    if (this == null) return false
    val c = correo.trim().lowercase()
    val u = usuario.trim().lowercase()
    val n = nombre.trim().lowercase()
    return c == "luifred1998@gmail.com" || 
           c.contains("luifred") || 
           u.contains("luifred") || 
           n.contains("luifred") ||
           cedula.trim() == "26625329" ||
           uuid.trim() == "f9286c30-4a28-40e6-9e47-8490c65e03b"
}

data class CodigoCliente(
    val id: String = "",
    val codigo: String = "",
    val clienteNombre: String = "",
    val planAsignado: String = "Fibra 100Mbps",
    val estado: String = "Activo"
)

@Entity(tableName = "prospectos_censo")
data class ProspectoCenso(
    @PrimaryKey val id: String = "",
    val nombreCompleto: String = "",
    val cedula: String = "",
    val telefono: String = "",
    val zona: String = "",
    val estatus: String = "Pendiente",
    val timestamp: Long = System.currentTimeMillis(),
    val usuarioGestor: String = ""
)

@Entity(tableName = "contratos_web")
data class ContratoWeb(
    val id: Long = 0,
    @PrimaryKey val uuid: String = java.util.UUID.randomUUID().toString(),
    val nroInstalacion: String = "",
    val nombreCliente: String = "",
    val cedula: String = "",
    val celular: String = "",
    val correo: String = "",
    val plan: String = "",
    val metodoPago: String = "",
    val monto: String = "",
    val referenciaPago: String = "",
    val puntoReferencia: String = "",
    val direccion: String = "",
    val fecha: String = "",
    val foto_frente_base64: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val tecnicoNombre: String = "Soporte Web",
    val estado: String = "Pendiente" // Pendiente, Procesado, Rechazado
)

data class PromotorUbicacion(
    val usuario: String = "",
    val nombre: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis()
)

