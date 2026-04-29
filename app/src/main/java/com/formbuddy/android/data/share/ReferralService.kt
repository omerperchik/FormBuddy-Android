package com.formbuddy.android.data.share

import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Invite a friend → both get 30 days Pro."
 *
 * The local app generates the link from the user's anonymous Firebase auth
 * UID. When the recipient opens it, the app calls a Firebase Functions
 * callable (`claimReferral`) which:
 *   - Idempotently grants the recipient 30 days Pro entitlement.
 *   - Once the recipient buys their first paid sub OR keeps the app
 *     installed for 7 days, grants the referrer the same 30 days.
 *
 * Both grants live in `users/{uid}.entitlements` in Firestore and are
 * mirrored into the local Pro state by [com.formbuddy.android.data.billing.BillingManager].
 *
 * The whole flow lives behind one callable so the client doesn't need to
 * trust itself with entitlement writes.
 */
@Singleton
class ReferralService @Inject constructor(
    private val auditLog: PrivacyAuditLog
) {
    private val functions: FirebaseFunctions by lazy { FirebaseFunctions.getInstance("us-central1") }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /** Builds a shareable link for the current user's UID. */
    suspend fun buildInviteUrl(planCode: String = "30d"): String {
        val uid = currentUid() ?: error("User not signed in")
        return DeepLink.referralUrl(uid, planCode)
    }

    /** Called by the app when the user taps a `formbuddy.app/invite` link. */
    suspend fun claimReferral(referrerId: String, planCode: String): Result<Unit> {
        val uid = currentUid() ?: return Result.failure(IllegalStateException("Not signed in"))
        if (uid == referrerId) {
            // Self-invite — silently ignore so we don't grant Pro to the sender.
            return Result.success(Unit)
        }
        return runCatching {
            auditLog.log(
                PrivacyAuditLog.Category.Cloud,
                destination = "fn:claimReferral",
                description = "Claiming referral from $referrerId (plan=$planCode)"
            )
            functions.getHttpsCallable("claimReferral")
                .call(mapOf("referrerId" to referrerId, "plan" to planCode))
                .await()
            Unit
        }
    }

    private suspend fun currentUid(): String? {
        if (auth.currentUser != null) return auth.currentUser?.uid
        return runCatching { auth.signInAnonymously().await().user?.uid }.getOrNull()
    }
}
