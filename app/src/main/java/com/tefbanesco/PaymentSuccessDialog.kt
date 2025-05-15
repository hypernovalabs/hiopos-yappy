package com.tefbanesco

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class PaymentSuccessDialog : DialogFragment() {

    private var onFinishListener: (() -> Unit)? = null

    companion object {
        fun newInstance(): PaymentSuccessDialog {
            return PaymentSuccessDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        return inflater.inflate(R.layout.dialog_payment_success, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnFinish: Button = view.findViewById(R.id.btnFinishPayment)
        val konfettiView: KonfettiView = view.findViewById(R.id.konfettiView)

        btnFinish.setOnClickListener {
            onFinishListener?.invoke()
            dismiss()
        }

        // Iniciar animación de confeti
        startConfettiAnimation(konfettiView)

        // Configurar cierre automático después de 5 segundos
        view.postDelayed({
            if (isAdded && !isDetached) {
                onFinishListener?.invoke()
                dismiss()
            }
        }, 5000) // 5 segundos

        dialog?.setCancelable(false)
        dialog?.setCanceledOnTouchOutside(false)
    }

    private fun startConfettiAnimation(konfettiView: KonfettiView) {
        // Explosión de confeti desde el centro (donde está el ícono de check)
        val centerExplosion = Party(
            speed = 0f,
            maxSpeed = 30f,
            damping = 0.9f,
            spread = 360,
            colors = listOf(Color.YELLOW, Color.RED, Color.MAGENTA, Color.BLUE), // Colores festivos
            emitter = Emitter(duration = 100, TimeUnit.MILLISECONDS).max(100),
            position = Position.Relative(0.5, 0.3)
        )
        
        // Lluvia continua suave de confeti desde arriba
        val fallingSoftly = Party(
            speed = 2f,
            maxSpeed = 10f,
            damping = 0.9f,
            spread = 10, // Un entero, no un objeto Spread
            colors = listOf(Color.CYAN, Color.GREEN, Color.rgb(255, 169, 64), Color.rgb(0, 132, 61)), // Colores relacionados con Yappy
            emitter = Emitter(duration = 3000, TimeUnit.MILLISECONDS).perSecond(50),
            position = Position.Relative(0.0, 0.0).between(Position.Relative(1.0, 0.0))
        )

        // Iniciar ambas animaciones
        konfettiView.start(centerExplosion)
        konfettiView.start(fallingSoftly)
    }

    fun setOnFinishListener(listener: () -> Unit) {
        this.onFinishListener = listener
    }
}