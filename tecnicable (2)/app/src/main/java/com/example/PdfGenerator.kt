package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    // Helper to draw clean styled section headers
    private fun drawSectionHeader(canvas: Canvas, title: String, y: Float, paint: Paint, textPaint: Paint, width: Int) {
        paint.color = Color.parseColor("#EEF2F6")
        canvas.drawRect(20f, y - 14f, width - 20f, y + 6f, paint)
        
        paint.color = Color.parseColor("#0C2340")
        canvas.drawRect(20f, y - 14f, 25f, y + 6f, paint)
        
        textPaint.color = Color.parseColor("#0C2340")
        textPaint.isFakeBoldText = true
        textPaint.textSize = 10f
        canvas.drawText(title, 35f, y, textPaint)
    }

    // Helper to draw key-value pairs
    private fun drawLabelValue(canvas: Canvas, label: String, value: String, xLabel: Float, xValue: Float, y: Float, textPaint: Paint) {
        textPaint.isFakeBoldText = true
        textPaint.color = Color.parseColor("#555555")
        canvas.drawText(label, xLabel, y, textPaint)
        
        textPaint.isFakeBoldText = false
        textPaint.color = Color.BLACK
        canvas.drawText(value, xValue, y, textPaint)
    }

    // Helper to decode Base64 data strings directly to Bitmaps for PDF embedding
    private fun loadBase64ToBitmap(base64Str: String?, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            var cleanStr = base64Str.trim().substringAfter(",")
            
            val bytes = try {
                android.util.Base64.decode(cleanStr, android.util.Base64.DEFAULT)
            } catch (e1: Exception) {
                try {
                    android.util.Base64.decode(cleanStr, android.util.Base64.NO_WRAP)
                } catch (e2: Exception) {
                    try {
                        android.util.Base64.decode(cleanStr, android.util.Base64.URL_SAFE)
                    } catch (e3: Exception) {
                        android.util.Base64.decode(cleanStr, android.util.Base64.NO_PADDING)
                    }
                }
            }
            
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            
            var scale = 1
            while (options.outWidth / scale / 2 >= maxWidth && options.outHeight / scale / 2 >= maxHeight) {
                scale *= 2
            }
            
            val scaleOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, scaleOptions)
            decoded?.let {
                Bitmap.createScaledBitmap(it, maxWidth, maxHeight, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Helper to load secure device photos safely
    private fun loadUriToBitmap(context: Context, uriString: String?, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)
            val inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate scaled-down resolution to prevent memory limits
            var scale = 1
            while (options.outWidth / scale / 2 >= maxWidth && options.outHeight / scale / 2 >= maxHeight) {
                scale *= 2
            }

            val scaleOptions = BitmapFactory.Options().apply {
                inSampleSize = scale
            }
            val stream = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(stream, null, scaleOptions)
            stream?.close()
            
            decoded?.let {
                Bitmap.createScaledBitmap(it, maxWidth, maxHeight, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 1. REPORT OF MULTIPLE CONTRACTS (LIST)
    fun generateContractsPdf(context: Context, periodTitle: String, contratos: List<ContratoDiario>) {
        val pdfDocument = PdfDocument()
        
        val itemsPerPage = 18
        val chunks = contratos.chunked(itemsPerPage)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedDate = sdf.format(Date())
        
        if (chunks.isEmpty()) {
            Toast.makeText(context, "No hay contratos para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pageWidth = 595
        val pageHeight = 842
        
        chunks.forEachIndexed { pageIndex, pageItems ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isAntiAlias = true
            }
            
            paint.color = Color.parseColor("#0C2340")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 55f, paint)
            
            textPaint.color = Color.WHITE
            textPaint.textSize = 14f
            textPaint.isFakeBoldText = true
            canvas.drawText("TECNICABLE • REPORTE DETALLADO DE CONTRATOS", 25f, 32f, textPaint)
            
            textPaint.textSize = 8f
            textPaint.isFakeBoldText = false
            canvas.drawText("Sede Margarita • Operaciones de Campo", pageWidth - 200f, 32f, textPaint)
            
            textPaint.color = Color.BLACK
            
            var yPos = 85f
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = true
            canvas.drawText("Filtro de Consulta: $periodTitle", 25f, yPos, textPaint)
            
            textPaint.isFakeBoldText = false
            textPaint.textSize = 9f
            canvas.drawText("Fecha de Emisión: $generatedDate", pageWidth - 210f, yPos, textPaint)
            
            yPos += 15f
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(20f, yPos, pageWidth - 20f, yPos, paint)
            
            yPos += 20f
            textPaint.isFakeBoldText = true
            textPaint.textSize = 9f
            textPaint.color = Color.parseColor("#0C2340")
            canvas.drawText("N° Instalación", 25f, yPos, textPaint)
            canvas.drawText("Abonado / Cliente", 110f, yPos, textPaint)
            canvas.drawText("Documento/Cédula", 250f, yPos, textPaint)
            canvas.drawText("Monto", 360f, yPos, textPaint)
            canvas.drawText("Mégas / Plan", 410f, yPos, textPaint)
            canvas.drawText("Vendedor / Técnico", 490f, yPos, textPaint)
            
            yPos += 8f
            paint.color = Color.parseColor("#BDBDBD")
            canvas.drawLine(20f, yPos, pageWidth - 20f, yPos, paint)
            
            pageItems.forEachIndexed { itemIndex, item ->
                yPos += 24f
                textPaint.isFakeBoldText = false
                textPaint.textSize = 8.5f
                textPaint.color = Color.BLACK
                
                val nameTruncated = if (item.nombreCliente.length > 25) item.nombreCliente.take(23) + "..." else item.nombreCliente
                val planTruncated = if (item.plan.contains("Mbps")) item.plan.substringBefore(" (") else item.plan
                val tecTruncated = if (item.tecnicoNombre.length > 15) item.tecnicoNombre.substringBefore(" ") else item.tecnicoNombre
                
                canvas.drawText(item.nroInstalacion, 25f, yPos, textPaint)
                canvas.drawText(nameTruncated, 110f, yPos, textPaint)
                canvas.drawText(item.cedula, 250f, yPos, textPaint)
                val formattedMontoInList = if (item.metodoPago.startsWith("Bolívares")) "Bs. ${item.monto}" else "$${item.monto}"
                canvas.drawText(formattedMontoInList, 360f, yPos, textPaint)
                canvas.drawText(planTruncated, 410f, yPos, textPaint)
                canvas.drawText(tecTruncated, 490f, yPos, textPaint)
                
                paint.color = Color.parseColor("#F5F5F5")
                canvas.drawLine(20f, yPos + 6f, pageWidth - 20f, yPos + 6f, paint)
            }
            
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(20f, pageHeight - 40f, pageWidth - 20f, pageHeight - 40f, paint)
            
            textPaint.textSize = 7.5f
            textPaint.color = Color.GRAY
            canvas.drawText("Sede Administrativa Tecnicable Margarita C.A. • Todos los derechos reservados.", 25f, pageHeight - 25f, textPaint)
            canvas.drawText("Página ${pageIndex + 1} de ${chunks.size}", pageWidth - 100f, pageHeight - 25f, textPaint)
            
            pdfDocument.finishPage(page)
        }
        
        val fileName = "Tecnicable_Reporte_${periodTitle.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val file = File(directory, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(context, "Reporte guardado localmente: ${file.name}", Toast.LENGTH_LONG).show()
            
            // Mirror to public Downloads folder for global accessibility
            saveToPublicDownloads(context, file, fileName)
            
            // Instantly prompt to Open or Share the PDF
            openOrSharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar archivo: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    // 3. CENSO DE PROSPECTOS REPORT (LIST)
    fun generateCensoPdf(context: Context, prospectos: List<ProspectoCenso>) {
        val pdfDocument = PdfDocument()
        
        val itemsPerPage = 22
        val chunks = prospectos.chunked(itemsPerPage)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedDate = sdf.format(Date())
        
        if (chunks.isEmpty()) {
            Toast.makeText(context, "No hay prospectos en el censo para exportar", Toast.LENGTH_SHORT).show()
            return
        }
        
        val pageWidth = 595
        val pageHeight = 842
        
        chunks.forEachIndexed { pageIndex, pageItems ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            
            val paint = Paint()
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                isAntiAlias = true
            }
            
            // Header panel in dark navy
            paint.color = Color.parseColor("#0C2340")
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 55f, paint)
            
            textPaint.color = Color.WHITE
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = true
            canvas.drawText("TECNICABLE • REPORTE DE CENSO Y PROSPECTOS", 25f, 32f, textPaint)
            
            textPaint.textSize = 8f
            textPaint.isFakeBoldText = false
            canvas.drawText("Sede Margarita • Filtro General", pageWidth - 160f, 32f, textPaint)
            
            textPaint.color = Color.BLACK
            
            var yPos = 85f
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = true
            canvas.drawText("Listado de Interesados Registrados", 25f, yPos, textPaint)
            
            textPaint.isFakeBoldText = false
            textPaint.textSize = 9f
            canvas.drawText("Fecha de Emisión: $generatedDate", pageWidth - 210f, yPos, textPaint)
            
            yPos += 15f
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(20f, yPos, pageWidth - 20f, yPos, paint)
            
            // Table headers
            yPos += 20f
            textPaint.isFakeBoldText = true
            textPaint.textSize = 9f
            textPaint.color = Color.parseColor("#0C2340")
            canvas.drawText("N°", 25f, yPos, textPaint)
            canvas.drawText("Nombre Completo o Razón Social", 55f, yPos, textPaint)
            canvas.drawText("Cédula / RIF", 230f, yPos, textPaint)
            canvas.drawText("Teléfono", 315f, yPos, textPaint)
            canvas.drawText("Zona", 415f, yPos, textPaint)
            canvas.drawText("Estatus", 500f, yPos, textPaint)
            
            yPos += 8f
            paint.color = Color.parseColor("#BDBDBD")
            canvas.drawLine(20f, yPos, pageWidth - 20f, yPos, paint)
            
            pageItems.forEachIndexed { itemIndex, item ->
                yPos += 24f
                textPaint.isFakeBoldText = false
                textPaint.textSize = 8.5f
                textPaint.color = Color.BLACK
                
                val globalIndex = (pageIndex * itemsPerPage) + itemIndex + 1
                val nameTruncated = if (item.nombreCompleto.length > 30) item.nombreCompleto.take(28) + "..." else item.nombreCompleto
                val zoneTruncated = if (item.zona.length > 15) item.zona.take(13) + "..." else item.zona
                val statusUpper = item.estatus.uppercase()
                
                canvas.drawText(globalIndex.toString(), 25f, yPos, textPaint)
                canvas.drawText(nameTruncated, 55f, yPos, textPaint)
                canvas.drawText(item.cedula, 230f, yPos, textPaint)
                canvas.drawText(item.telefono, 315f, yPos, textPaint)
                canvas.drawText(zoneTruncated, 415f, yPos, textPaint)
                
                // Color status text or draw small background
                if (item.estatus.contains("Pendiente", ignoreCase = true)) {
                    textPaint.color = Color.parseColor("#7C3AED") // Purple
                } else if (item.estatus.contains("Contactado", ignoreCase = true)) {
                    textPaint.color = Color.parseColor("#2563EB") // Blue
                } else if (item.estatus.contains("Sin factibilidad", ignoreCase = true)) {
                    textPaint.color = Color.parseColor("#DC2626") // Red
                } else {
                    textPaint.color = Color.parseColor("#16A34A") // Green
                }
                textPaint.isFakeBoldText = true
                canvas.drawText(statusUpper, 500f, yPos, textPaint)
                
                paint.color = Color.parseColor("#F5F5F5")
                canvas.drawLine(20f, yPos + 6f, pageWidth - 20f, yPos + 6f, paint)
            }
            
            paint.color = Color.parseColor("#E0E0E0")
            canvas.drawLine(20f, pageHeight - 40f, pageWidth - 20f, pageHeight - 40f, paint)
            
            textPaint.textSize = 7.5f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.GRAY
            canvas.drawText("Sede Administrativa Tecnicable Margarita C.A. • Todos los derechos reservados.", 25f, pageHeight - 25f, textPaint)
            canvas.drawText("Página ${pageIndex + 1} de ${chunks.size}", pageWidth - 100f, pageHeight - 25f, textPaint)
            
            pdfDocument.finishPage(page)
        }
        
        val fileName = "Tecnicable_Reporte_Censo_${System.currentTimeMillis()}.pdf"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val file = File(directory, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(context, "Reporte de censo guardado: ${file.name}", Toast.LENGTH_LONG).show()
            
            // Mirror to public Downloads folder for global accessibility
            saveToPublicDownloads(context, file, fileName)
            
            // Instantly prompt to Open or Share the PDF
            openOrSharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar archivo de censo: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    // 2. EXPORT ALL DATA OF A SINGLE SPECIFIC CONTRACT TO A SINGLE PDF FILE
    fun generateSingleContractPdf(context: Context, contrato: ContratoDiario) {
        val pdfDocument = PdfDocument()
        
        val pageWidth = 595
        val pageHeight = 842
        
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        
        val paint = Paint()
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isAntiAlias = true
        }
        
        // 1. Dark Navy Title Banner
        paint.color = Color.parseColor("#0C2340")
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 65f, paint)
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 15f
        textPaint.isFakeBoldText = true
        canvas.drawText("TECNICABLE MARGARITA • REGISTRO DE INSTALACIÓN", 25f, 38f, textPaint)
        
        textPaint.textSize = 8.5f
        textPaint.isFakeBoldText = false
        canvas.drawText("Soporte de Suscriptor HFC/GPON Óptico", pageWidth - 200f, 38f, textPaint)
        
        // 2. Under-banner Contract Highlight
        var yPos = 100f
        textPaint.color = Color.parseColor("#0C2340")
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas.drawText("CONTRATO N°: ${contrato.nroInstalacion}", 25f, yPos, textPaint)
        
        textPaint.color = Color.DKGRAY
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false
        canvas.drawText("Fecha de Registro: ${contrato.fecha}", pageWidth - 220f, yPos, textPaint)
        
        yPos += 15f
        paint.color = Color.parseColor("#CFCECE")
        canvas.drawLine(20f, yPos, pageWidth - 20f, yPos, paint)
        
        // 3. SECTION 1: DATOS PERSONALES DEL SUSCRIPTOR
        yPos += 25f
        drawSectionHeader(canvas, "1. DATOS PERSONALES DEL SUSCRIPTOR", yPos, paint, textPaint, pageWidth)
        
        yPos += 25f
        drawLabelValue(canvas, "Nombre Completo: ", contrato.nombreCliente, 30f, 130f, yPos, textPaint)
        drawLabelValue(canvas, "Cédula / RIF: ", contrato.cedula, 320f, 400f, yPos, textPaint)
        
        yPos += 20f
        drawLabelValue(canvas, "Teléfono Celular: ", contrato.celular, 30f, 130f, yPos, textPaint)
        drawLabelValue(canvas, "Correo Electrónico: ", contrato.correo.ifBlank { "No Registrado" }, 320f, 420f, yPos, textPaint)
        
        yPos += 20f
        drawLabelValue(canvas, "Fecha de Nac.: ", contrato.fechaNacimiento.ifBlank { "No Registrada" }, 30f, 130f, yPos, textPaint)
        drawLabelValue(canvas, "Técnico Responsable: ", contrato.tecnicoNombre, 320f, 430f, yPos, textPaint)

        // 4. SECTION 2: DETALLES DEL PLAN Y FORMA DE PAGO
        yPos += 30f
        drawSectionHeader(canvas, "2. DETALLES DE PLAN ADQUIRIDO Y CONFIGURACIÓN", yPos, paint, textPaint, pageWidth)
        
        yPos += 25f
        drawLabelValue(canvas, "Plan Contratado: ", contrato.plan, 30f, 130f, yPos, textPaint)
        drawLabelValue(canvas, "Método de Pago: ", contrato.metodoPago, 320f, 410f, yPos, textPaint)
        
        yPos += 20f
        if (contrato.metodoPago.startsWith("Bolívares")) {
            drawLabelValue(canvas, "Monto Cobrado (Bs): ", "Bs. ${contrato.monto}", 30f, 145f, yPos, textPaint)
        } else {
            drawLabelValue(canvas, "Monto Cobrado (US$): ", "$${contrato.monto}", 30f, 145f, yPos, textPaint)
        }
        drawLabelValue(canvas, "Referencia de Pago: ", contrato.referenciaPago.ifBlank { "Sin Referencia o Efectivo" }, 320f, 430f, yPos, textPaint)

        // 5. SECTION 3: DATOS GEOGRÁFICOS DE LA INSTALACIÓN
        yPos += 30f
        drawSectionHeader(canvas, "3. LOCALIZACIÓN Y GEOFACILIDADES", yPos, paint, textPaint, pageWidth)
        
        yPos += 25f
        // Splitting or wrapping long addresses safely
        val fullDireccion = contrato.direccion
        val addressLines = if (fullDireccion.length > 75) {
            listOf(fullDireccion.take(75), fullDireccion.substring(75).take(75))
        } else {
            listOf(fullDireccion)
        }
        
        drawLabelValue(canvas, "Dirección Completa: ", addressLines.firstOrNull() ?: "No Indicada", 30f, 140f, yPos, textPaint)
        if (addressLines.size > 1) {
            yPos += 15f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.BLACK
            canvas.drawText(addressLines[1], 140f, yPos, textPaint)
        }
        
        yPos += 20f
        drawLabelValue(canvas, "Punto de Referencia: ", contrato.puntoReferencia.ifBlank { "No Indicado" }, 30f, 140f, yPos, textPaint)
        
        yPos += 20f
        val userCoords = if (contrato.latitud != null && contrato.longitud != null) {
            String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f", contrato.latitud, contrato.longitud)
        } else {
            "No Registrado"
        }
        val boxCoords = if (contrato.latitudCaja != null && contrato.longitudCaja != null) {
            String.format(Locale.getDefault(), "Lat: %.6f, Lon: %.6f", contrato.latitudCaja, contrato.longitudCaja)
        } else {
            "No Registrado"
        }
        drawLabelValue(canvas, "Coordenadas Cliente: ", userCoords, 30f, 140f, yPos, textPaint)
        drawLabelValue(canvas, "Coordenadas Caja: ", boxCoords, 320f, 420f, yPos, textPaint)

        // 6. SECTION 4: FIRMAS Y SOPORTE MULTIMEDIA EN DISPOSITIVO
        yPos += 35f
        drawSectionHeader(canvas, "4. REGISTRO FOTOGRÁFICO Y COMPROBACIONES", yPos, paint, textPaint, pageWidth)
        
        yPos += 20f
        // Draw 3 boxes for images: Signature, Client Photo, Box Photo
        val boxWidth = 150
        val boxHeight = 110
        val spacing = 25
        
        val frames = listOf(
            Rect(30, yPos.toInt(), 30 + boxWidth, (yPos + boxHeight).toInt()),
            Rect(30 + boxWidth + spacing, yPos.toInt(), 30 + boxWidth * 2 + spacing, (yPos + boxHeight).toInt()),
            Rect(30 + (boxWidth + spacing) * 2, yPos.toInt(), 30 + boxWidth * 3 + spacing * 2, (yPos + boxHeight).toInt())
        )
        
        val bitmapFirma = loadBase64ToBitmap(contrato.firmaBase64, boxWidth, boxHeight)
            ?: loadUriToBitmap(context, contrato.firmaUri, boxWidth, boxHeight)
        val bitmapFotoCli = loadBase64ToBitmap(contrato.fotoClientBase64, boxWidth, boxHeight)
            ?: loadUriToBitmap(context, contrato.fotoClientUri, boxWidth, boxHeight)
        val bitmapFotoCaj = loadBase64ToBitmap(contrato.fotoCajaBase64, boxWidth, boxHeight)
            ?: loadUriToBitmap(context, contrato.fotoCajaUri, boxWidth, boxHeight)
        
        val titles = listOf("FIRMA ABONADO", "FACHADA CLIENTE", "PUERTO EN CAJA")
        val bitmaps = listOf(bitmapFirma, bitmapFotoCli, bitmapFotoCaj)
        val stateText = listOf(
            if (contrato.firmaBase64 != null || contrato.firmaUri != null) "REGISTRADA" else "PENDIENTE",
            if (contrato.fotoClientBase64 != null || contrato.fotoClientUri != null) "FOTO CARGADA" else "SIN IMAGEN",
            if (contrato.fotoCajaBase64 != null || contrato.fotoCajaUri != null) "FOTO CARGADA" else "SIN IMAGEN"
        )
        
        frames.forEachIndexed { idx, rect ->
            // Draw subtle gray background for placeholder
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F9FAFC")
            canvas.drawRect(rect, paint)
            
            // Draw thin border
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#D1D5DB")
            paint.strokeWidth = 1f
            canvas.drawRect(rect, paint)
            
            // Render bitmap centered inside the rect frame if available
            val bmp = bitmaps[idx]
            if (bmp != null) {
                val left = rect.left + (boxWidth - bmp.width) / 2f
                val top = rect.top + (boxHeight - bmp.height) / 2f
                canvas.drawBitmap(bmp, left, top, null)
            } else {
                // If null, draw helpful placeholder texts
                textPaint.isFakeBoldText = true
                textPaint.textSize = 8f
                textPaint.color = Color.GRAY
                
                val textWidth = textPaint.measureText(stateText[idx])
                val tx = rect.left + (boxWidth - textWidth) / 2f
                val ty = rect.top + (boxHeight / 2f) + 4f
                canvas.drawText(stateText[idx], tx, ty, textPaint)
            }
            
            // Draw visual label text below or inside each box
            textPaint.isFakeBoldText = true
            textPaint.color = Color.parseColor("#0C2340")
            textPaint.textSize = 7.5f
            val titleWidth = textPaint.measureText(titles[idx])
            canvas.drawText(titles[idx], rect.left + (boxWidth - titleWidth) / 2f, rect.top - 5f, textPaint)
        }
        
        // 7. Footer details
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#E0E0E0")
        canvas.drawLine(20f, pageHeight - 45f, pageWidth - 20f, pageHeight - 45f, paint)
        
        textPaint.textSize = 7.5f
        textPaint.color = Color.GRAY
        canvas.drawText("Elaborado por Tecnicable Margarita C.A. • Generado directamente desde módulo de operaciones de campo.", 25f, pageHeight - 30f, textPaint)
        canvas.drawText("Documento Digital de Conformidad", pageWidth - 165f, pageHeight - 30f, textPaint)
        
        pdfDocument.finishPage(page)
        
        // 8. Write PDF to device storage space
        val cleanName = contrato.nombreCliente.replace(" ", "_").replace("/", "_").lowercase()
        val fileName = "Contrato_${contrato.nroInstalacion}_$cleanName.pdf"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        val file = File(directory, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            Toast.makeText(context, "Contrato guardado localmente: ${file.name}", Toast.LENGTH_LONG).show()
            
            // Mirror to public Downloads directory
            saveToPublicDownloads(context, file, fileName)
            
            // Proactively prompt user to view or transmit the file
            openOrSharePdf(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error al generar PDF de contrato: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun saveToPublicDownloads(context: Context, file: File, fileName: String) {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            try {
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        java.io.FileInputStream(file).use { fileInputStream ->
                            fileInputStream.copyTo(outputStream)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!publicDir.exists()) {
                    publicDir.mkdirs()
                }
                val destFile = File(publicDir, fileName)
                java.io.FileInputStream(file).use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun openOrSharePdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = android.content.Intent.createChooser(intent, "Abrir PDF con...").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            sharePdf(context, file)
        }
    }

    private fun sharePdf(context: Context, file: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = android.content.Intent.createChooser(intent, "Compartir Reporte PDF...").apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No se pudo compartir el archivo", Toast.LENGTH_SHORT).show()
        }
    }
}
