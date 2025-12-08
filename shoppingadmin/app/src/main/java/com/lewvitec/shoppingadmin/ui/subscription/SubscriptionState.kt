package com.lewvitec.shoppingadmin.ui.subscription

sealed class SubscriptionState {
    data class Loading(val isLoading: Boolean) : SubscriptionState()
    data class Error(val message: String) : SubscriptionState()
    object Success : SubscriptionState()
}