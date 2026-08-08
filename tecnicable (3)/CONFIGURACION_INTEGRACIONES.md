# 📡 CONFIGURACIÓN DE CONEXIÓN: CORREO Y TELEGRAM (TECNICABLE PRO)

Este documento contiene la especificación completa, endpoints, tokens, datos de configuración y plantillas de código para las integraciones de **Correo Electrónico (Google Apps Script)** y **Telegram Bot API**.

---

## 1. 📧 INTEGRACIÓN DE CORREO ELECTRÓNICO (GOOGLE APPS SCRIPT BYPASS)

### 📌 Parámetros de Conexión
- **Endpoint URL:** `https://script.google.com/macros/s/AKfycbzNk-t_XhmSzeJGtTk8RnBkcMjAkRGB1em3vJ1kFCLExM6sADJss3mLAsrhwWH2zias/exec`
- **Método HTTP:** `POST`
- **Content-Type:** `application/json; charset=utf-8`
- **Token de Seguridad (Secret Key):** `Tecnicable2026*SecureKey`

### 📄 Payload JSON (Estructura de Envío)
```json
{
  "token": "Tecnicable2026*SecureKey",
  "para": "cliente@ejemplo.com",
  "asunto": "Copia de Ficha de Servicio Tecnicable - Instalación N° TC26-1001",
  "cuerpo": "<div style=\"font-family: Arial;\">... Plantilla HTML de Ficha Digital ...</div>"
}
```

### 📜 Código del Servidor Google Apps Script (`Code.gs`)
```javascript
function doPost(e) {
  try {
    var data = JSON.parse(e.postData.contents);
    
    // Verificación de Token de Seguridad
    if (data.token !== "Tecnicable2026*SecureKey") {
      return ContentService.createTextOutput(JSON.stringify({
        "status": "error",
        "message": "Token de seguridad no válido"
      })).setMimeType(ContentService.MimeType.JSON);
    }
    
    var recipient = data.para;
    var subject = data.asunto;
    var htmlBody = data.cuerpo;
    
    // Envío a través de la API nativa de Google Mail
    MailApp.sendEmail({
      to: recipient,
      subject: subject,
      htmlBody: htmlBody,
      name: "Tecnicable Operaciones"
    });
    
    return ContentService.createTextOutput(JSON.stringify({
      "status": "success",
      "message": "Correo enviado exitosamente"
    })).setMimeType(ContentService.MimeType.JSON);
    
  } catch (error) {
    return ContentService.createTextOutput(JSON.stringify({
      "status": "error",
      "message": error.toString()
    })).setMimeType(ContentService.MimeType.JSON);
  }
}
```

---

## 2. ✈️ INTEGRACIÓN DE TELEGRAM BOT API

### 📌 Parámetros de Conexión
- **Bot Token Config:** Inyectado mediante `BuildConfig.TELEGRAM_BOT_TOKEN` (o variable `.env` `TELEGRAM_BOT_TOKEN`).
- **Canal / Chat ID Objetivo:** `-1004291177890` (Canal de Operaciones Tecnicable).

### 📨 A. Envío de Mensaje Texto Estructurado (Reportes / Cierres / Nuevos Contratos)
- **Endpoint URL:** `https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/sendMessage`
- **Método HTTP:** `POST`
- **Content-Type:** `application/json; charset=utf-8`

#### Payload JSON:
```json
{
  "chat_id": "-1004291177890",
  "parse_mode": "HTML",
  "text": "🚀 <b>ENTREGA AUTOMÁTICA DE INFORMACIÓN</b>\n--------------------------------------------\n<b>📁 DETALLES DEL CONTRATO / INSTALACIÓN</b>\n• <b>N° de Instalación:</b> TC26-1001\n• <b>Fecha de Registro:</b> 07/08/2026\n\n<b>👤 DATOS PERSONALES DEL CLIENTE</b>\n• <b>Nombre Completo:</b> Pedro Pérez\n• <b>Cédula de Identidad:</b> V-12345678\n• <b>Teléfono Celular:</b> 0414-1234567\n• <b>Correo Electrónico:</b> cliente@gmail.com\n\n<b>💰 INFORMACIÓN DE PAGO Y PLAN</b>\n• <b>Plan CONTRATADO:</b> Plan Básico 400 Mbps (Tarifa US$ 30)\n• <b>Monto del Pago:</b> $30\n• <b>Forma de Pago:</b> Divisas\n\n<b>📍 GEOLOCALIZACIÓN GPS (MAPAS)</b>\n• <b>Coordenadas Cliente:</b> <code>10.95750, -63.86940</code>\n👉 <a href=\"https://www.google.com/maps/search/?api=1&query=10.95750,-63.86940\">Ver Ubicación del Cliente en Google Maps</a>"
}
```

---

### 🖼️ B. Envío de Fotografías y Firma Digital
- **Endpoint URL:** `https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/sendPhoto`
- **Método HTTP:** `POST`
- **Content-Type:** `multipart/form-data`

#### Campos Multipart:
| Campo | Tipo | Valor / Descripción |
| :--- | :--- | :--- |
| `chat_id` | Text | `-1004291177890` |
| `parse_mode` | Text | `HTML` |
| `photo` | File (Binary) | Archivo JPG/PNG de la foto o firma |
| `caption` | Text | Pie de foto con datos del contrato |

#### Ejemplo de Caption (Pie de foto):
```html
🪪 <b>DOCUMENTO DE IDENTIDAD</b>
--------------------------------------------
👤 <b>Cliente:</b> Pedro Pérez
🔑 <b>N° Instalación:</b> TC26-1001
📝 Foto de Cédula/DNI cargada desde la aplicación.
```

---

## 🔒 NOTAS DE SEGURIDAD
1. **Google Apps Script**: La URL del webhook gestiona el envío de correos sin requerir contraseñas SMTP locales en el dispositivo móvil del promotor.
2. **Telegram Bot**: Toda la comunicación utiliza HTTPS encriptado directamente con los servidores oficiales de Telegram (`api.telegram.org`).
