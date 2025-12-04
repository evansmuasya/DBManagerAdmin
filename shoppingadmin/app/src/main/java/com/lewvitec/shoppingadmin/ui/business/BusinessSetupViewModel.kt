package com.lewvitec.shoppingadmin.ui.business
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewvitec.shoppingadmin.models.RegisterTenantRequest
import com.lewvitec.shoppingadmin.models.RegisterTenantResponse
import com.lewvitec.shoppingadmin.network.ApiService
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class BusinessSetupViewModel @Inject constructor(
    private val apiService: ApiService,
    private val tenantManager: TenantManager
) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _storeCreated = MutableLiveData<Boolean>()
    val storeCreated: LiveData<Boolean> = _storeCreated

    var createdSubdomain: String = ""

    fun createStore(businessName: String, subdomain: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val email = "$subdomain@lewvitec.co.ke"

                val request = RegisterTenantRequest(
                    businessName = businessName,
                    subdomain = subdomain,
                    email = email,
                    password = password,
                    planId = 1
                )

                val response: Response<RegisterTenantResponse> = apiService.registerTenant(request)

                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    if (registerResponse != null && registerResponse.success) {

                        registerResponse.tenantId?.let { tenantId ->
                            tenantManager.saveTenantInfo(
                                tenantId = tenantId,
                                subdomain = subdomain,
                                businessName = businessName
                            )

                            createdSubdomain = subdomain
                            _storeCreated.value = true
                        } ?: run {
                            _error.value = "Missing tenant ID in response"
                        }

                    } else {
                        _error.value = registerResponse?.message ?: "Failed to create store"
                    }
                } else {
                    _error.value = "Server error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Network error: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}
