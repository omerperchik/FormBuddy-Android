package com.formbuddy.android.data.share

import android.net.Uri

/**
 * Single source of truth for FormBuddy deep links.
 *
 * Two link types:
 *   - **Referral** — `https://formbuddy.app/invite?ref=USER&plan=30d`
 *     Recipient gets a 30-day Pro grant; sender gets the same when the
 *     recipient activates. Backed by a Cloud Function (see
 *     `functions/index.js#claimReferral`).
 *   - **Share-for-fill** — `https://formbuddy.app/fill?token=BASE64_PAYLOAD`
 *     Recipient opens the same form on their phone, fills with their
 *     profile, signs, and sends back. The token encodes the form id and
 *     a one-time download key.
 *
 * Both schemes share the host so a single intent-filter in the manifest
 * routes them — the path discriminates.
 */
object DeepLink {

    const val HOST = "formbuddy.app"
    const val SCHEME_HTTPS = "https"
    const val SCHEME_APP = "formbuddy"
    const val PATH_INVITE = "invite"
    const val PATH_FILL = "fill"

    sealed interface Parsed {
        data class Referral(val referrerId: String, val planCode: String) : Parsed
        data class ShareForFill(val token: String) : Parsed
        data object None : Parsed
    }

    fun parse(uri: Uri?): Parsed {
        uri ?: return Parsed.None
        if (uri.host != HOST && uri.scheme != SCHEME_APP) return Parsed.None
        val path = uri.lastPathSegment ?: return Parsed.None
        return when (path) {
            PATH_INVITE -> {
                val ref = uri.getQueryParameter("ref") ?: return Parsed.None
                val plan = uri.getQueryParameter("plan") ?: "30d"
                Parsed.Referral(ref, plan)
            }
            PATH_FILL -> {
                val token = uri.getQueryParameter("token") ?: return Parsed.None
                Parsed.ShareForFill(token)
            }
            else -> Parsed.None
        }
    }

    fun referralUrl(referrerId: String, planCode: String = "30d"): String =
        "https://$HOST/$PATH_INVITE?ref=$referrerId&plan=$planCode"

    fun shareForFillUrl(token: String): String =
        "https://$HOST/$PATH_FILL?token=$token"
}
