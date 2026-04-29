package com.formbuddy.android.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.formbuddy.android.data.repository.FormRepository
import com.formbuddy.android.domain.analysis.FormAnalyzer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Mirrors iOS `BackgroundProcessingTaskCoordinator`: long-running document
 * analysis runs in WorkManager so the user can lock the phone and walk away
 * from the DMV waiting room. WiFi-preferred + retry-with-backoff because
 * scanned PDFs can take 30–90 s on the cloud pipeline.
 *
 * Inputs (workDataOf):
 *   - "uri" — content URI of the PDF/image
 *   - "expedited" — boolean, opt into Expedited (skips usage quota when on AC)
 *
 * Output:
 *   - "formId" — id of the saved form
 */
@HiltWorker
class DocumentProcessingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val formAnalyzer: FormAnalyzer,
    private val formRepository: FormRepository,
    private val auditLog: PrivacyAuditLog
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uriStr = inputData.getString(KEY_URI) ?: return Result.failure()
        return try {
            auditLog.log(
                PrivacyAuditLog.Category.OnDevice,
                destination = "background-worker",
                description = "Started analysis of uploaded document"
            )
            val uri = android.net.Uri.parse(uriStr)
            val bytes = applicationContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure()

            val template = formAnalyzer.analyzeDocument(bytes)
            val formId = formRepository.saveForm(template, bytes, existingId = null)
            Result.success(workDataOf(KEY_FORM_ID to formId))
        } catch (t: Throwable) {
            // Retry up to 3x with exponential backoff
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_URI = "uri"
        const val KEY_EXPEDITED = "expedited"
        const val KEY_FORM_ID = "formId"
        const val WORK_NAME_PREFIX = "doc-process-"
        private const val MAX_RETRIES = 3

        fun enqueue(context: Context, uri: android.net.Uri, expedited: Boolean = true): String {
            val name = WORK_NAME_PREFIX + uri.toString().hashCode()
            // Any-network: most users open a form on cellular; only fail when offline.
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<DocumentProcessingWorker>()
                .setInputData(Data.Builder().putString(KEY_URI, uri.toString()).build())
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .apply {
                    if (expedited) setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                }
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                name,
                // REPLACE so a re-open of the same URI always triggers a fresh run
                // instead of returning the previous (possibly failed) result.
                ExistingWorkPolicy.REPLACE,
                request
            )
            return name
        }
    }
}
