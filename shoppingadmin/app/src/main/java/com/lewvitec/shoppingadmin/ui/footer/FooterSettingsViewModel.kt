package com.lewvitec.shoppingadmin.ui.footer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lewvitec.shoppingadmin.models.TenantFooter
import com.lewvitec.shoppingadmin.repository.AdminRepository
import com.lewvitec.shoppingadmin.repository.Result
import com.lewvitec.shoppingadmin.utils.TenantManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FooterSettingsViewModel @Inject constructor(
    private val repository: AdminRepository,
    private val tenantManager: TenantManager
) : ViewModel() {

    private val _footerData = MutableLiveData<TenantFooter?>(null)
    val footerData: LiveData<TenantFooter?> = _footerData

    private val _loading = MutableLiveData<Boolean>(false)
    val loading: LiveData<Boolean> = _loading

    private val _saveResult = MutableLiveData<kotlin.Result<Unit>?>(null)
    val saveResult: LiveData<kotlin.Result<Unit>?> = _saveResult

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    fun loadFooterSettings(sessionId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val tenantId = tenantManager.getCurrentTenantId()

                if (tenantId <= 0) {
                    _error.value = "No tenant configured"
                    _loading.value = false
                    return@launch
                }

                val result = repository.getTenantFooter(sessionId, tenantId)

                when (result) {
                    is Result.Success -> {
                        _footerData.value = result.data
                    }
                    is Result.Failure -> {
                        // If no footer exists yet, create an empty one
                        if (result.exception.message?.contains("No footer found") == true) {
                            _footerData.value = TenantFooter(
                                tenantId = tenantId,
                                monFri = "08:00 To 19:00",
                                saturday = "09:00 To 19:00",
                                sunday = "Closed"
                            )
                        } else {
                            _error.value = "Failed to load footer: ${result.exception.message}"
                        }
                    }
                    Result.Loading -> {
                        // Loading state handled by _loading
                    }
                }
            } catch (e: Exception) {
                _error.value = "Error loading footer: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveFooterSettings(
        sessionId: String,
        businessName: String,
        aboutText: String,
        address: String,
        phone1: String,
        phone2: String,
        email: String,
        monFri: String,
        saturday: String,
        sunday: String,
        facebook: String,
        twitter: String,
        instagram: String,
        linkedin: String,
        pinterest: String
    ) {
        viewModelScope.launch {
            _loading.value = true
            _saveResult.value = null

            try {
                val tenantId = tenantManager.getCurrentTenantId()

                if (tenantId <= 0) {
                    _saveResult.value = kotlin.Result.failure(Exception("No tenant configured"))
                    _loading.value = false
                    return@launch
                }

                val result = repository.saveTenantFooter(
                    sessionId = sessionId,
                    tenantId = tenantId,
                    businessName = businessName,
                    aboutText = aboutText,
                    address = address,
                    phone1 = phone1,
                    phone2 = phone2,
                    email = email,
                    monFri = monFri,
                    saturday = saturday,
                    sunday = sunday,
                    facebook = facebook,
                    twitter = twitter,
                    instagram = instagram,
                    linkedin = linkedin,
                    pinterest = pinterest
                )

                when (result) {
                    is Result.Success -> {
                        _saveResult.value = kotlin.Result.success(Unit)
                        // Update the local data
                        _footerData.value = TenantFooter(
                            tenantId = tenantId,
                            businessName = businessName,
                            aboutText = aboutText,
                            address = address,
                            phone1 = phone1,
                            phone2 = phone2,
                            email = email,
                            monFri = monFri,
                            saturday = saturday,
                            sunday = sunday,
                            facebook = facebook,
                            twitter = twitter,
                            instagram = instagram,
                            linkedin = linkedin,
                            pinterest = pinterest
                        )
                    }
                    is Result.Failure -> {
                        _saveResult.value = kotlin.Result.failure(result.exception)
                    }
                    Result.Loading -> {
                        // Loading state handled by _loading
                    }
                }
            } catch (e: Exception) {
                _saveResult.value = kotlin.Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSaveResult() {
        _saveResult.value = null
    }
}