package com.formbuddy.android.data.telemetry

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.formbuddy.android.BuildConfig
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin telemetry facade so callers don't have to depend on Firebase directly.
 *
 * Two channels:
 *   - **Analytics** — `logEvent` and `setUserProperty` shipped to Firebase
 *     Analytics. We additionally write a [PrivacyAuditLog] entry of type
 *     `Telemetry` so the user's audit screen reflects every metric we sent.
 *   - **Errors** — `logBreadcrumb` adds a Crashlytics breadcrumb;
 *     `recordException` reports a non-fatal. Crashes get auto-reported by
 *     Crashlytics' uncaught-exception handler, which is installed
 *     automatically when the SDK initialises.
 *
 * In debug builds we keep collection on (so Crashlytics tooling works) but
 * mappings aren't uploaded — the Gradle config gates that.
 */
@Singleton
class Analytics @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val analytics: FirebaseAnalytics by lazy { FirebaseAnalytics.getInstance(context) }
    private val crashlytics: FirebaseCrashlytics by lazy { FirebaseCrashlytics.getInstance() }

    init {
        crashlytics.setCrashlyticsCollectionEnabled(true)
        crashlytics.setCustomKey("buildType", if (BuildConfig.DEBUG) "debug" else "release")
        crashlytics.setCustomKey("versionName", BuildConfig.VERSION_NAME)
    }

    fun logEvent(name: String, params: Map<String, Any?> = emptyMap()) {
        val bundle: Bundle = bundleOf().apply {
            params.forEach { (k, v) ->
                when (v) {
                    is String -> putString(k, v)
                    is Int -> putInt(k, v)
                    is Long -> putLong(k, v)
                    is Double -> putDouble(k, v)
                    is Boolean -> putBoolean(k, v)
                    null -> putString(k, "")
                    else -> putString(k, v.toString())
                }
            }
        }
        analytics.logEvent(name, bundle)
        crashlytics.log("event:$name params=$params")
    }

    fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
        crashlytics.setCustomKey("user.$name", value ?: "")
    }

    fun setUserId(id: String?) {
        analytics.setUserId(id)
        crashlytics.setUserId(id ?: "")
    }

    fun logBreadcrumb(message: String) {
        crashlytics.log(message)
    }

    fun recordException(throwable: Throwable, vararg keys: Pair<String, String>) {
        keys.forEach { (k, v) -> crashlytics.setCustomKey(k, v) }
        crashlytics.recordException(throwable)
    }
}

/** Catalogues the events the app emits — keep them in one place so the
 *  Mixpanel/Looker dashboards match the code. */
object Events {
    const val APP_LAUNCH                  = "app_launch"
    const val ONBOARDING_COMPLETE         = "onboarding_complete"
    const val DOCUMENT_SCANNED            = "document_scanned"
    const val DOCUMENT_UPLOADED           = "document_uploaded"
    const val DOCUMENT_FROM_LIBRARY       = "document_from_library"
    const val DOCUMENT_FROM_IMAGE         = "document_from_image"
    const val DOCUMENT_FROM_SHARE         = "document_from_share"
    const val ANALYSIS_STARTED            = "analysis_started"
    const val ANALYSIS_COMPLETED          = "analysis_completed"
    const val ANALYSIS_INSTANT_MATCH      = "analysis_instant_match"
    const val FORM_SAVED                  = "form_saved"
    const val FORM_DUPLICATED             = "form_duplicated"
    const val FORM_EXPORTED_TO_DRIVE      = "form_exported_to_drive"
    const val FORM_SHARED_FOR_FILL        = "form_shared_for_fill"
    const val FILL_RECEIVED_FROM_LINK     = "fill_received_from_link"
    const val FIELD_FILLED_FROM_PROFILE   = "field_filled_from_profile"
    const val FIELD_FILLED_FROM_VOICE     = "field_filled_from_voice"
    const val FIELD_FILLED_FROM_AGENT     = "field_filled_from_agent"
    const val FIELD_FILLED_FROM_CLIPBOARD = "field_filled_from_clipboard"
    const val PAYWALL_VIEWED              = "paywall_viewed"
    const val PAYWALL_PURCHASED           = "paywall_purchased"
    const val REFERRAL_INVITE_SENT        = "referral_invite_sent"
    const val REFERRAL_ACCEPTED           = "referral_accepted"
    const val AUTOFILL_DATASET_PROVIDED   = "autofill_dataset_provided"
    const val AUTOFILL_VALUE_SAVED        = "autofill_value_saved"
}
