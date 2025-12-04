package com.lewvitec.shoppingadmin.ui.payment

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewvitec.shoppingadmin.models.*
import com.lewvitec.shoppingadmin.repository.AdminRepository
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

    private val _paymentMethods = MutableLiveData<List<PaymentMethod>>()
    val paymentMethods: LiveData<List<PaymentMethod>> = _paymentMethods

    private val _mpesaResponse = MutableLiveData<MpesaPaymentResponse>()
    val mpesaResponse: LiveData<MpesaPaymentResponse> = _mpesaResponse

    private val _pesapalResponse = MutableLiveData<PesapalPaymentResponse>()
    val pesapalResponse: LiveData<PesapalPaymentResponse> = _pesapalResponse

    private val _paymentStatus = MutableLiveData<String>()
    val paymentStatus: LiveData<String> = _paymentStatus

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private var pollingJob: Job? = null

    fun loadPaymentMethods(sessionId: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.getPaymentMethods(sessionId)
                result.onSuccess { methods ->
                    _paymentMethods.value = methods
                }.onFailure { error ->
                    // Handle error
                    _paymentMethods.value = emptyList()
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
                result.onSuccess { response ->
                    _mpesaResponse.value = response
                }.onFailure { error ->
                    _mpesaResponse.value = MpesaPaymentResponse(
                        success = false,
                        message = error.message ?: "Payment failed"
                    )
                }
            } finally {
                _loading.value = false
            }
        }
    }

    fun initiatePesapalPayment(sessionId: String, tenantId: Int, planId: Int, amount: Double) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.processPesapalPayment(sessionId, tenantId, planId, amount)
                result.onSuccess { response ->
                    _pesapalResponse.value = response
                }.onFailure { error ->
                    _pesapalResponse.value = PesapalPaymentResponse(
                        success = false,
                        message = error.message ?: "Payment failed"
                    )
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

                result.onSuccess { statusResponse ->
                    when (statusResponse.status) {
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

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}