package com.formbuddy.android.ui.screens.paywall

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor() : ViewModel() {

    fun purchase(planIndex: Int) {
        // TODO: Integrate Google Play Billing
        // planIndex: 0 = weekly, 1 = monthly, 2 = yearly
    }

    fun restorePurchases() {
        // TODO: Restore Google Play purchases
    }
}
