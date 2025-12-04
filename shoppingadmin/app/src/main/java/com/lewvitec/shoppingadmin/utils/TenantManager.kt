package com.lewvitec.shoppingadmin.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TenantManager @Inject constructor(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("tenant_prefs", Context.MODE_PRIVATE)

    fun saveTenantInfo(tenantId: Int, subdomain: String, businessName: String) {
        prefs.edit().apply {
            putInt("tenant_id", tenantId)
            putString("subdomain", subdomain)
            putString("business_name", businessName)
            putBoolean("is_tenant_setup", true)
            apply()
        }
    }

    fun getCurrentTenantId(): Int {
        return prefs.getInt("tenant_id", 0)
    }

    fun getCurrentSubdomain(): String {
        return prefs.getString("subdomain", "") ?: ""
    }

    fun getBusinessName(): String {
        return prefs.getString("business_name", "") ?: ""
    }

    fun isTenantSetup(): Boolean {
        return prefs.getBoolean("is_tenant_setup", false)
    }

    fun clearTenantInfo() {
        prefs.edit().clear().apply()
    }
}