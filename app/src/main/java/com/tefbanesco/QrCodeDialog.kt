package com.tefbanesco

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Diálogo que muestra un código QR para ser escaneado
 */
class QrCodeDialog : DialogFragment() {
    private var qrHash: String? = null
    private var amount: String? = null
    private var onCancelListener: (() -> Unit)? = null
    private var statusTextView: TextView? = null

    companion object {
        private const val TAG = "QrCodeDialog"
        private const val ARG_QR_HASH = "qr_hash"
        private const val ARG_AMOUNT = "amount"

        fun newInstance(qrHash: String, amount: String): QrCodeDialog {
            return QrCodeDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_QR_HASH, qrHash)
                    putString(ARG_AMOUNT, amount)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            qrHash = it.getString(ARG_QR_HASH)
            amount = it.getString(ARG_AMOUNT)
        }
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog_MinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_qr_code, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAmount = view.findViewById<TextView>(R.id.tvQrAmount)
        val ivQrCode = view.findViewById<ImageView>(R.id.ivQrCode)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelQr)
        statusTextView = view.findViewById<TextView>(R.id.tvQrStatus)

        // Mostrar el monto formateado
        val formattedAmount = formatAmount(amount ?: "0")
        tvAmount.text = getString(R.string.payment_amount_format).format(formattedAmount, "PAB")

        // Establecer texto de estado inicial
        statusTextView?.text = getString(R.string.qr_payment_processing)

        // Generar y mostrar el código QR
        Log.d(TAG, "=== Generando QR con hash: $qrHash ===")
        Log.d(TAG, "Longitud del hash: ${qrHash?.length ?: 0} caracteres")

        generateQrCode(qrHash ?: "")?.let {
            ivQrCode.setImageBitmap(it)
            Log.d(TAG, "QR generado exitosamente, listo para ser escaneado")
        } ?: run {
            Log.e(TAG, "Error generando QR con hash: $qrHash")
            updateStatus(getString(R.string.error_module_internal))
            // Mostrar mensaje de error pero NO cerrar automáticamente el diálogo
            // para que el usuario pueda leer el error y decidir cancelar manualmente
        }

        // Botón para cancelar el pago
        btnCancel.setOnClickListener {
            updateStatus(getString(R.string.error_payment_canceled))
            onCancelListener?.invoke()
            dismiss(1500) // Cerrar con delay para mostrar mensaje de cancelación
        }

        // Asegurarnos de que el diálogo no se pueda cerrar al tocar fuera
        dialog?.setCanceledOnTouchOutside(false)
        dialog?.setCancelable(false)
    }

    /**
     * Actualiza el mensaje de estado del diálogo
     */
    fun updateStatus(statusMessage: String) {
        if (isAdded && !isDetached) {
            Log.d(TAG, "Actualizando estado a: $statusMessage")
            statusTextView?.text = statusMessage
        }
    }

    /**
     * Cierra el diálogo con un retraso opcional
     */
    fun dismiss(delayMillis: Long = 0) {
        if (delayMillis > 0) {
            // Utilizar Handler para cerrar con delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isAdded && !isDetached) {
                    super.dismiss()
                }
            }, delayMillis)
        } else {
            if (isAdded && !isDetached) {
                super.dismiss()
            }
        }
    }

    /**
     * Formatea el monto de "1234" a "12.34"
     */
    private fun formatAmount(amountStr: String): String {
        return amountStr.toIntOrNull()?.let { cents ->
            String.format("%d.%02d", cents / 100, cents % 100)
        } ?: "0.00"
    }

    /**
     * Genera un bitmap del código QR a partir del hash
     */
    private fun generateQrCode(hash: String): Bitmap? {
        if (hash.isBlank()) return null
        try {
            // El QR se genera directamente con el hash, sin URL
            val qrContent = hash

            Log.d(TAG, "Generando QR directamente con hash: $qrContent")

            val qrWriter = QRCodeWriter()
            val bitMatrix = qrWriter.encode(qrContent, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: WriterException) {
            Log.e(TAG, "Error generando QR", e)
            return null
        }
    }

    fun setOnCancelListener(listener: () -> Unit) {
        onCancelListener = listener
    }
}