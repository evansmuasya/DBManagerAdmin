package com.lewvitec.shoppingadmin.ui.subscription

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewvitec.shoppingadmin.models.SubscriptionPlan
import com.lewvitec.shoppingadmin.models.TenantSubscription
import com.lewvitec.shoppingadmin.repository.AdminRepository
import com.lewvitec.shoppingadmin.repository.Result
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val tenantManager: TenantManager
) : ViewModel() {

    private val _plans = MutableLiveData<List<SubscriptionPlan>?>(null)
    val plans: LiveData<List<SubscriptionPlan>?> = _plans

    private val _currentSubscription = MutableLiveData<TenantSubscription?>(null)
    val currentSubscription: LiveData<TenantSubscription?> = _currentSubscription

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadSubscriptionPlans(sessionId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                // Load subscription plans
                val plansResult = repository.getSubscriptionPlans(sessionId)

                when (plansResult) {
                    is Result.Success -> {
                        _plans.value = plansResult.data ?: emptyList()
                    }
                    is Result.Failure -> {
                        _error.value = "Failed to load plans: ${plansResult.exception.message}"
                        _plans.value = emptyList()
                    }
                    Result.Loading -> {
                        // Handle loading state if needed
                    }
                }

                // Load current subscription
                val tenantId = tenantManager.getCurrentTenantId()
                if (tenantId > 0) {
                    val subscriptionResult = repository.getTenantSubscription(sessionId, tenantId)

                    when (subscriptionResult) {
                        is Result.Success -> {
                            _currentSubscription.value = subscriptionResult.data
                        }
                        is Result.Failure -> {
                            // It's okay if no subscription exists
                            _currentSubscription.value = null
                        }
                        Result.Loading -> {
                            // Handle loading state if needed
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                _plans.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}