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

data class PaymentHistoryResponse(
    val success: Boolean,
    val message: String,
    val transactions: List<PaymentTransaction> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerializedName("total_pages")
    val totalPages: Int = 1
)

// Payment Method Models
data class PaymentMethod(
    val id: Int,
    val name: String,
    @SerializedName("display_name")
    val displayName: String,
    @SerializedName("icon_url")
    val iconUrl: String?,
    @SerializedName("is_active")
    val isActive: Boolean
)

data class PaymentMethodsResponse(
    val success: Boolean,
    val message: String,
    val methods: List<PaymentMethod>
)

// M-Pesa Models
data class MpesaPaymentRequest(
    @SerializedName("phone_number")
    val phoneNumber: String,
    val amount: Double,
    @SerializedName("account_reference")
    val accountReference: String = "",
    @SerializedName("transaction_description")
    val transactionDescription: String = ""
)

data class MpesaPaymentResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("checkout_request_id")
    val checkoutRequestId: String? = null,
    @SerializedName("merchant_request_id")
    val merchantRequestId: String? = null,
    @SerializedName("response_code")
    val responseCode: String? = null,
    @SerializedName("response_description")
    val responseDescription: String? = null,
    @SerializedName("customer_message")
    val customerMessage: String? = null
)

// Pesapal Models
data class PesapalPaymentRequest(
    val amount: Double,
    val description: String,
    val email: String,
    @SerializedName("phone_number")
    val phoneNumber: String,
    @SerializedName("first_name")
    val firstName: String,
    @SerializedName("last_name")
    val lastName: String
)

data class PesapalPaymentResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("order_id")
    val orderId: String? = null,
    @SerializedName("order_tracking_id")
    val orderTrackingId: String? = null,
    @SerializedName("redirect_url")
    val redirectUrl: String? = null
)

// Payment Status Models
data class PaymentStatusResponse(
    val success: Boolean,
    val message: String,
    val status: String? = null, // pending, completed, failed
    @SerializedName("transaction_id")
    val transactionId: String? = null
)