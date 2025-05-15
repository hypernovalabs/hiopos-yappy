package com.tefbanesco

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

class RefundFinalConfirmationDialog : DialogFragment() {

    interface RefundConfirmationListener {
        fun onRefundConfirmed(detail: String)
        fun onRefundConfirmationCancelled()
    }

    private var listener: RefundConfirmationListener? = null
    private var amountToDisplay: String? = null
    
    // Views as member variables
    private lateinit var etRefundDetail: EditText
    private lateinit var cbConfirmManualRefund: CheckBox
    private lateinit var btnConfirm: Button
    private var originalCheckBoxTextColor: Int = Color.GRAY // To restore the color

    companion object {
        private const val ARG_AMOUNT = "amount_to_display"
        fun newInstance(amount: String): RefundFinalConfirmationDialog {
            val args = Bundle()
            args.putString(ARG_AMOUNT, amount)
            val fragment = RefundFinalConfirmationDialog()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        amountToDisplay = arguments?.getString(ARG_AMOUNT)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Make the dialog background transparent to show the rounded corners properly
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return inflater.inflate(R.layout.dialog_refund_final_confirmation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvFinalRefundAmount: TextView = view.findViewById(R.id.tvFinalRefundAmount)
        etRefundDetail = view.findViewById(R.id.etRefundDetail)
        cbConfirmManualRefund = view.findViewById(R.id.cbConfirmManualRefund)
        val btnCancel: Button = view.findViewById(R.id.btnCancelFinalConfirmation)
        btnConfirm = view.findViewById(R.id.btnConfirmFinalRefund)

        tvFinalRefundAmount.text = "Monto devuelto: $amountToDisplay"
        originalCheckBoxTextColor = cbConfirmManualRefund.currentTextColor // Save original color

        // TextWatcher for detail field
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateInputsAndToggleButtonState()
            }
        }
        etRefundDetail.addTextChangedListener(textWatcher)

        // Listener for CheckBox
        cbConfirmManualRefund.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbConfirmManualRefund.setTextColor(originalCheckBoxTextColor) // Restore color if checked
            }
            validateInputsAndToggleButtonState()
        }

        btnCancel.setOnClickListener {
            listener?.onRefundConfirmationCancelled()
            dismiss()
        }

        btnConfirm.setOnClickListener {
            val detailText = etRefundDetail.text.toString().trim()
            var isValid = true

            if (detailText.isEmpty()) {
                etRefundDetail.error = "El detalle es obligatorio" // Show error in EditText
                isValid = false
            } else {
                etRefundDetail.error = null // Clear error
            }

            if (!cbConfirmManualRefund.isChecked) {
                // Set CheckBox text to red
                cbConfirmManualRefund.setTextColor(ContextCompat.getColor(requireContext(), R.color.refund_validation_error_red))
                isValid = false
            } else {
                cbConfirmManualRefund.setTextColor(originalCheckBoxTextColor) // Restore color if already checked
            }

            if (isValid) {
                listener?.onRefundConfirmed(detailText)
                dismiss()
            }
        }
        dialog?.setCanceledOnTouchOutside(false)
        validateInputsAndToggleButtonState() // Initial button state
    }

    private fun validateInputsAndToggleButtonState() {
        val detailIsNotEmpty = etRefundDetail.text.toString().trim().isNotEmpty()
        val checkBoxIsChecked = cbConfirmManualRefund.isChecked
        btnConfirm.isEnabled = detailIsNotEmpty && checkBoxIsChecked
    }

    fun setRefundConfirmationListener(listener: RefundConfirmationListener) {
        this.listener = listener
    }

    override fun onStart() {
        super.onStart()
        val dialogWindow = dialog?.window
        if (dialogWindow != null) {
            // Set the dialog width to 90% of screen width for better proportions
            val displayMetrics = requireContext().resources.displayMetrics
            val width = (displayMetrics.widthPixels * 0.90).toInt() // 90% of screen width
            dialogWindow.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
            
            // Ensure the dialog is centered
            dialogWindow.setGravity(android.view.Gravity.CENTER)
        }
    }
}