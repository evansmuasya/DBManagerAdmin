package com.lewvitec.shoppingadmin.models

data class TenantFooter(
    val id: Int = 0,
    val tenantId: Int = 0,

    val businessName: String? = null,
    val aboutText: String? = null,

    val address: String? = null,
    val phone1: String? = null,
    val phone2: String? = null,
    val email: String? = null,

    val monFri: String? = "08:00 To 19:00",
    val saturday: String? = "09:00 To 19:00",
    val sunday: String? = "Closed",

    val facebook: String? = null,
    val twitter: String? = null,
    val instagram: String? = null,
    val linkedin: String? = null,
    val pinterest: String? = null
)