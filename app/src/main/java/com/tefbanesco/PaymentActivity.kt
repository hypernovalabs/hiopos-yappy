// src/main/java/com/tefbanesco/PaymentActivity.kt
package com.tefbanesco

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope

// Constantes para las acciones
const val ACTION_PREFIX = "icg.actions.electronicpayment.tefbanesco."
const val ACTION_INITIALIZE = ACTION_PREFIX + "INITIALIZE"
const val ACTION_TRANSACTION = ACTION_PREFIX + "TRANSACTION"
const val ACTION_GET_BEHAVIOR = ACTION_PREFIX + "GET_BEHAVIOR"

// Constantes para los extras de los Intents
object ExtraKeys {
    const val PARAMETERS = "Parameters"
    const val TOKEN = "Token"
    const val ERROR_MESSAGE = "ErrorMessage"
    const val SUPPORTS_TRANSACTION_VOID = "SupportsTransactionVoid"
    const val SUPPORTS_TRANSACTION_QUERY = "SupportsTransactionQuery"
    const val SUPPORTS_NEGATIVE_SALES = "SupportsNegativeSales"
    const val SUPPORTS_PARTIAL_REFUND = "SupportsPartialRefund"
    const val SUPPORTS_BATCH_CLOSE = "SupportsBatchClose"
    const val SUPPORTS_TIP_ADJUSTMENT = "SupportsTipAdjustment"
    const val ONLY_CREDIT_FOR_TIP_ADJUSTMENT = "OnlyCreditForTipAdjustment"
    const val SUPPORTS_CREDIT = "SupportsCredit"
    const val SUPPORTS_DEBIT = "SupportsDebit"
    const val SUPPORTS_EBT_FOODSTAMP = "SupportsEBTFoodstamp"
    const val HAS_CUSTOM_PARAMS = "HasCustomParams"
    const val CAN_CHARGE_CARD = "CanChargeCard"
    const val CAN_AUDIT = "canAudit"
    const val EXECUTE_VOID_WHEN_AVAILABLE = "ExecuteVoidWhenAvailable"
    const val SAVE_LOYALTY_CARD_NUM = "SaveLoyaltyCardNum"
    const val CAN_PRINT = "CanPrint"
    const val READ_CARD_FROM_API = "ReadCardFromApi"
    const val ONLY_USE_DOCUMENT_PATH = "OnlyUseDocumentPath"

    const val TRANSACTION_TYPE = "TransactionType"
    const val AMOUNT = "Amount"
    const val CURRENCY_ISO = "CurrencyISO"
    const val LANGUAGE_ISO = "LanguageISO"
    const val TRANSACTION_ID_HIO = "TransactionId"
    const val SHOP_DATA = "ShopData"
    const val SELLER_DATA = "SellerData"
    const val TAX_DETAIL = "TaxDetail"
    const val RECEIPT_PRINTER_COLUMNS = "ReceiptPrinterColumns"
    const val DOCUMENT_DATA = "DocumentData"
    const val DOCUMENT_PATH = "DocumentPath"
    const val OVER_PAYMENT_TYPE = "OverPaymentType"

    const val TRANSACTION_RESULT = "TransactionResult"
    const val TRANSACTION_DATA_MODULE = "TransactionData"
    const val AUTHORIZATION_ID = "AuthorizationId"
    const val CARD_HOLDER = "CardHolder"
    const val CARD_TYPE = "CardType"
    const val CARD_NUM = "CardNum"
    const val ERROR_MESSAGE_TITLE = "ErrorMessageTitle"
    const val DOCUMENT_ID = "DocumentId"
}

class PaymentActivity : AppCompatActivity() {

    private val TAG = "PaymentActivity"

    // Variables para la transacción actual
    private var currentTransactionIdHio: Int? = 0
    private var currentAmount: String? = null
    private var currentCurrencyIso: String? = null
    private var currentTransactionType: String? = null

