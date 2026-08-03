# Tecnicable Backend as a Service (BaaS) - Arquitectura Ktor

Este backend modular constituye un motor de servicios desacoplados de alto rendimiento para soportar múltiples usuarios técnicos de campo en tiempo real, garantizando seguridad a nivel de registros (RLS lógico) de forma nativa.

## 🛠️ Stack Tecnológico
* **Lenguaje:** Kotlin 1.9+
* **Framework:** Ktor Server (Netty Engine)
* **Base de Datos:** PostgreSQL
* **ORM / Acceso a Datos:** JetBrains Exposed (Core & JDBC)
* **Autenticación:** JWT (JSON Web Tokens) con contraseñas encriptadas mediante BCrypt
* **Sincronización:** WebSockets nativos de Ktor con filtrado por identificador de usuario único.

---

## 📂 Estructura de Archivos Creados
El backend se encuentra completamente desacoplado y modularizado en su espacio de la siguiente manera:
```text
/backend/src/main/kotlin/com/tecnicable/backend/
│
├── Application.kt                     # Punto de entrada, configuración de plugins y servidores Netty
│
├── config/
│   └── DatabaseFactory.kt             # Inicialización de PostgreSQL, conexión y autogeneración de esquema
│
├── model/
│   └── Models.kt                      # Definición de tablas en Exposed, modelos de datos y DTOs serializables
│
├── security/
│   ├── JwtService.kt                  # Generación y validación de tokens JWT (Válidos por 24 horas)
│   └── PasswordHasher.kt              # Algoritmo de hasheo seguro BCrypt (salt factor 12)
│
└── routes/
    ├── AuthRoutes.kt                  # Control de autenticación (/auth/register , /auth/login)
    ├── FormRoutes.kt                  # CRUD Seguro de formularios con restricción por token (/api/forms)
    └── WebSocketRoutes.kt             # Transmisión en tiempo real por WebSocket (/ws/forms)
```

---

## ⚙️ Cómo Levantar el Servidor Localmente

### 1. Prerrequisitos
* **Java JDK 17 o superior** instalado en el equipo de desarrollo.
* **Base de Datos PostgreSQL** instalada y activa.

### 2. Configuración de PostgreSQL
Inicie sesión en su consola o motor de administración de PostgreSQL (e.g., pgAdmin) y ejecute la consulta inicial para crear la base de datos necesaria:
```sql
CREATE DATABASE tecnicable_baas;
```

> **Nota:** Si sus credenciales locales de postgres difieren (por ejemplo, si usa otra clave u otro usuario), edite el archivo `DatabaseFactory.kt` con su JDBC correspondiente:
> ```kotlin
> val jdbcUrl = "jdbc:postgresql://localhost:5432/tecnicable_baas"
> val user = "postgres"
> val password = "su_contrasena"
> ```

### 3. Ejecución del Servidor
Abra su terminal en la raíz del proyecto backend y ejecute gradle para iniciar el servidor Netty incorporado en el puerto `8080`:
```bash
./gradlew run
```
Si todo se levanta correctamente, verá el mensaje del sistema de que el servidor está escuchando en el puerto local y verificando la base de datos de manera automatizada:
`[main] INFO  org.eclipse.jetty.util.log - Logging initialized...`
`Base de datos PostgreSQL inicializada con Exposed correctamente.`

---

## ⚡ Guía de Integración para el Cliente (Frontend)

A continuación se detalla cómo el cliente móvil (Android Compose con Retrofit) u otra UI web realiza las integraciones con el backend:

### 1. Flujo de Autenticación (JWT)
El cliente debe realizar la solicitud POST enviando el objeto de credenciales en formato JSON.

#### Entrada: `POST /auth/login`
```json
{
  "email": "tecnico@tecnicable.com",
  "password": "clave_segura_123"
}
```

#### Respuesta: `200 OK`
Al autenticarse correctamente, el backend retorna el token JWT y los datos primarios para persistencia local:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiO...",
  "email": "tecnico@tecnicable.com",
  "userId": "b3eacd53-91a3-4876-9214-41d5757d5985"
}
```

El cliente debe almacenar este **`token`** de manera segura (e.g., utilizando *EncryptedSharedPreferences* o *DataStore* en Android) para adjuntarlo en las cabeceras de toda petición REST subsecuente.

---

### 2. Rutas Protegidas de Formularios (Logical Row-Level Security)
Al hacer peticiones a las rutas de formularios en `/api/forms`, el cliente debe adjuntar la cabecera del token de la siguiente forma:

#### Petición HTTP Ejemplo
```http
GET /api/forms HTTP/1.1
Host: localhost:8080
Authorization: Bearer <token_jwt_almacenado>
Content-Type: application/json
```

**Comportamiento en el Servidor (Row-Level Security):** El backend descifra el token mediante el middleware JWT de Ktor, extrae la propiedad `userId` (`"b3eacd53-91a3-4876-9214-..."`) y filtra los registros en PostgreSQL garantizando que un usuario técnico jamás pueda visualizar o modificar el formulario guardado por otro usuario.

---

### 3. Conectividad en Tiempo Real (WebSocket Channel)
El cliente se conecta al canal TCP proporcionado por el WebSocket mandando el token como query parameter para autorizar el enlace:

#### Conexión WebSocket
```text
WS ws://localhost:8080/ws/forms?token=<token_jwt_almacenado>
```

#### Ejemplo de Código de Conexión en Frontend (Kotlin en Android/Ktor Client)
```kotlin
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*

val client = HttpClient {
    install(WebSockets)
}

suspend fun escucharActualizaciones(tokenJWT: String) {
    client.webSocket(host = "localhost", port = 8080, path = "/ws/forms?token=$tokenJWT") {
        println("Conectado con éxito al túnel WebSocket en tiempo real")
        
        // Bucle continuo para recibir notificaciones cuando se agregue o modifique un formulario en la central
        for (frame in incoming) {
            when (frame) {
                is Frame.Text -> {
                    val jsonResponse = frame.readText()
                    println("Actualización recibida del servidor: $jsonResponse")
                    // Ejemplo de Payload de actualización recibido:
                    // {
                    //   "action": "CREATE",
                    //   "id": "787fbebc-601d-4ebc-b397...",
                    //   "data": "{...JSON del Formulario...}",
                    //   "timestamp": 1780075502
                    // }
                    
                    // Aquí se integra con la base de datos Room u UI de Compose local de inmediato
                    actualizarUIConDatosNuevos(jsonResponse)
                }
                else -> {}
            }
        }
    }
}
```

Cuando un técnico guarde un formulario mediante un dispositivo, el resto de dispositivos (tablet, laptop o móvil) que estén con la misma sesión (`userId`) recibirán instantáneamente la trama JSON, logrando una sincronización multiusuario en vivo y transparente.
