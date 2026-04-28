package com.formbuddy.android.data.review

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.review.ReviewManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private val Context.reviewStore by preferencesDataStore("in_app_review")

/**
 * Mirrors iOS `requestReview` — asks Google Play to show the in-app review
 * sheet at most once after the user has had two delightful moments
 * (first save + N saves) and never again. Uses an internal counter so we
 * don't pester anyone.
 *
 * Play returns no result either way (by design — anti-spam) so we just mark
 * the request as fulfilled and move on.
 */
@Singleton
class InAppReviewManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val manager: ReviewManager by lazy { ReviewManagerFactory.create(context) }

    /** Call after a successful save — only triggers the dialog when criteria met. */
    suspend fun maybeRequestReview(activity: Activity) {
        val asked = context.reviewStore.data.first()[KEY_ASKED] ?: false
        if (asked) return
        val saves = (context.reviewStore.data.first()[KEY_SAVE_COUNT] ?: 0) + 1
        context.reviewStore.edit { it[KEY_SAVE_COUNT] = saves }
        if (saves < TRIGGER_AFTER_SAVES) return

        try {
            val info = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, info).await()
            context.reviewStore.edit { it[KEY_ASKED] = true }
        } catch (_: Throwable) {
            // Don't retry — Play deliberately gives no signal on success/failure.
        }
    }

    companion object {
        private val KEY_ASKED = booleanPreferencesKey("asked")
        private val KEY_SAVE_COUNT = intPreferencesKey("save_count")
        private const val TRIGGER_AFTER_SAVES = 3
    }
}
