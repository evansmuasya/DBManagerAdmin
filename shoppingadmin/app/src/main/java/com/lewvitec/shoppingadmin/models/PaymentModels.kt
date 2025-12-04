package com.lewvitec.shoppingadmin.models

import com.google.gson.annotations.SerializedName


// Payment Status Models
data class PaymentStatusRequest(
    @SerializedName("checkout_request_id")
    val checkoutRequestId: String
)