    // Diálogos
    private var successDialog: PaymentSuccessDialog? = null
    private var failureDialog: PaymentFailureDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Intent action = ${intent.action}")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: Intent action = ${intent?.action}")
        intent?.let { handleIntent(it) }
    }

    private fun handleIntent(intent: Intent) {
        logIntentExtras(intent.action, intent)
        when (intent.action) {
            ACTION_INITIALIZE   -> handleInitialize(intent)
            ACTION_GET_BEHAVIOR -> handleGetBehavior(intent)
            ACTION_TRANSACTION  -> handleTransaction(intent)
            else                -> respondUnsupported(intent)
        }
    }

    private fun logIntentExtras(action: String?, intent: Intent) {
        Log.d(TAG, "╔═════════════════════════════════════════")
        Log.d(TAG, "║ RECIBIDO: $action")
        Log.d(TAG, "╠═════════════════════════════════════════")

        // Aquí reemplazamos isNullOrEmpty() por comprobación explícita
        val extras = intent.extras
        if (extras == null || extras.isEmpty) {
            Log.d(TAG, "║ No hay extras en el intent")
            Log.d(TAG, "╚═════════════════════════════════════════")
            return
        }

        for (key in extras.keySet()) {
            val value = extras.get(key)
            Log.d(TAG, "║ $key: $value")
        }
        Log.d(TAG, "╚═════════════════════════════════════════")
    }

    private fun respondUnsupported(intent: Intent) {
        Log.e(TAG, "Acción no soportada: ${intent.action}")
        val result = Intent(intent.action).apply {
            putExtra(ExtraKeys.ERROR_MESSAGE, "Acción no soportada: ${intent.action}")
        }
        setResult(Activity.RESULT_CANCELED, result)
        finish()
    }

    private fun handleInitialize(intent: Intent) {
        val paramsXml = intent.getStringExtra(ExtraKeys.PARAMETERS)
        if (paramsXml.isNullOrBlank()) {
            val error = "Parameters (XML) no recibido"
            setResult(Activity.RESULT_CANCELED, Intent(ACTION_INITIALIZE).apply {
                putExtra(ExtraKeys.ERROR_MESSAGE, error)
            })
        } else {
            setResult(Activity.RESULT_OK, Intent(ACTION_INITIALIZE))
        }
        finish()
    }

    private fun handleGetBehavior(intent: Intent) {
        val result = Intent(ACTION_GET_BEHAVIOR).apply {
            putExtra(ExtraKeys.SUPPORTS_CREDIT, true)
            putExtra(ExtraKeys.SUPPORTS_DEBIT, false)
            putExtra(ExtraKeys.SUPPORTS_PARTIAL_REFUND, false)
            putExtra(ExtraKeys.SUPPORTS_TIP_ADJUSTMENT, false)
            putExtra(ExtraKeys.CAN_PRINT, false)
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun handleTransaction(intent: Intent) {
        currentTransactionType = intent.getStringExtra(ExtraKeys.TRANSACTION_TYPE)
        currentAmount           = intent.getStringExtra(ExtraKeys.AMOUNT)
        currentCurrencyIso      = intent.getStringExtra(ExtraKeys.CURRENCY_ISO)
        currentTransactionIdHio = intent.getIntExtra(ExtraKeys.TRANSACTION_ID_HIO, 0)

        // Validación de campos obligatorios
        if (currentTransactionType.isNullOrBlank()
            || currentAmount.isNullOrBlank()
            || currentCurrencyIso.isNullOrBlank()
            || currentTransactionIdHio == 0
        ) {
            Log.e(TAG, "Parámetros incompletos para la transacción. " +
                    "Type: $currentTransactionType, Amount: $currentAmount, " +
                    "Currency: $currentCurrencyIso, HIO ID: $currentTransactionIdHio")
            setResult(Activity.RESULT_OK, Intent(ACTION_TRANSACTION).apply {
                putExtra(ExtraKeys.TRANSACTION_RESULT, "FAILED")
                putExtra(ExtraKeys.ERROR_MESSAGE, getString(R.string.error_missing_parameters))
                putExtra(ExtraKeys.ERROR_MESSAGE_TITLE, getString(R.string.error_data_title))
            })
            finish()
            return
        }

        // Ya no se llama a setContentView ni se configuran los botones de la UI intermedia.
        // Se procede directamente a iniciar el pago alternativo.
        Log.d(TAG, "Iniciando directamente el flujo de pago alternativo para la transacción HIO ID: $currentTransactionIdHio")
        startAlternativePayment(intent)
    }

    private fun sendTransactionResponse(accepted: Boolean, intent: Intent) {
        val result = Intent(ACTION_TRANSACTION).apply {
            putExtra(ExtraKeys.TRANSACTION_TYPE, currentTransactionType)
            putExtra(ExtraKeys.AMOUNT, currentAmount)
            putExtra(ExtraKeys.TRANSACTION_RESULT, if (accepted) "ACCEPTED" else "FAILED")
            if (!accepted) {
                putExtra(ExtraKeys.ERROR_MESSAGE, getString(R.string.error_payment_canceled))
                putExtra(ExtraKeys.ERROR_MESSAGE_TITLE, getString(R.string.error_payment_canceled_title))
            } else {
                putExtra(ExtraKeys.AUTHORIZATION_ID, "AUTH_${System.currentTimeMillis() % 100000}")
                putExtra(ExtraKeys.CARD_HOLDER, "CLIENTE YAPPY")
                putExtra(ExtraKeys.CARD_TYPE, "VISA")
                putExtra(ExtraKeys.CARD_NUM, "**** **** **** 1234")
            }
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    private fun formatAmount(amountStr: String): String {
        return amountStr.toIntOrNull()?.let { cents ->
            String.format("%d.%02d", cents / 100, cents % 100)
        } ?: "0.00"
    }

    private var qrDialog: QrCodeDialog? = null
    private var alternativePaymentJob: Job? = null
    private var statusUpdateJob: Job? = null
    private var paymentStartTime = 0L

    /**
     * Inicia el proceso de pago alternativo con Yappy
     */
    private fun startAlternativePayment(intent: Intent) {
        // Cancelar cualquier trabajo pendiente
        alternativePaymentJob?.cancel()
        statusUpdateJob?.cancel()
        paymentStartTime = 0L

        // Iniciar el procesamiento en el hilo de background
        alternativePaymentJob = lifecycleScope.launch {
            // Crear instancia para conectar con API real de Yappy
            val alternativePayment = AlternativePayment(
                useMock = false  // Usar API real de Yappy
            )

            Log.d(TAG, "Iniciando proceso de pago alternativo con monto ${currentAmount ?: "0"} ${currentCurrencyIso ?: "PAB"}")

            // Configurar callbacks
            alternativePayment.setPaymentCallbacks(object : AlternativePayment.PaymentCallbacks {
                override fun onQrGenerated(hash: String, amount: String) {
                    Log.d(TAG, "QR generado con hash: ${hash.take(10)}... y monto: $amount")

                    // Verificar si es un mensaje de error (formato especial: ERROR:mensaje)
                    if (hash.startsWith("ERROR:")) {
                        val errorMessage = hash.substringAfter("ERROR:")
                        Log.e(TAG, "Error en la generación de QR: $errorMessage")

                        // Mostrar el diálogo de error genérico
                        showPaymentFailureDialog(PaymentFailureDialog.FailureType.FAILED_GENERIC)
                        return
                    }

                    // Mostrar el diálogo QR cuando esté listo (flujo normal)
                    qrDialog = QrCodeDialog.newInstance(hash, amount)
                    qrDialog?.setOnCancelListener {
                        // Si el usuario cancela, intentamos cancelar el trabajo
                        Log.d(TAG, "Usuario canceló el pago con QR")
                        alternativePaymentJob?.cancel()
                        statusUpdateJob?.cancel()
                        // Mostrar pantalla de cancelación
                        showPaymentFailureDialog(PaymentFailureDialog.FailureType.CANCELLED)
                    }
                    qrDialog?.show(supportFragmentManager, "QRDialog")
                }

                override fun onTransactionStatusChanged(status: String) {
                    // Si es la primera vez, iniciamos la cuenta regresiva
                    if (status == AlternativePayment.STATUS_PENDING && paymentStartTime == 0L) {
                        paymentStartTime = System.currentTimeMillis()

                        // Definir el tiempo límite para el escaneo (2 minutos = 120 segundos)
                        val timeoutSeconds = 120

                        // Iniciar un job que actualice la cuenta regresiva cada segundo
                        statusUpdateJob = lifecycleScope.launch {
                            for (remainingSeconds in timeoutSeconds downTo 0) {
                                if (!isActive) break

                                // Formatear en formato MM:SS y mostrar el tiempo restante
                                val minutes = remainingSeconds / 60
                                val seconds = remainingSeconds % 60
                                val timeFormatted = String.format("%02d:%02d", minutes, seconds)
                                val message = getString(R.string.qr_payment_countdown_format_mm_ss, timeFormatted)
                                qrDialog?.updateStatus(message)

                                // Si llegamos a cero, timeout
                                if (remainingSeconds == 0) {
                                    Log.d(TAG, "Tiempo de espera agotado para el escaneo del QR")
                                    qrDialog?.updateStatus(getString(R.string.qr_payment_timeout))

                                    // Notificar timeout y cerrar después de un delay
                                    lifecycleScope.launch {
                                        delay(3000) // 3 segundos para leer el mensaje de timeout
                                        qrDialog?.dismiss()
                                        sendTransactionResponse(false, intent)
                                    }
                                    break
                                }

                                delay(1000) // Actualizar cada segundo
                            }
                        }
                    }

                    // Cancelar el job de actualización si ya no estamos en estado pendiente
                    if (status != AlternativePayment.STATUS_PENDING) {
                        statusUpdateJob?.cancel()
                    }

                    // Actualizar la UI según el estado de la transacción
                    val statusMessage = when (status) {
                        AlternativePayment.STATUS_COMPLETED -> getString(R.string.qr_payment_completed)
                        AlternativePayment.STATUS_FAILED -> getString(R.string.qr_payment_failed)
                        AlternativePayment.STATUS_TIMEOUT -> getString(R.string.qr_payment_timeout)
                        AlternativePayment.STATUS_ERROR -> getString(R.string.error_module_internal)
                        AlternativePayment.STATUS_CANCELED -> getString(R.string.error_payment_canceled)
                        else -> getString(R.string.qr_payment_processing)
                    }

                    Log.d(TAG, "Estado de transacción cambiado a: $status ($statusMessage)")

                    // Actualizar mensaje en el diálogo para todos los estados
                    qrDialog?.updateStatus(statusMessage)

                    // Solo cerramos el diálogo automáticamente si la transacción se completó correctamente
                    // o falló definitivamente. Los estados intermedios se mantienen.
                    when (status) {
                        AlternativePayment.STATUS_COMPLETED -> {
                            // Si el pago se completó con éxito, cerrar el diálogo QR y mostrar pantalla de éxito
                            statusUpdateJob?.cancel() // Detener contador de QR
                            qrDialog?.dismiss() // Cerrar diálogo del QR inmediatamente

                            // Mostrar el diálogo de éxito
                            showPaymentSuccessDialog()
                        }
                        AlternativePayment.STATUS_FAILED,
                        AlternativePayment.STATUS_ERROR -> {
                            // Cerrar el diálogo QR y mostrar la pantalla de fallo genérico
                            statusUpdateJob?.cancel()
                            qrDialog?.dismiss()
                            showPaymentFailureDialog(PaymentFailureDialog.FailureType.FAILED_GENERIC)
                        }
                        AlternativePayment.STATUS_TIMEOUT -> {
                            // Cerrar el diálogo QR y mostrar la pantalla de timeout
                            statusUpdateJob?.cancel()
                            qrDialog?.dismiss()
                            showPaymentFailureDialog(PaymentFailureDialog.FailureType.TIMEOUT)
                        }
                        AlternativePayment.STATUS_CANCELED -> {
                            // Cerrar el diálogo QR y mostrar la pantalla de cancelación
                            statusUpdateJob?.cancel()
                            qrDialog?.dismiss()
                            showPaymentFailureDialog(PaymentFailureDialog.FailureType.CANCELLED)
                        }
                        // En estado PENDING no cerramos el diálogo, esperamos a que el usuario
                        // complete el pago o cancele manualmente
                    }
                }

                override fun onPaymentComplete(success: Boolean) {
                    // Notificar el resultado a HioPos
                    Log.d(TAG, "Pago completado con resultado: ${if (success) "EXITOSO" else "FALLIDO"}")

                    if (success) {
                        // Si ya mostramos el diálogo de éxito en onTransactionStatusChanged,
                        // solo logueamos que se completó exitosamente
                        if (successDialog?.isAdded != true) {
                            Log.d(TAG, "onPaymentComplete: Éxito, pero el diálogo de éxito no se mostró. Mostrándolo ahora.")
                            showPaymentSuccessDialog()
                        } else {
                            Log.d(TAG, "onPaymentComplete: Éxito. El diálogo de éxito ya está siendo mostrado.")
                        }
                    } else {
                        // Si el pago falló y no se ha mostrado el diálogo de fallo
                        if (failureDialog?.isAdded != true) {
                            Log.d(TAG, "onPaymentComplete: Fallido, mostrando diálogo de fallo genérico.")
                            qrDialog?.dismiss() // Asegurarse de cerrar el diálogo QR
                            showPaymentFailureDialog(PaymentFailureDialog.FailureType.FAILED_GENERIC)
                        } else {
                            Log.d(TAG, "onPaymentComplete: Fallido. El diálogo de fallo ya está siendo mostrado.")
                        }
                    }
                }
            })

            // Iniciar el proceso de pago
            try {
                // El resultado lo procesaremos a través de los callbacks
                Log.d(TAG, "Iniciando flujo completo de pago")
                Log.d(TAG, "Tipo de transacción: $currentTransactionType, ID de transacción: $currentTransactionIdHio")
                alternativePayment.procesarPagoCompleto(
                    amount = currentAmount ?: "0",
                    transactionType = currentTransactionType ?: "VENTA",
                    transactionIdHio = currentTransactionIdHio ?: 0
                )
            } catch (e: Exception) {
                // En caso de error, asegurar que cerramos el diálogo y notificamos fallo
                Log.e(TAG, "Error en pago alternativo", e)
                qrDialog?.dismiss()
                sendTransactionResponse(false, intent)
            }
        }
    }

    /**
     * Muestra el diálogo de pago exitoso con animación de confeti
     */
    private fun showPaymentSuccessDialog() {
        if (successDialog?.isAdded == true) {
            return // Ya se está mostrando
        }

        successDialog = PaymentSuccessDialog.newInstance()
        successDialog?.setOnFinishListener {
            // Cuando el usuario presiona "TERMINAR" en el diálogo de éxito
            Log.d(TAG, "Flujo de pago completado y confirmado por el usuario.")
            // Enviamos la respuesta exitosa a HioPos
            sendTransactionResponse(true, intent)
        }

        successDialog?.show(supportFragmentManager, "PaymentSuccessDialog")
    }

    /**
     * Muestra el diálogo de pago fallido/cancelado/timeout
     */
    private fun showPaymentFailureDialog(type: PaymentFailureDialog.FailureType) {
        if (failureDialog?.isAdded == true || successDialog?.isAdded == true) {
            return // Evitar múltiples diálogos
        }

        failureDialog = PaymentFailureDialog.newInstance(type)
        failureDialog?.setOnAcknowledgeListener {
            Log.d(TAG, "Flujo de pago no exitoso. Confirmado por usuario.")
            sendTransactionResponse(false, intent) // Envía FAILED
        }

        failureDialog?.show(supportFragmentManager, "PaymentFailureDialog")
    }
}
