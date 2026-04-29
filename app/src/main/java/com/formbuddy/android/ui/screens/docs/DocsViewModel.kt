package com.formbuddy.android.ui.screens.docs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formbuddy.android.data.local.db.entity.FormReferenceEntity
import com.formbuddy.android.data.metrics.TimeSavedTracker
import com.formbuddy.android.data.repository.FormRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DocsViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val shareForFillService: com.formbuddy.android.data.share.ShareForFillService,
    timeSavedTracker: TimeSavedTracker
) : ViewModel() {

    val forms: StateFlow<List<FormReferenceEntity>> = formRepository.getAllForms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Cumulative seconds the user has saved using FormBuddy. Surfaces on the
     *  home screen as a single human-readable banner. */
    val timeSavedSeconds: StateFlow<Long> = timeSavedTracker.totalSavedSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun deleteForm(id: String) {
        viewModelScope.launch {
            formRepository.deleteForm(id)
        }
    }

    /** "Fill the same form again." Creates a new form entity reusing the
     *  source PDF bytes but with every field cleared. */
    fun duplicateForm(id: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            onDone(formRepository.duplicateForm(id))
        }
    }

    /** "Send to a friend to fill." Publishes the form template (no values)
     *  to Firestore and returns a shareable link via [onUrl]. */
    fun shareForFill(formId: String, onUrl: (String?) -> Unit) {
        viewModelScope.launch { onUrl(shareForFillService.publish(formId)) }
    }
}
