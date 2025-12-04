package com.lewvitec.shoppingadmin.ui.subscription

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewvitec.shoppingadmin.models.SubscriptionPlan
import com.lewvitec.shoppingadmin.models.TenantSubscription
import com.lewvitec.shoppingadmin.repository.AdminRepository
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val tenantManager: TenantManager
) : ViewModel() {

    private val _plans = MutableLiveData<List<SubscriptionPlan>>()
    val plans: LiveData<List<SubscriptionPlan>> = _plans

    private val _currentSubscription = MutableLiveData<TenantSubscription?>()
    val currentSubscription: LiveData<TenantSubscription?> = _currentSubscription

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadSubscriptionPlans(sessionId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val plansResult = repository.getSubscriptionPlans(sessionId)
                plansResult.onSuccess { plansList ->
                    _plans.value = plansList
                }.onFailure { throwable ->
                    _error.value = "Failed to load plans: ${throwable.message}"
                }

                // Load current subscription
                val tenantId = tenantManager.getCurrentTenantId()
                if (tenantId > 0) {
                    val subscriptionResult = repository.getTenantSubscription(sessionId, tenantId)
                    subscriptionResult.onSuccess { subscription ->
                        _currentSubscription.value = subscription
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}