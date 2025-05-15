package com.tefbanesco

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment

class PaymentFailureDialog : DialogFragment() {

    enum class FailureType {
        CANCELLED, TIMEOUT, FAILED_GENERIC
    }

    private var failureType: FailureType = FailureType.FAILED_GENERIC
    private var onAcknowledgeListener: (() -> Unit)? = null

    companion object {
        private const val ARG_FAILURE_TYPE = "failure_type"

        fun newInstance(type: FailureType): PaymentFailureDialog {
            return PaymentFailureDialog().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_FAILURE_TYPE, type)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            failureType = it.getSerializable(ARG_FAILURE_TYPE) as? FailureType ?: FailureType.FAILED_GENERIC
        }
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
    }

    override fun onStart() {
        super.onStart()
        dialog?.let { dialog ->
            // Configurar el diálogo para ocupar toda la pantalla
            dialog.window?.apply {
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Fondo transparente
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_payment_failure, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivFailureIcon: ImageView = view.findViewById(R.id.ivFailureIcon)
        val tvFailureMessage: TextView = view.findViewById(R.id.tvFailureMessage)
        val tvFailureSubMessage: TextView = view.findViewById(R.id.tvFailureSubMessage) // Opcional
        val btnAcknowledge: Button = view.findViewById(R.id.btnFailureAcknowledge)

        when (failureType) {
            FailureType.CANCELLED -> {
                ivFailureIcon.setImageResource(R.drawable.ic_yappy_cancel_x)
                tvFailureMessage.text = getString(R.string.error_payment_canceled_title) // "Pago Cancelado"
                tvFailureSubMessage.text = getString(R.string.payment_failure_canceled_submessage)
                tvFailureSubMessage.visibility = View.VISIBLE
            }
            FailureType.TIMEOUT -> {
                ivFailureIcon.setImageResource(R.drawable.ic_yappy_timeout_clock)
                tvFailureMessage.text = getString(R.string.qr_payment_timeout) // "Tiempo Expirado"
                tvFailureSubMessage.text = getString(R.string.payment_failure_timeout_submessage)
                tvFailureSubMessage.visibility = View.VISIBLE
            }
            FailureType.FAILED_GENERIC -> {
                ivFailureIcon.setImageResource(R.drawable.ic_yappy_cancel_x) // O un icono de error genérico
                tvFailureMessage.text = getString(R.string.qr_payment_failed) // "Pago Fallido"
                tvFailureSubMessage.text = getString(R.string.payment_failure_generic_submessage)
                tvFailureSubMessage.visibility = View.VISIBLE
            }
        }

        btnAcknowledge.setOnClickListener {
            onAcknowledgeListener?.invoke()
            dismiss()
        }

        // Configurar cierre automático después de 5 segundos
        view.postDelayed({
            if (isAdded && !isDetached) {
                onAcknowledgeListener?.invoke()
                dismiss()
            }
        }, 5000) // 5 segundos

        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
    }

    fun setOnAcknowledgeListener(listener: () -> Unit) {
        this.onAcknowledgeListener = listener
    }
}