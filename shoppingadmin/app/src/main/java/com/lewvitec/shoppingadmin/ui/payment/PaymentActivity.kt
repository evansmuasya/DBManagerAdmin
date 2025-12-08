package com.lewvitec.shoppingadmin.ui.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
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

    // Activity extras
    private var amount: Double = 0.0
    private var planId: Int = 0
    private var planName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get intent extras
        amount = intent.getDoubleExtra("amount", 0.0)
        planId = intent.getIntExtra("plan_id", 0)
        planName = intent.getStringExtra("plan_name") ?: ""

        setupUI()
        setupClickListeners()
        setupObservers()
        loadPaymentMethods()
    }

    override fun onResume() {
        super.onResume()
        checkIfReturnedFromPesapal()
    }

    override fun onDestroy() {
        viewModel.cancelPolling()
        viewModel.stopSubscriptionPolling()
        super.onDestroy()
    }

    private fun setupUI() {
        binding.tvAmount.text = "KES ${String.format("%.2f", amount)}"
        binding.tvPlanName.text = planName
    }

    private fun setupClickListeners() {
        // M-Pesa card click
        binding.cardMpesa.setOnClickListener {
            Log.d("PaymentActivity", "M-Pesa card clicked")
            showMpesaDialog()
        }

        // PesaPal card click
        binding.cardPesapal.setOnClickListener {
            Log.d("PaymentActivity", "PesaPal card clicked")
            initiatePesapalSubscription()
        }

        // Card payment click
        binding.cardCard.setOnClickListener {
            Log.d("PaymentActivity", "Card payment clicked")
            showCardPayment()
        }

        // Test button (temporary - remove in production)
        binding.btnTestPesapal.setOnClickListener {
            Log.d("PaymentActivity", "Test button clicked")
            testPesapalDirect()
        }
    }

    private fun setupObservers() {
        // Payment Methods observer
        viewModel.paymentMethods.observe(this) { methods ->
            if (methods.isNullOrEmpty()) {
                binding.cardMpesa.visibility = View.GONE
                binding.cardPesapal.visibility = View.GONE
                binding.cardCard.visibility = View.GONE
            } else {
                // Show available payment methods
                val hasMpesa = methods.any { it.name.equals("mpesa", ignoreCase = true) }
                val hasPesapal = methods.any { it.name.equals("pesapal", ignoreCase = true) }
                val hasCard = methods.any { it.name.equals("card", ignoreCase = true) }

                binding.cardMpesa.visibility = if (hasMpesa) View.VISIBLE else View.GONE
                binding.cardPesapal.visibility = if (hasPesapal) View.VISIBLE else View.GONE
                binding.cardCard.visibility = if (hasCard) View.VISIBLE else View.GONE
            }
        }

        // M-Pesa response observer
        viewModel.mpesaResponse.observe(this) { response ->
            response?.let {
                if (it.success) {
                    Toast.makeText(this, "Check your phone to complete payment", Toast.LENGTH_LONG).show()
                    it.checkoutRequestId?.let { checkoutId ->
                        viewModel.startPaymentStatusPolling(checkoutId)
                    }
                } else {
                    Toast.makeText(this, "Payment failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        // PesaPal response observer
        viewModel.pesapalResponse.observe(this) { response ->
            binding.progressBar.visibility = View.GONE

            response?.let {
                if (it.success) {
                    handleSuccessfulPesapalResponse(it)
                } else {
                    handleFailedPesapalResponse(it)
                }
            }
        }

        // Subscription status observer (for polling)
        viewModel.subscriptionStatus.observe(this) { statusResponse ->
            binding.progressBar.visibility = View.GONE
            statusResponse?.let {
                handleSubscriptionStatus(it)
            }
        }

        // Payment status observer (for M-Pesa)
        viewModel.paymentStatus.observe(this) { status ->
            status?.let {
                when (it) {
                    "completed" -> {
                        Toast.makeText(this, "Payment completed successfully!", Toast.LENGTH_LONG).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            setResult(RESULT_OK)
                            finish()
                        }, 1500)
                    }
                    "failed" -> {
                        Toast.makeText(this, "Payment failed. Please try again.", Toast.LENGTH_LONG).show()
                    }
                    "timeout" -> {
                        Toast.makeText(this, "Payment timeout. Please check your phone.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        // Loading observer
        viewModel.loading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
            setViewsEnabled(isLoading != true)
        }
    }

    private fun handleSuccessfulPesapalResponse(response: com.lewvitec.shoppingadmin.models.PesapalPaymentResponse) {
        Log.d("PaymentActivity", "PesaPal response success: ${response.message}")

        response.redirectUrl?.let { url ->
            Log.d("PaymentActivity", "Opening PesaPal URL: $url")

            // Save pending payment info BEFORE redirecting
            response.orderTrackingId?.let { trackingId ->
                savePendingPayment(trackingId, planId)
            }

            // Open PesaPal in browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)

            Toast.makeText(this, "Complete payment in PesaPal...", Toast.LENGTH_LONG).show()
        } ?: run {
            Toast.makeText(this, "Payment URL not received", Toast.LENGTH_SHORT).show()
            Log.e("PaymentActivity", "No redirect URL in response")
        }
    }

    private fun handleFailedPesapalResponse(response: com.lewvitec.shoppingadmin.models.PesapalPaymentResponse) {
        Toast.makeText(this, "Payment failed: ${response.message}", Toast.LENGTH_LONG).show()
        Log.e("PaymentActivity", "PesaPal error: ${response.message}")
    }

    private fun handleSubscriptionStatus(statusResponse: com.lewvitec.shoppingadmin.models.PaymentStatusResponse) {
        when (statusResponse.status) {
            "completed" -> {
                Toast.makeText(this, "Payment completed successfully!", Toast.LENGTH_LONG).show()
                clearPendingPayment()
                Handler(Looper.getMainLooper()).postDelayed({
                    setResult(RESULT_OK)
                    finish()
                }, 1500)
            }
            "failed" -> {
                Toast.makeText(this, "Payment failed: ${statusResponse.message}", Toast.LENGTH_LONG).show()
                clearPendingPayment()
            }
            "timeout" -> {
                Toast.makeText(this, "Payment verification timeout. Please check your email.", Toast.LENGTH_LONG).show()
                clearPendingPayment()
            }
            else -> {
                Toast.makeText(this, "Payment status: ${statusResponse.status}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initiatePesapalSubscription() {
        Log.d("PaymentActivity", "initiatePesapalSubscription() called")

        val sessionId = preferenceManager.getSessionId()
        val tenantId = tenantManager.getCurrentTenantId()

        Log.d("PaymentActivity", "Session ID: $sessionId")
        Log.d("PaymentActivity", "Tenant ID: $tenantId")
        Log.d("PaymentActivity", "Amount: $amount")
        Log.d("PaymentActivity", "Plan ID: $planId")

        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            return
        }

        if (amount <= 0.0 || planId <= 0) {
            Toast.makeText(this, "Invalid payment details", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        binding.progressBar.visibility = View.VISIBLE

        viewModel.initiatePesapalSubscription(sessionId, tenantId, planId, amount)
    }

    private fun showMpesaDialog() {
        // TODO: Implement M-Pesa dialog with phone number input
        Toast.makeText(this, "M-Pesa payment selected", Toast.LENGTH_SHORT).show()
        // Show dialog to enter phone number, then call:
        // viewModel.initiateMpesaPayment(sessionId, tenantId, planId, amount, phoneNumber)
    }

    private fun showCardPayment() {
        Toast.makeText(this, "Card payment coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun loadPaymentMethods() {
        val sessionId = preferenceManager.getSessionId()
        if (sessionId.isNotEmpty()) {
            viewModel.loadPaymentMethods(sessionId)
        } else {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun checkIfReturnedFromPesapal() {
        val pendingTrackingId = preferenceManager.getPendingPesapalTrackingId()
        val pendingSubscriptionId = preferenceManager.getPendingSubscriptionId()

        if (!pendingTrackingId.isNullOrEmpty() && pendingSubscriptionId > 0) {
            Log.d("PaymentActivity", "Resuming after PesaPal, starting polling...")
            Log.d("PaymentActivity", "Tracking ID: $pendingTrackingId, Subscription ID: $pendingSubscriptionId")

            // Start polling for status
            viewModel.startSubscriptionPolling(pendingTrackingId, pendingSubscriptionId)

            // Show loading
            binding.progressBar.visibility = View.VISIBLE
            setViewsEnabled(false)
        }
    }

    private fun savePendingPayment(trackingId: String, subscriptionId: Int) {
        preferenceManager.savePendingPesapalPayment(trackingId, subscriptionId)
        Log.d("PaymentActivity", "Saved pending payment: trackingId=$trackingId, subscriptionId=$subscriptionId")
    }

    private fun clearPendingPayment() {
        preferenceManager.clearPendingPesapalPayment()
        Log.d("PaymentActivity", "Cleared pending payment")
    }

    private fun setViewsEnabled(enabled: Boolean) {
        binding.cardMpesa.isEnabled = enabled
        binding.cardPesapal.isEnabled = enabled
        binding.cardCard.isEnabled = enabled
        binding.btnTestPesapal.isEnabled = enabled
    }

    private fun testPesapalDirect() {
        Toast.makeText(this, "Testing PesaPal directly...", Toast.LENGTH_SHORT).show()

        // Hardcode for testing
        val sessionId = preferenceManager.getSessionId()
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val tenantId = tenantManager.getCurrentTenantId()
        val testPlanId = 1
        val testAmount = 100.0

        Log.d("PaymentActivity", "Direct test with values: sessionId=$sessionId, tenantId=$tenantId")
        viewModel.initiatePesapalSubscription(sessionId, tenantId, testPlanId, testAmount)
    }
}