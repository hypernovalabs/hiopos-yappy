// src/main/java/com/tefbanesco/AlternativePayment.kt
package com.tefbanesco

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class AlternativePayment(
    private val apiKey: String = "937bfbe7-a29e-4fcb-b155-affe133bd3a6",
    private val secretKey: String = "WVBfQkVCOTZDNzQtMDgxOC0zODVBLTg0ODktNUQxQTNBODVCRjFF",
    private val baseUrl: String = "https://api-integrationcheckout-uat.yappycloud.com/v1", // URL oficial para UAT (pruebas)
    private val defaultDevice: DeviceInfo = DeviceInfo(id = "CAJA-02", name = "C", user = "a"),
    private val defaultGroupId: String = "ID-TESTING-HYPER",
    private val useMock: Boolean = false // Desactivamos modo mock para uso real
) {
    companion object {
        private const val TAG = "AlternativePayment"
        private const val TIMEOUT = 30000 // 30 segundos

        // Estados de transacción Yappy
        const val STATUS_PENDING = "PENDING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_TIMEOUT = "TIMEOUT"
        const val STATUS_ERROR = "ERROR"
        const val STATUS_CANCELED = "CANCELED"

        // Configuración del modo mock
        private const val MOCK_DELAY_SESSION = 500L // Tiempo simulado para abrir sesión
        private const val MOCK_DELAY_QR = 800L // Tiempo simulado para generar QR
        private const val MOCK_DELAY_TRANSACTION = 25000L // Tiempo de espera para simular transacción (25 segundos)
        private const val MOCK_SUCCESS_RATE = 95 // Porcentaje de transacciones exitosas en modo mock (0-100)
    }

    private var sessionToken: String? = null

    /**
     * Procesa un pago alternativo desde la integración con HioPOS y espera a que termine
     */
    fun processPayment(
        transactionType: String?,
        amount: String?,
        currencyIso: String?,
        transactionIdHio: Int?,
        shopData: String?,
        sellerData: String?,
        documentData: String?,
        taxDetail: String?,
        languageIso: String?
    ): Boolean = runBlocking {
        // Guardar los datos para la descripción
        val safeTransactionType = transactionType ?: "VENTA"
        val safeTransactionId = transactionIdHio ?: 0

        // Procesar el pago con los datos adicionales para la descripción
        procesarPagoCompleto(amount ?: "0", safeTransactionType, safeTransactionId)
    }

    /**
     * Callbacks para eventos del flujo de pago
     */
    interface PaymentCallbacks {
        fun onQrGenerated(hash: String, amount: String)
        fun onTransactionStatusChanged(status: String)
        fun onPaymentComplete(success: Boolean)
    }

    private var callbacks: PaymentCallbacks? = null

    /**
     * Establece los callbacks para eventos del flujo de pago
     */
    fun setPaymentCallbacks(callbacks: PaymentCallbacks) {
        this.callbacks = callbacks
    }

    /**
     * Método que realiza todo el flujo: abrir sesión, generar QR, monitorear y cerrar sesión
     */
    suspend fun procesarPagoCompleto(
        amount: String,
        transactionType: String = "VENTA",
        transactionIdHio: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "=== Iniciando flujo de Yappy ===")
            val token = abrirSesion() ?: return@withContext false

            val qrData = generarQr(amount, token, transactionType, transactionIdHio)
            val txId = qrData["transactionId"]
            val hash = qrData["hash"]

            if (txId.isNullOrEmpty() || hash.isNullOrEmpty()) {
                cerrarSesion(token)
                return@withContext false
            }

            Log.d(TAG, "QR generado, transactionId=$txId, hash=$hash")

            // Notificar que el QR está listo para mostrarse
            withContext(Dispatchers.Main) {
                callbacks?.onQrGenerated(hash, amount)
            }

            val status = monitorearTransaccion(txId, token)
            Log.d(TAG, "Estado final de la transacción: $status")

            // Notificar el resultado final
            val success = status == "COMPLETED"
            withContext(Dispatchers.Main) {
                callbacks?.onPaymentComplete(success)
            }

            cerrarSesion(token)
            return@withContext success
        } catch (e: Exception) {
            Log.e(TAG, "Error en flujo Yappy", e)
            withContext(Dispatchers.Main) {
                callbacks?.onPaymentComplete(false)
            }
            return@withContext false
        }
    }

    private suspend fun abrirSesion(): String? = withContext(Dispatchers.IO) {
        try {
            // Para depuración, usar un modo Mock
            if (useMock) {
                Log.d(TAG, "Usando modo Mock para abrirSesion")

                // Simular un tiempo de respuesta realista
                delay(MOCK_DELAY_SESSION)

                // Generar un token único con timestamp
                val mockToken = "mock-token-${System.currentTimeMillis()}"
                sessionToken = mockToken

                Log.d(TAG, "Mock: Sesión abierta con token=$mockToken")
                return@withContext mockToken
            }

            // A partir de aquí es el modo real con API
            val urlString = "$baseUrl/session/device"
            Log.d(TAG, "Conectando a URL: $urlString")

            try {
                val url = URL(urlString)

                // Log de la URL completa para depuración
                Log.d(TAG, "URL completa: ${url.protocol}://${url.host}:${url.port ?: ""}${url.path}")

                // 1. Preparar el JSON interno (request body)
                val deviceJson = JSONObject().apply {
                    put("id", defaultDevice.id)
                    put("name", defaultDevice.name ?: JSONObject.NULL)
                    put("user", defaultDevice.user ?: JSONObject.NULL)
                }
                val inner = JSONObject().apply {
                    put("device", deviceJson)
                    put("group_id", defaultGroupId)
                }

                // 2. Envoltorio exterior que requiere la API UAT
                val wrapped = JSONObject().apply {
                    put("body", inner)
                }

                // Log del JSON para depuración
                Log.d(TAG, "JSON interno: $inner")
                Log.d(TAG, "JSON envuelto para solicitud: $wrapped")

                // Generar y mostrar el comando curl equivalente (para abrir sesión)
                val headers = mapOf(
                    "Content-Type" to "application/json",
                    "api-key" to apiKey,
                    "secret-key" to secretKey
                )
                val curlCommand = CurlGenerator.generateCurlCommand(
                    method = "POST",
                    url = urlString,
                    headers = headers,
                    body = wrapped.toString()
                )

                // Imprimir versión completa y sanitizada para depuración
                Log.d(TAG, "Comando curl para abrir sesión:\n$curlCommand")
                Log.i(TAG, "Comando curl sanitizado para abrir sesión:\n${CurlGenerator.sanitizeForDisplay(curlCommand)}")

                // Configurar la conexión
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("api-key", apiKey)
                    setRequestProperty("secret-key", secretKey)
                    connectTimeout = TIMEOUT
                    readTimeout = TIMEOUT

                    // Log de cabeceras para depuración (ocultando parte de claves sensibles)
                    Log.d(TAG, "Cabeceras: Content-Type=${getRequestProperty("Content-Type")}, " +
                            "api-key=${getRequestProperty("api-key")?.take(5)}..., " +
                            "secret-key=${getRequestProperty("secret-key")?.take(5)}...")
                }

                try {
                    // Escribir el cuerpo de la solicitud (JSON envuelto)
                    Log.d(TAG, "Escribiendo cuerpo de la solicitud...")
                    OutputStreamWriter(connection.outputStream).use {
                        it.write(wrapped.toString())
                        it.flush()
                    }

                    // Leer la respuesta
                    val responseCode = connection.responseCode
                    Log.d(TAG, "Respuesta obtenida, código: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val resp = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }

                        // Formateamos la respuesta JSON para mejor visualización
                        try {
                            val jsonResponse = JSONObject(resp)

                            // Mostrar la respuesta completa con formato para mejor análisis
                            Log.d(TAG, "=========== RESPUESTA EXITOSA DE LA API (ABRIR SESIÓN) ===========")
                            Log.d(TAG, "Respuesta formateada:\n${jsonResponse.toString(2)}")

                            // Mostrar detalles del status
                            val status = jsonResponse.optJSONObject("status")
                            if (status != null) {
                                Log.d(TAG, "Status code: ${status.optString("code")}")
                                Log.d(TAG, "Status description: ${status.optString("description")}")
                            }

                            // Mostrar detalles del body
                            val body = jsonResponse.getJSONObject("body")
                            Log.d(TAG, "Status de sesión: ${body.optString("status", "No disponible")}")
                            Log.d(TAG, "Fecha apertura: ${body.optString("opened_date", "No disponible")}")
                            Log.d(TAG, "Token: ${body.getString("token").take(20)}...")
                            Log.d(TAG, "=========== FIN DE RESPUESTA ===========")

                            // Continuar con el procesamiento normal
                            val token = body.getString("token")
                            sessionToken = token
                            Log.d(TAG, "Sesión abierta con token=$token")
                            return@withContext token
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar JSON de respuesta", e)
                            return@withContext null
                        }
                    } else {
                        // En caso de error, leer el mensaje de error
                        val errorStream = connection.errorStream
                        val errorMessage = if (errorStream != null) {
                            BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                        } else {
                            "Sin mensaje de error"
                        }
                        // Log del error completo
                        Log.e(TAG, "Error al abrir sesión. Código: $responseCode, Mensaje: $errorMessage")

                        // Extraer código y descripción del error de Yappy si es posible
                        val yappyErrorMessage = try {
                            val jsonError = JSONObject(errorMessage)
                            val status = jsonError.optJSONObject("status")
                            if (status != null) {
                                val code = status.optString("code", "")
                                val apiDescription = status.optString("description", "")

                                // Obtener descripción amigable del error
                                val friendlyDescription = YappyErrorCodes.getErrorDescription(code)

                                // Mensaje completo con código, descripción amigable y descripción técnica
                                "[$code] $friendlyDescription\n\nRespuesta API: $apiDescription"
                            } else {
                                "Error HTTP $responseCode: No se pudo procesar la respuesta del servidor."
                            }
                        } catch (e: Exception) {
                            "Error de comunicación: ${e.message}\n\nRespuesta: $errorMessage"
                        }

                        // Notificar el error a través de callbacks
                        withContext(Dispatchers.Main) {
                            callbacks?.onTransactionStatusChanged(STATUS_ERROR)
                            // Usando un trick para pasar el mensaje de error a través del sistema de callbacks
                            callbacks?.onQrGenerated("ERROR:$yappyErrorMessage", "0")
                        }

                        return@withContext null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al comunicarse con el servidor", e)
                    return@withContext null
                } finally {
                    connection.disconnect()
                    Log.d(TAG, "Conexión cerrada")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear URL o configurar conexión", e)
                return@withContext null
            }
        } catch (e: Exception) {
            // Detectar tipo específico de error para mensajes más útiles
            val errorMsg = when {
                e.toString().contains("UnknownHostException") ->
                    "Error de conexión: No se pudo conectar al servidor. Verifica tu conexión a internet."
                e.toString().contains("SocketTimeoutException") ->
                    "Tiempo de espera agotado al conectar con Yappy. El servidor está tardando en responder."
                else -> "Error general al abrir sesión: ${e.message}"
            }
            Log.e(TAG, errorMsg, e)
            return@withContext null
        }
    }

    private suspend fun generarQr(
        amountStr: String,
        token: String,
        transactionType: String = "VENTA",
        transactionIdHio: Int = 0
    ): Map<String, String> = withContext(Dispatchers.IO) {
        try {
            // Para depuración, usar un modo Mock
            if (useMock) {
                Log.d(TAG, "Usando modo Mock para generarQr")

                // Simular un tiempo de respuesta realista
                delay(MOCK_DELAY_QR)

                // Formatear el monto para los logs
                val formattedAmount = try {
                    val cents = amountStr.toInt()
                    String.format("%.2f", cents / 100.0)
                } catch (e: Exception) {
                    "0.00"
                }

                // Construir la descripción formateada para el modo mock
                val cleanedTransactionType = transactionType
                    .let { type -> java.text.Normalizer.normalize(type, java.text.Normalizer.Form.NFD) }
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                    .replace("[^a-zA-Z0-9 ]".toRegex(), "")
                    .replace("\\s+".toRegex(), " ") // Colapsar múltiples espacios en uno solo
                    .uppercase()
                    .trim()

                val yappyDescription = "YAPPY ${cleanedTransactionType} ${transactionIdHio}"
                Log.d(TAG, "Mock: Descripción para Yappy: $yappyDescription")

                // Generar datos mock consistentes
                val mockTxId = "mock-tx-${System.currentTimeMillis()}"
                val mockHash = "mock-hash-${System.currentTimeMillis()}"

                Log.d(TAG, "Mock: QR generado para monto $formattedAmount PAB - txId: $mockTxId, hash: $mockHash, descripción: $yappyDescription")

                return@withContext mapOf(
                    "transactionId" to mockTxId,
                    "hash" to mockHash
                )
            }

            // A partir de aquí es el modo real con API
            // Usamos DYN como tipo de QR (Dinámico) según la documentación
            val qrType = "DYN" // Tipo de QR dinámico
            val urlString = "$baseUrl/qr/generate/$qrType"
            Log.d(TAG, "Conectando a URL para generar QR: $urlString")

            try {
                val url = URL(urlString)

                // Validar que el monto sea válido
                if (amountStr.isBlank() || amountStr == "0") {
                    Log.e(TAG, "ERROR: Monto no válido. Monto recibido: '$amountStr'")
                    throw IllegalArgumentException("El monto no puede estar vacío o ser cero")
                }

                // Preparar el JSON de la solicitud
                val amountValue = try {
                    val value = BigDecimal(amountStr)
                    if (value <= BigDecimal.ZERO) {
                        Log.e(TAG, "ERROR: Monto debe ser positivo. Monto recibido: $amountStr")
                        throw IllegalArgumentException("El monto debe ser positivo")
                    }
                    value
                } catch (e: Exception) {
                    Log.e(TAG, "Error al convertir monto: '$amountStr'", e)
                    throw IllegalArgumentException("Formato de monto no válido: $amountStr", e)
                }

                // 1. Preparar JSON interno (body)
                // Convertir el monto de centavos a formato decimal con punto
                // Ej: 10050 centavos -> 100.50 dólares
                val amountInCents = amountValue.toInt()
                // Usamos BigDecimal para garantizar precisión en cálculos monetarios
                val dollarsWithDecimals = BigDecimal(amountInCents).divide(BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP)

                Log.d(TAG, "Convirtiendo monto de $amountValue centavos a $dollarsWithDecimals dólares con decimales")

                // Preparar JSON según documentación exacta de Yappy con monto en formato decimal
                val chargeAmount = JSONObject().apply {
                    put("sub_total", dollarsWithDecimals) // Campo requerido según documentación
                    put("total", dollarsWithDecimals) // Valor total requerido
                    put("tax", 0.0) // Impuesto
                    put("tip", 0.0) // Propina
                    put("discount", 0.0) // Descuento
                    // Eliminamos currency que no aparece en la documentación
                }
                // 1. Limpiar el transactionType (quitar tildes y caracteres especiales)
                val cleanedTransactionType = transactionType
                    .let { type ->
                        // Normalizar para separar caracteres base de sus diacríticos
                        java.text.Normalizer.normalize(type, java.text.Normalizer.Form.NFD)
                    }
                    .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "") // Remover diacríticos
                    .replace("[^a-zA-Z0-9 ]".toRegex(), "") // Remover cualquier cosa que no sea letra, número o espacio
                    .replace("\\s+".toRegex(), " ") // Colapsar múltiples espacios en uno solo
                    .uppercase() // Convertir a mayúsculas para consistencia
                    .trim() // Quitar espacios al inicio/final

                // 2. Construir la descripción en formato: "YAPPY [TIPO_TRANSACCION] [ID_TRANSACCION]"
                val yappyDescription = "YAPPY ${cleanedTransactionType} ${transactionIdHio}"

                // Log para verificar la descripción formateada
                Log.d(TAG, "Descripción para Yappy (texto plano): $yappyDescription")

                val inner = JSONObject().apply {
                    put("charge_amount", chargeAmount)
                    put("order_id", transactionIdHio.toString()) // Usar transactionIdHio como order_id para tracking
                    put("description", yappyDescription) // Descripción actualizada y limpia
                }

                // 2. Envoltorio exterior que requiere la API UAT
                val wrapped = JSONObject().apply {
                    put("body", inner)
                }

                // Log del JSON para depuración
                Log.d(TAG, "JSON interno QR (según documentación): $inner")
                Log.d(TAG, "JSON envuelto para QR: $wrapped")
                Log.d(TAG, "Formato charge_amount: ${chargeAmount.toString(2)}") // Formato indentado para mejor lectura

                // Configurar la conexión
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("api-key", apiKey)
                    setRequestProperty("secret-key", secretKey)
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = TIMEOUT
                    readTimeout = TIMEOUT

                    // Log para depuración
                    Log.d(TAG, "Cabeceras QR: api-key=${getRequestProperty("api-key")?.take(5)}..., " +
                            "Authorization=Bearer ${token.take(5)}...")
                }

                // Generar y mostrar el comando curl equivalente
                val headers = mapOf(
                    "Content-Type" to "application/json",
                    "api-key" to apiKey,
                    "secret-key" to secretKey,
                    "Authorization" to "Bearer $token"
                )
                val curlCommand = CurlGenerator.generateCurlCommand(
                    method = "POST",
                    url = urlString,
                    headers = headers,
                    body = wrapped.toString()
                )

                // Imprimir versión completa en el log (para desarrolladores)
                Log.d(TAG, "Comando curl para generación de QR:\n$curlCommand")

                // Imprimir versión sanitizada en el log (para producción y capturas de pantalla)
                Log.i(TAG, "Comando curl sanitizado para generación de QR:\n${CurlGenerator.sanitizeForDisplay(curlCommand)}")

                try {
                    // Escribir el cuerpo de la solicitud (JSON envuelto)
                    Log.d(TAG, "Escribiendo cuerpo de la solicitud de QR...")
                    OutputStreamWriter(connection.outputStream).use {
                        it.write(wrapped.toString())
                        it.flush()
                    }

                    // Leer la respuesta
                    val responseCode = connection.responseCode
                    Log.d(TAG, "Respuesta de QR obtenida, código: $responseCode")

                    // Manejar específicamente el error 503 (servicio no disponible)
                    if (responseCode == 503) {
                        Log.w(TAG, "Servicio temporalmente no disponible (503). Reintentando en 2 segundos...")

                        // Reintento después de una espera
                        delay(2000)

                        // Crear una nueva conexión y reintentar
                        connection.disconnect()
                        Log.d(TAG, "Reintentando generar QR...")

                        val retryConn = (url.openConnection() as HttpURLConnection).apply {
                            doOutput = true
                            requestMethod = "POST"
                            setRequestProperty("Content-Type", "application/json")
                            setRequestProperty("api-key", apiKey)
                            setRequestProperty("secret-key", secretKey)
                            setRequestProperty("Authorization", "Bearer $token")
                            connectTimeout = TIMEOUT
                            readTimeout = TIMEOUT
                        }

                        // Escribir el cuerpo nuevamente
                        OutputStreamWriter(retryConn.outputStream).use {
                            it.write(wrapped.toString())
                            it.flush()
                        }

                        // Verificar respuesta del reintento
                        val retryCode = retryConn.responseCode
                        Log.d(TAG, "Respuesta de reintento QR, código: $retryCode")

                        if (retryCode == HttpURLConnection.HTTP_OK) {
                            val resp = BufferedReader(InputStreamReader(retryConn.inputStream)).use { it.readText() }
                            Log.d(TAG, "Respuesta QR (reintento): $resp")

                            try {
                                val jsonResponse = JSONObject(resp)
                                val body = jsonResponse.getJSONObject("body")
                                val result = mapOf(
                                    "transactionId" to body.getString("transactionId"),
                                    "hash" to body.getString("hash")
                                )
                                Log.d(TAG, "QR generado exitosamente en reintento")
                                retryConn.disconnect()
                                return@withContext result
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar JSON de respuesta QR en reintento", e)
                                retryConn.disconnect()
                                return@withContext emptyMap<String, String>()
                            }
                        } else {
                            // Error en el reintento
                            Log.e(TAG, "Error en reintento. Código: $retryCode")
                            retryConn.disconnect()
                        }
                    }

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val resp = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                        // Formateamos la respuesta JSON para mejor visualización
                        try {
                            val jsonResponse = JSONObject(resp)

                            // Mostrar la respuesta completa con formato para mejor análisis
                            Log.d(TAG, "=========== RESPUESTA EXITOSA DE LA API (GENERACIÓN QR) ===========")
                            Log.d(TAG, "Respuesta QR formateada:\n${jsonResponse.toString(2)}")

                            // Mostrar detalles del status
                            val status = jsonResponse.optJSONObject("status")
                            if (status != null) {
                                Log.d(TAG, "Status code: ${status.optString("code")}")
                                Log.d(TAG, "Status description: ${status.optString("description")}")
                            }

                            // Mostrar detalles del body
                            val body = jsonResponse.getJSONObject("body")
                            Log.d(TAG, "Fecha: ${body.optString("date", "No disponible")}")
                            Log.d(TAG, "TransactionId: ${body.getString("transactionId")}")
                            Log.d(TAG, "Hash: ${body.getString("hash")}")
                            Log.d(TAG, "=========== FIN DE RESPUESTA ===========")

                            // Continuar con el procesamiento normal
                            val result = mapOf(
                                "transactionId" to body.getString("transactionId"),
                                "hash" to body.getString("hash")
                            )
                            Log.d(TAG, "QR generado exitosamente: transactionId=${result["transactionId"]}, hash=${result["hash"]}")
                            return@withContext result
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al procesar JSON de respuesta QR", e)
                            return@withContext emptyMap<String, String>()
                        }
                    } else {
                        // En caso de error, leer el mensaje de error
                        val errorStream = connection.errorStream
                        val errorMessage = if (errorStream != null) {
                            BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                        } else {
                            "Sin mensaje de error"
                        }
                        Log.e(TAG, "Error al generar QR. Código: $responseCode, Mensaje: $errorMessage")

                        // Para cualquier error, intentamos mostrar la respuesta formateada
                        try {
                            Log.e(TAG, "=========== ERROR EN GENERACIÓN DE QR (CÓDIGO $responseCode) ===========")
                            // Intentar formatear el mensaje de error como JSON
                            try {
                                val jsonError = JSONObject(errorMessage)
                                Log.e(TAG, "Respuesta de error formateada:\n${jsonError.toString(2)}")

                                val status = jsonError.optJSONObject("status")
                                if (status != null) {
                                    val code = status.optString("code", "")
                                    val description = status.optString("description", "")
                                    Log.e(TAG, "Código de error: $code")
                                    Log.e(TAG, "Descripción: $description")
                                }
                            } catch (e: Exception) {
                                // Si no es JSON, mostrar el mensaje de error tal cual
                                Log.e(TAG, "Mensaje de error (no es JSON): $errorMessage")
                            }

                            // Mensaje detallado del JSON enviado para debuggear
                            Log.e(TAG, "JSON de solicitud enviado:")
                            Log.e(TAG, "charge_amount:\n${chargeAmount.toString(2)}")
                            Log.e(TAG, "Payload completo:\n${wrapped.toString(2)}")
                            Log.e(TAG, "=========== FIN DE ERROR ===========")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error al analizar respuesta de error", e)
                        }

                        return@withContext emptyMap<String, String>()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en la comunicación al generar QR", e)
                    return@withContext emptyMap<String, String>()
                } finally {
                    connection.disconnect()
                    Log.d(TAG, "Conexión QR cerrada")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear URL o configurar conexión para QR", e)
                return@withContext emptyMap<String, String>()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción general en generarQr", e)
            return@withContext emptyMap<String, String>()
        }
    }

    private suspend fun monitorearTransaccion(txId: String, token: String): String = withContext(Dispatchers.IO) {
        try {
            // Para depuración, usar un modo Mock
            if (useMock) {
                Log.d(TAG, "Usando modo Mock para monitorearTransaccion")

                // Inicialmente el estado es pendiente
                var mockStatus = STATUS_PENDING

                // Notificar estado inicial
                withContext(Dispatchers.Main) {
                    callbacks?.onTransactionStatusChanged(mockStatus)
                }

                Log.d(TAG, "Mock: Estado inicial de transacción $txId: $mockStatus")

                // Simular tiempo de procesamiento variable
                delay(MOCK_DELAY_TRANSACTION)

                // Determinar resultado final (con probabilidad de éxito configurable)
                val random = (0..100).random()
                mockStatus = if (random < MOCK_SUCCESS_RATE) {
                    STATUS_COMPLETED
                } else {
                    STATUS_FAILED
                }

                Log.d(TAG, "Mock: Estado final de transacción $txId: $mockStatus (random=$random, threshold=$MOCK_SUCCESS_RATE)")

                // Notificar el estado final
                withContext(Dispatchers.Main) {
                    callbacks?.onTransactionStatusChanged(mockStatus)
                }

                return@withContext mockStatus
            }

            // A partir de aquí es el modo real con API
            var lastStatus = STATUS_PENDING
            var attemptCount = 0

            // Notificar estado inicial
            withContext(Dispatchers.Main) {
                callbacks?.onTransactionStatusChanged(lastStatus)
            }

            // Loop de monitoreo (hasta 60 intentos = 2 minutos con 2 segundos entre intentos)
            repeat(60) {
                attemptCount++
                delay(2_000) // Esperar 2 segundos entre consultas

                val urlString = "$baseUrl/transaction/$txId"
                Log.d(TAG, "Consultando estado de transacción en URL: $urlString (intento $attemptCount)")

                try {
                    val url = URL(urlString)
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        setRequestProperty("api-key", apiKey)
                        setRequestProperty("secret-key", secretKey)
                        setRequestProperty("Authorization", "Bearer $token")
                        connectTimeout = TIMEOUT
                        readTimeout = TIMEOUT

                        // Log para depuración (ocultando parte de claves sensibles)
                        Log.d(TAG, "Cabeceras de monitoreo: " +
                                "api-key=${getRequestProperty("api-key")?.take(5)}..., " +
                                "Authorization=Bearer ${token.take(5)}...")
                    }

                    try {
                        val responseCode = conn.responseCode
                        Log.d(TAG, "Código de respuesta monitoreo (intento $attemptCount): $responseCode")

                        val status = if (responseCode == HttpURLConnection.HTTP_OK) {
                            try {
                                val resp = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                                Log.d(TAG, "Respuesta monitoreo: $resp")

                                val jsonResponse = JSONObject(resp)
                                jsonResponse.getJSONObject("body").getString("status")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error al procesar JSON de monitoreo", e)
                                STATUS_ERROR
                            }
                        } else {
                            // En caso de error, leer el mensaje de error
                            val errorStream = conn.errorStream
                            val errorMessage = if (errorStream != null) {
                                BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                            } else {
                                "Sin mensaje de error"
                            }
                            Log.e(TAG, "Error en monitoreo. Código: $responseCode, Mensaje: $errorMessage")
                            STATUS_ERROR
                        }

                        // Si cambió el status, notificar
                        if (status != lastStatus) {
                            lastStatus = status
                            withContext(Dispatchers.Main) {
                                callbacks?.onTransactionStatusChanged(status)
                            }
                            Log.d(TAG, "Estado de transacción actualizado a: $status")
                        }

                        // Si ya no está pendiente, terminar
                        if (status != STATUS_PENDING) {
                            return@withContext status
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error en consulta de estado (intento $attemptCount)", e)
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error al crear URL o conexión para monitoreo", e)
                }
            }

            // Si llegamos aquí, es timeout
            Log.w(TAG, "Timeout en monitoreo de transacción después de $attemptCount intentos")
            val timeoutStatus = STATUS_TIMEOUT
            withContext(Dispatchers.Main) {
                callbacks?.onTransactionStatusChanged(timeoutStatus)
            }
            return@withContext timeoutStatus
        } catch (e: Exception) {
            // Detectar tipo específico de error para informar al usuario
            val errorStatus = STATUS_ERROR
            val errorMsg = when {
                e.toString().contains("UnknownHostException") ->
                    "Error de conexión: No se pudo conectar al servidor para verificar el estado del pago."
                e.toString().contains("SocketTimeoutException") ->
                    "Tiempo de espera agotado al consultar estado. El servidor está tardando en responder."
                else -> "Error verificando el estado del pago: ${e.message}"
            }
            Log.e(TAG, errorMsg, e)

            withContext(Dispatchers.Main) {
                callbacks?.onTransactionStatusChanged(errorStatus)
            }
            return@withContext errorStatus
        }
    }

    private suspend fun cerrarSesion(token: String) = withContext(Dispatchers.IO) {
        try {
            // Para depuración, usar un modo Mock
            if (useMock) {
                Log.d(TAG, "Usando modo Mock para cerrarSesion")

                // Simular tiempo de respuesta
                delay(500)

                // Limpiar token en memoria
                sessionToken = null
                Log.d(TAG, "Mock: Sesión cerrada exitosamente")
                return@withContext
            }

            // A partir de aquí es el modo real con API
            val urlString = "$baseUrl/session/device"
            Log.d(TAG, "Cerrando sesión en URL: $urlString")

            try {
                val url = URL(urlString)

                // Configurar la conexión
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Content-Type", "application/json") // Agregar tipo de contenido
                    setRequestProperty("api-key", apiKey)
                    setRequestProperty("secret-key", secretKey)
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = TIMEOUT
                    readTimeout = TIMEOUT
                    doOutput = true // Importante para que acepte cuerpo en DELETE

                    // Log para depuración (ocultando parte de claves sensibles)
                    Log.d(TAG, "Cabeceras de cierre: " +
                            "api-key=${getRequestProperty("api-key")?.take(5)}..., " +
                            "Authorization=Bearer ${token.take(5)}...")
                }

                // Crear un body vacío o con minimal payload para DELETE
                val deleteBody = JSONObject().apply {
                    put("token", token)
                }
                val wrappedDelete = JSONObject().apply {
                    put("body", deleteBody)
                }

                Log.d(TAG, "Body para DELETE: $wrappedDelete")

                try {
                    // Escribir cuerpo para DELETE
                    OutputStreamWriter(conn.outputStream).use {
                        it.write(wrappedDelete.toString())
                        it.flush()
                    }

                    val responseCode = conn.responseCode
                    Log.d(TAG, "Respuesta de cierre obtenida, código: $responseCode")

                    if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                        Log.d(TAG, "Sesión cerrada exitosamente")
                    } else {
                        // En caso de error, leer el mensaje de error
                        val errorStream = conn.errorStream
                        val errorMessage = if (errorStream != null) {
                            BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                        } else {
                            "Sin mensaje de error"
                        }
                        Log.w(TAG, "Error al cerrar sesión. Código: $responseCode, Mensaje: $errorMessage")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error en la comunicación al cerrar sesión", e)
                } finally {
                    conn.disconnect()
                    Log.d(TAG, "Conexión de cierre finalizada")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error al crear URL o conexión para cierre", e)
            } finally {
                // Siempre limpiar el token de sesión, incluso si hay error
                sessionToken = null
                Log.d(TAG, "Token de sesión limpiado de la memoria")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Excepción general en cerrarSesion", e)
            sessionToken = null
        }
    }
}

data class DeviceInfo(val id: String, val name: String?, val user: String?)
