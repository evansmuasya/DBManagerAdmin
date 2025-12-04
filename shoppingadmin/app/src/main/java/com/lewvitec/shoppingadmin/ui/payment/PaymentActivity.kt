package com.lewvitec.shoppingadmin.ui.payment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lewvitec.shoppingadmin.databinding.ActivityPaymentBinding
import com.lewvitec.shoppingadmin.utils.PreferenceManager
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PaymentActivity : AppCompatActivity() {

    @Inject
    lateinit var tenantManager: TenantManager

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private lateinit var binding: ActivityPaymentBinding
    private val viewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val amount = intent.getDoubleExtra("amount", 0.0)
        val planId = intent.getIntExtra("plan_id", 0)
        val planName = intent.getStringExtra("plan_name") ?: ""

        setupUI(amount, planName)
        setupObservers()
        loadPaymentMethods()
    }

    private fun setupUI(amount: Double, planName: String) {
        binding.tvAmount.text = "KES ${String.format("%.2f", amount)}"
        binding.tvPlanName.text = planName

        binding.btnPayMpesa.setOnClickListener {
            showMpesaDialog()
        }

        binding.btnPayPesapal.setOnClickListener {
            showPesapalDialog()
        }

        binding.btnPayCard.setOnClickListener {
            // Call the method
            showCardPayment()
        }
    }

    private fun showMpesaDialog() {
        // Create dialog using AlertDialog
        val editText = android.widget.EditText(this)
        editText.hint = "07XX XXX XXX"
        editText.inputType = android.text.InputType.TYPE_CLASS_PHONE

        val dialog = AlertDialog.Builder(this)
            .setTitle("Pay with M-Pesa")
            .setMessage("Enter your M-Pesa phone number")
            .setView(editText)
            .setPositiveButton("Pay") { dialogInterface, _ ->
                val phone = editText.text.toString()

                if (validatePhoneNumber(phone)) {
                    processMpesaPayment(phone)
                } else {
                    Toast.makeText(this, "Invalid phone number", Toast.LENGTH_SHORT).show()
                }
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showPesapalDialog() {
        // Show Pesapal webview or redirect
        val sessionId = preferenceManager.getSessionId()
        val tenantId = tenantManager.getCurrentTenantId()
        val amount = intent.getDoubleExtra("amount", 0.0)
        val planId = intent.getIntExtra("plan_id", 0)

        viewModel.initiatePesapalPayment(sessionId, tenantId, planId, amount)
    }

    private fun showCardPayment() {
        // Implement card payment logic
        Toast.makeText(this, "Card payment coming soon", Toast.LENGTH_SHORT).show()
        // You can implement this later with Stripe or another card processor
    }

    private fun validatePhoneNumber(phone: String): Boolean {
        // Simple validation for Kenyan numbers
        val cleaned = phone.replace("[^0-9]".toRegex(), "")
        return cleaned.length in 9..12 && (cleaned.startsWith("7") ||
                cleaned.startsWith("07") || cleaned.startsWith("2547"))
    }

    private fun processMpesaPayment(phone: String) {
        val sessionId = preferenceManager.getSessionId()
        val tenantId = tenantManager.getCurrentTenantId()
        val amount = intent.getDoubleExtra("amount", 0.0)
        val planId = intent.getIntExtra("plan_id", 0)

        viewModel.initiateMpesaPayment(sessionId, tenantId, planId, amount, phone)
    }

    private fun setupObservers() {
        viewModel.paymentMethods.observe(this) { methods ->
            // Update UI with available payment methods
            if (methods.isNullOrEmpty()) {
                binding.cardMpesa.visibility = View.GONE
                binding.cardPesapal.visibility = View.GONE
                binding.cardCard.visibility = View.GONE
            }
        }

        viewModel.mpesaResponse.observe(this) { response ->
            if (response.success) {
                Toast.makeText(this, "Check your phone to complete payment", Toast.LENGTH_LONG).show()
                // Start polling for payment status
                response.checkoutRequestId?.let {
                    viewModel.startPaymentStatusPolling(it)
                }
            } else {
                Toast.makeText(this, "Payment failed: ${response.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.paymentStatus.observe(this) { status ->
            when (status) {
                "completed" -> {
                    Toast.makeText(this, "Payment completed successfully!", Toast.LENGTH_LONG).show()
                    finish()
                }
                "failed" -> {
                    Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_LONG).show()
                }
                "timeout" -> {
                    Toast.makeText(this, "Payment timeout. Please check your phone.", Toast.LENGTH_LONG).show()
                }
            }
        }

        viewModel.loading.observe(this) { isLoading ->
            // Show/hide loading indicator if you have one
            // binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun loadPaymentMethods() {
        val sessionId = preferenceManager.getSessionId()
        viewModel.loadPaymentMethods(sessionId)
    }
}