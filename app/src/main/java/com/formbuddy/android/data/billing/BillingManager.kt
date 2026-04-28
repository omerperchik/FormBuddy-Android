package com.formbuddy.android.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

/**
 * Wraps Google Play Billing v7 to handle FormBuddy Pro subscriptions.
 *
 * Mirrors the iOS StoreKit flow used by `AppStoreManager`:
 *   - Queries the three subscription products (weekly / monthly / yearly).
 *   - Launches the Play purchase sheet for a selected product.
 *   - Acknowledges new purchases and surfaces an `isPro` state flow that the rest of
 *     the app reads to gate Pro features (Family/Business profiles, voice, agent,
 *     biometric lock).
 *
 * Product IDs are kept in sync with the App Store identifiers from iOS so server-side
 * receipt validation (Firebase Functions) can treat them uniformly.
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PurchasesUpdatedListener, BillingClientStateListener {

    object ProductIds {
        const val WEEKLY = "formbuddy.pro.weekly"
        const val MONTHLY = "formbuddy.pro.monthly"
        const val YEARLY = "formbuddy.pro.yearly"
        val ALL = listOf(WEEKLY, MONTHLY, YEARLY)
    }

    enum class Plan(val productId: String) {
        WEEKLY(ProductIds.WEEKLY),
        MONTHLY(ProductIds.MONTHLY),
        YEARLY(ProductIds.YEARLY)
    }

    data class PlanOffer(
        val plan: Plan,
        val productId: String,
        val title: String,
        val formattedPrice: String,
        val priceMicros: Long,
        val currencyCode: String,
        val billingPeriod: String,
        val productDetails: ProductDetails,
        val offerToken: String
    )

    sealed interface PurchaseResult {
        data object Pending : PurchaseResult
        data class Success(val purchase: Purchase) : PurchaseResult
        data class UserCancelled(val message: String?) : PurchaseResult
        data class Failure(val code: Int, val message: String?) : PurchaseResult
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .setListener(this)
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _offers = MutableStateFlow<List<PlanOffer>>(emptyList())
    val offers: StateFlow<List<PlanOffer>> = _offers.asStateFlow()

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _lastPurchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val lastPurchaseResult: StateFlow<PurchaseResult?> = _lastPurchaseResult.asStateFlow()

    private var retryAttempt = 0

    enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED }

    fun start() {
        if (_connectionState.value == ConnectionState.CONNECTED) return
        _connectionState.value = ConnectionState.CONNECTING
        client.startConnection(this)
    }

    fun release() {
        client.endConnection()
        _connectionState.value = ConnectionState.DISCONNECTED
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _connectionState.value = ConnectionState.CONNECTED
            retryAttempt = 0
            scope.launch { refresh() }
        } else {
            Log.w(TAG, "Setup failed: code=${billingResult.responseCode} msg=${billingResult.debugMessage}")
            _connectionState.value = ConnectionState.DISCONNECTED
            retryWithBackoff()
        }
    }

    override fun onBillingServiceDisconnected() {
        _connectionState.value = ConnectionState.DISCONNECTED
        retryWithBackoff()
    }

    private fun retryWithBackoff() {
        retryAttempt = min(retryAttempt + 1, MAX_RETRY)
        val delayMs = (BASE_BACKOFF_MS * 2.0.pow(retryAttempt - 1)).toLong()
        scope.launch {
            kotlinx.coroutines.delay(delayMs)
            start()
        }
    }

    /** Re-query Play for offers and restore active subscriptions. */
    suspend fun refresh() {
        if (_connectionState.value != ConnectionState.CONNECTED) return
        queryOffers()
        restoreEntitlement()
    }

    private suspend fun queryOffers() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                ProductIds.ALL.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
            return
        }
        val list = result.productDetailsList.orEmpty()
        _offers.value = list.mapNotNull { details ->
            val plan = Plan.entries.firstOrNull { it.productId == details.productId } ?: return@mapNotNull null
            // Pick the first available subscription offer + its first base pricing phase.
            val subOffer = details.subscriptionOfferDetails?.firstOrNull() ?: return@mapNotNull null
            val phase = subOffer.pricingPhases.pricingPhaseList.firstOrNull() ?: return@mapNotNull null
            PlanOffer(
                plan = plan,
                productId = details.productId,
                title = details.title,
                formattedPrice = phase.formattedPrice,
                priceMicros = phase.priceAmountMicros,
                currencyCode = phase.priceCurrencyCode,
                billingPeriod = phase.billingPeriod,
                productDetails = details,
                offerToken = subOffer.offerToken
            )
        }
    }

    private suspend fun restoreEntitlement() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return
        applyPurchases(result.purchasesList)
    }

    /** Caller passes the foreground [Activity] required by [BillingClient.launchBillingFlow]. */
    fun launchPurchase(activity: Activity, plan: Plan): BillingResult {
        val offer = _offers.value.firstOrNull { it.plan == plan }
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE)
                .setDebugMessage("Plan ${plan.name} is not loaded yet")
                .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(offer.productDetails)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .build()
        return client.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.let { applyPurchases(it) }
                purchases?.firstOrNull()?.let {
                    _lastPurchaseResult.value = PurchaseResult.Success(it)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                _lastPurchaseResult.value = PurchaseResult.UserCancelled(billingResult.debugMessage)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Treat as success — refresh entitlement.
                scope.launch { restoreEntitlement() }
            }
            else -> _lastPurchaseResult.value =
                PurchaseResult.Failure(billingResult.responseCode, billingResult.debugMessage)
        }
    }

    private fun applyPurchases(purchases: List<Purchase>) {
        val active = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                it.products.any { id -> id in ProductIds.ALL }
        }
        _isPro.value = active.isNotEmpty()

        // Acknowledge any unacknowledged subscription so Play doesn't auto-refund.
        for (purchase in active) {
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                client.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "ack failed: ${result.debugMessage}")
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "BillingManager"
        private const val MAX_RETRY = 5
        private const val BASE_BACKOFF_MS = 1_000L
    }
}
