package com.lewvitec.shoppingadmin.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewvitec.shoppingadmin.models.*
import com.lewvitec.shoppingadmin.repository.AdminRepository
import com.lewvitec.shoppingadmin.repository.Result
import com.lewvitec.shoppingadmin.utils.PreferenceManager
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val tenantManager: TenantManager,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _paymentMethods = MutableLiveData<List<PaymentMethod>>(emptyList())
    val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    private val _mpesaResponse = MutableLiveData<MpesaPaymentResponse?>()
    val mpesaResponse: LiveData<MpesaPaymentResponse?> = _mpesaResponse

    private val _pesapalResponse = MutableLiveData<PesapalPaymentResponse?>()
    val pesapalResponse: LiveData<PesapalPaymentResponse?> = _pesapalResponse

    private val _paymentStatus = MutableLiveData<String?>()
    val paymentStatus: LiveData<String?> = _paymentStatus

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _subscriptionStatus = MutableLiveData<PaymentStatusResponse?>()
    val subscriptionStatus: LiveData<PaymentStatusResponse?> = _subscriptionStatus

    private var subscriptionPollingJob: Job? = null
    private var pollingJob: Job? = null

    fun loadPaymentMethods(sessionId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.getPaymentMethods(sessionId)

                when (result) {
                    is Result.Success -> {
                        _paymentMethods.value = result.data ?: emptyList()
                    }
                    is Result.Failure -> {
                        _paymentMethods.value = emptyList()
                    }
                    Result.Loading -> {
                        // Loading state handled by _loading
                    }
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun initiateMpesaPayment(sessionId: String, tenantId: Int, planId: Int, amount: Double, phone: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.processMpesaPayment(sessionId, tenantId, planId, amount, phone)

                when (result) {
                    is Result.Success -> {
                        _mpesaResponse.value = result.data
                    }
                    is Result.Failure -> {
                        _mpesaResponse.value = MpesaPaymentResponse(
                            success = false,
                            message = result.exception.message ?: "Payment failed"
                        )
                    }
                    Result.Loading -> {
                        // Loading state handled by _loading
                    }
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun initiatePesapalSubscription(sessionId: String, tenantId: Int, planId: Int, amount: Double) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.processPesapalSubscription(sessionId, tenantId, planId, amount)

                when (result) {
                    is Result.Success -> {
                        _pesapalResponse.value = result.data
                    }
                    is Result.Failure -> {
                        _pesapalResponse.value = PesapalPaymentResponse(
                            success = false,
                            message = result.exception.message ?: "Subscription payment failed"
                        )
                    }
                    Result.Loading -> {
                        // Loading state handled by _loading
                    }
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun startPaymentStatusPolling(checkoutRequestId: String) {
        pollingJob?.cancel()

        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 30) {
                delay(10000) // Every 10 seconds
                attempts++

                val sessionId = preferenceManager.getSessionId()
                val result = repository.checkMpesaPaymentStatus(sessionId, checkoutRequestId)

                when (result) {
                    is Result.Success -> {
                        val statusResponse = result.data
                        when (statusResponse?.status) {
                            "completed" -> {
                                _paymentStatus.value = "completed"
                                pollingJob?.cancel()
                                return@launch
                            }
                            "failed" -> {
                                _paymentStatus.value = "failed"
                                pollingJob?.cancel()
                                return@launch
                            }
                        }
                    }
                    is Result.Failure -> {
                        // Log error but continue polling
                    }
                    Result.Loading -> {
                        // Continue polling
                    }
                }

                if (pollingJob?.isCancelled == true) {
                    return@launch
                }
            }

            // If polling ends without success
            _paymentStatus.value = "timeout"
        }
    }

    fun cancelPolling() {
        pollingJob?.cancel()
    }

    fun startSubscriptionPolling(orderTrackingId: String, subscriptionId: Int) {
        subscriptionPollingJob?.cancel()

        subscriptionPollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < 60) { // Poll for up to 10 minutes (60 * 10 seconds)
                delay(10000) // Every 10 seconds

                val sessionId = preferenceManager.getSessionId()
                val result = repository.checkPesapalPaymentStatus(sessionId, orderTrackingId)

                when (result) {
                    is Result.Success -> {
                        val statusResponse = result.data
                        when (statusResponse?.status) {
                            "completed" -> {
                                _subscriptionStatus.value = statusResponse
                                subscriptionPollingJob?.cancel()
                                // Also update payment status for UI
                                _paymentStatus.value = "completed"
                                return@launch
                            }
                            "failed" -> {
                                _subscriptionStatus.value = statusResponse
                                subscriptionPollingJob?.cancel()
                                _paymentStatus.value = "failed"
                                return@launch
                            }
                        }
                    }
                    is Result.Failure -> {
                        // Log error but continue polling
                    }
                    Result.Loading -> {
                        // Continue polling
                    }
                }

                attempts++

                // Stop after 10 minutes
                if (attempts >= 60) {
                    _subscriptionStatus.value = PaymentStatusResponse(
                        success = false,
                        message = "Payment status check timeout",
                        status = "timeout"
                    )
                    return@launch
                }

                if (subscriptionPollingJob?.isCancelled == true) {
                    return@launch
                }
            }
        }
    }

    fun stopSubscriptionPolling() {
        subscriptionPollingJob?.cancel()
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        subscriptionPollingJob?.cancel()
    }
}