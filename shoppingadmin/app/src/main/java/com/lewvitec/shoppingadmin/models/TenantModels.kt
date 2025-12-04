package com.lewvitec.shoppingadmin.models

import com.google.gson.annotations.SerializedName

// -----------------------
// TENANT REGISTRATION
// -----------------------

data class RegisterTenantRequest(
    @SerializedName("business_name") val businessName: String,
    @SerializedName("subdomain") val subdomain: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,   // user-chosen password
    @SerializedName("plan_id") val planId: Int = 1      // default plan
)

data class RegisterTenantResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("tenant_id") val tenantId: Int? = null,
    @SerializedName("user_id") val userId: Int? = null,
    @SerializedName("subdomain") val subdomain: String? = null,
    @SerializedName("email") val email: String? = null
)

// -----------------------
// SUBDOMAIN CHECK
// -----------------------

data class CheckSubdomainRequest(
    @SerializedName("subdomain") val subdomain: String
)

data class SubdomainCheckResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("isAvailable") val isAvailable: Boolean
)
