package com.formbuddy.android.autofill

import android.app.assist.AssistStructure
import android.os.Build
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.util.Log
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import com.formbuddy.android.R
import com.formbuddy.android.data.local.db.entity.ProfileEntity
import com.formbuddy.android.data.repository.ProfileRepository
import com.formbuddy.android.data.telemetry.Analytics
import com.formbuddy.android.data.telemetry.Events
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android system-wide autofill bridge.
 *
 * When the user taps a text field anywhere on the device — banking apps,
 * tax software, web views, system Settings — the OS asks the registered
 * `AutofillService` for suggestions. We hand back a [Dataset] populated
 * from the user's saved [ProfileEntity], so the same profile data the user
 * configured inside FormBuddy fills *every* form on the phone.
 *
 * No competitor in the form-filling space registers as an AutofillService;
 * this is the single biggest distribution moat we can ship without an ASO
 * change. To enable, the user goes to
 * Settings → System → Languages & input → Autofill service → FormBuddy.
 *
 * Mirrors the `Profile.suggestedValue(field)` semantics from the iOS
 * profile autofill so a field that fills inside FormBuddy fills the same
 * way at the OS level.
 */
@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint
class FormBuddyAutofillService : AutofillService() {

    @Inject lateinit var profileRepository: ProfileRepository
    @Inject lateinit var analytics: Analytics

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
            ?: return callback.onSuccess(null)
        scope.launch {
            try {
                val wrapper = profileRepository.getAllProfiles().first().firstOrNull()
                val profile: ProfileEntity? = (wrapper as? com.formbuddy.android.data.model.Profile.Personal)?.entity
                    ?: (wrapper as? com.formbuddy.android.data.model.Profile.Family)?.entity
                if (profile == null) {
                    callback.onSuccess(null)
                    return@launch
                }
                val candidates = collectFillCandidates(structure)
                if (candidates.isEmpty()) {
                    callback.onSuccess(null)
                    return@launch
                }
                val response = buildResponse(profile, candidates)
                analytics.logEvent(
                    Events.AUTOFILL_DATASET_PROVIDED,
                    mapOf("fields" to candidates.size, "package" to (structure.activityComponent?.packageName ?: ""))
                )
                callback.onSuccess(response)
            } catch (t: Throwable) {
                Log.w(TAG, "onFillRequest failed", t)
                callback.onSuccess(null) // Surface gracefully — never crash the host app.
            }
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // We're a one-way provider for now: we read profile data, we don't
        // persist new values discovered in third-party apps. Acknowledge.
        analytics.logEvent(Events.AUTOFILL_VALUE_SAVED)
        callback.onSuccess()
    }

    private fun collectFillCandidates(structure: AssistStructure): List<FillCandidate> {
        val out = mutableListOf<FillCandidate>()
        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode, out)
        }
        return out
    }

    private fun walk(node: AssistStructure.ViewNode, out: MutableList<FillCandidate>) {
        val hint = AutofillFieldClassifier.classify(node)
        val id = node.autofillId
        if (hint != null && id != null) {
            out += FillCandidate(id, hint, node.text?.toString())
        }
        for (i in 0 until node.childCount) {
            walk(node.getChildAt(i), out)
        }
    }

    private fun buildResponse(profile: ProfileEntity, candidates: List<FillCandidate>): FillResponse {
        val builder = FillResponse.Builder()
        // We register a single dataset for the whole profile so the user gets
        // one tap that fills every recognized field on the screen at once.
        val dataset = Dataset.Builder()
        var added = false
        for (c in candidates) {
            val value = profile.value(c.hint) ?: continue
            val presentation = RemoteViews(packageName, R.layout.autofill_dataset_row).apply {
                setTextViewText(R.id.autofill_label, "FormBuddy: ${c.hint.displayName}")
                setTextViewText(R.id.autofill_value, value)
            }
            dataset.setValue(c.id, AutofillValue.forText(value), presentation)
            added = true
        }
        if (added) builder.addDataset(dataset.build())

        // Tell the OS we want a save callback for the contact / postal hints
        // so we don't break the system save dialog on third-party apps.
        val saveTypes = SaveInfo.SAVE_DATA_TYPE_GENERIC or
            SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS or
            SaveInfo.SAVE_DATA_TYPE_USERNAME
        val saveIds = candidates.map { it.id }.toTypedArray()
        if (saveIds.isNotEmpty()) {
            builder.setSaveInfo(SaveInfo.Builder(saveTypes, saveIds).build())
        }
        return builder.build()
    }

    private data class FillCandidate(
        val id: AutofillId,
        val hint: AutofillFieldClassifier.Hint,
        val currentText: String?
    )

    /** Maps `ProfileEntity` to a value for a classified hint. */
    private fun ProfileEntity.value(hint: AutofillFieldClassifier.Hint): String? = when (hint) {
        AutofillFieldClassifier.Hint.GIVEN_NAME -> firstName.takeIf { it.isNotBlank() }
        AutofillFieldClassifier.Hint.MIDDLE_NAME -> middleName?.takeIf { it.isNotBlank() }
        AutofillFieldClassifier.Hint.FAMILY_NAME -> lastName.takeIf { it.isNotBlank() }
        AutofillFieldClassifier.Hint.FULL_NAME -> listOfNotNull(
            firstName.takeIf { it.isNotBlank() },
            middleName?.takeIf { it.isNotBlank() },
            lastName.takeIf { it.isNotBlank() }
        ).joinToString(" ").ifBlank { null }
        AutofillFieldClassifier.Hint.EMAIL -> email
        AutofillFieldClassifier.Hint.PHONE -> phone
        AutofillFieldClassifier.Hint.BIRTH_DATE -> birthDate
        AutofillFieldClassifier.Hint.STREET_ADDRESS -> homeAddress
        AutofillFieldClassifier.Hint.CITY -> city
        AutofillFieldClassifier.Hint.REGION -> state
        AutofillFieldClassifier.Hint.COUNTRY -> country
        AutofillFieldClassifier.Hint.POSTAL_CODE -> postalCode
    }

    companion object { private const val TAG = "FillinAutofill" }
}
