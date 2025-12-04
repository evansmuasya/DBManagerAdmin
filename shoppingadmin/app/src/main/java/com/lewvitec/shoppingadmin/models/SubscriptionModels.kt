package com.lewvitec.shoppingadmin.models

import com.google.gson.annotations.SerializedName

data class SubscriptionPlan(
    val id: Int,
    val name: String,
    val description: String?,
    val price: Double,
    @SerializedName("duration_days")
    val durationDays: Int,
    val features: List<String>?,
    @SerializedName("is_active")
    val isActive: Boolean
)

data class SubscriptionPlansResponse(
    val success: Boolean,
    val message: String,
    val plans: List<SubscriptionPlan>
)

data class TenantSubscription(
    val id: Int,
    @SerializedName("tenant_id")
    val tenantId: Int,
    @SerializedName("plan_id")
    val planId: Int,
    @SerializedName("stripe_subscription_id")
    val stripeSubscriptionId: String? = null,
    @SerializedName("mpesa_checkout_request_id")
    val mpesaCheckoutRequestId: String? = null,
    @SerializedName("pesapal_transaction_id")
    val pesapalTransactionId: String? = null,
    val status: String,
    @SerializedName("current_period_start")
    val currentPeriodStart: String? = null,
    @SerializedName("current_period_end")
    val currentPeriodEnd: String? = null,
    @SerializedName("cancel_at_period_end")
    val cancelAtPeriodEnd: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String
)

data class SubscriptionResponse(
    val success: Boolean,
    val message: String,
    val subscription: TenantSubscription? = null
)

data class PaymentTransaction(
    val id: Int,
    @SerializedName("tenant_id")
    val tenantId: Int,
    @SerializedName("subscription_id")
    val subscriptionId: Int? = null,
    val amount: Double,
    val currency: String,
    @SerializedName("payment_method")
    val paymentMethod: String,
    @SerializedName("transaction_id")
    val transactionId: String? = null,
    val status: String,
    val description: String? = null,
    @SerializedName("created_at")
    val createdAt: String
)