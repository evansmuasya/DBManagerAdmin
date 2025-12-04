package com.lewvitec.shoppingadmin.models

import com.google.gson.annotations.SerializedName

// Payment History Models
data class PaymentHistoryResponse(
    val success: Boolean,
    val message: String,
    val transactions: List<PaymentTransaction>,
    val total: Int,
    val page: Int,
    val totalPages: Int
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
data class PaymentStatusRequest(
    @SerializedName("checkout_request_id")
    val checkoutRequestId: String
)

data class PaymentStatusResponse(
    val success: Boolean,
    val message: String,
    val status: String? = null, // pending, completed, failed
    @SerializedName("transaction_id")
    val transactionId: String? = null
)