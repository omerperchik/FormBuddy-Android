package com.formbuddy.android.ui.screens.paywall

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.billing.BillingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billingManager: BillingManager
) : ViewModel() {

    val offers: StateFlow<List<BillingManager.PlanOffer>> = billingManager.offers
    val isPro: StateFlow<Boolean> = billingManager.isPro
    val purchaseResult: StateFlow<BillingManager.PurchaseResult?> = billingManager.lastPurchaseResult

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        billingManager.start()
        viewModelScope.launch { billingManager.refresh() }
    }

    fun purchase(activity: Activity, plan: BillingManager.Plan) {
        _isLoading.value = true
        billingManager.launchPurchase(activity, plan)
        // The result will arrive via [purchaseResult].
        _isLoading.value = false
    }

    fun restore() {
        viewModelScope.launch { billingManager.refresh() }
    }
}
