package com.formbuddy.android.ui.screens.filling

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.formbuddy.android.data.local.preferences.PreferencesManager
import com.formbuddy.android.data.metrics.TimeSavedTracker
import com.formbuddy.android.data.model.ChatMessage
import com.formbuddy.android.data.model.ChatSession
import com.formbuddy.android.data.model.FormField
import com.formbuddy.android.data.model.FormMode
import com.formbuddy.android.data.model.FormTemplate
import com.formbuddy.android.data.model.Profile
import com.formbuddy.android.data.model.UserInputMethod
import com.formbuddy.android.data.privacy.PrivacyAuditLog
import com.formbuddy.android.data.repository.FormRepository
import com.formbuddy.android.data.repository.InstantMatchRepository
import com.formbuddy.android.data.repository.ProfileRepository
import com.formbuddy.android.data.review.InAppReviewManager
import com.formbuddy.android.data.telemetry.Analytics
import com.formbuddy.android.data.telemetry.Events
import com.formbuddy.android.data.work.DocumentProcessingWorker
import com.formbuddy.android.domain.analysis.FormAnalyzer
import com.formbuddy.android.domain.filling.ConversationManager
import com.formbuddy.android.domain.filling.ResponseClassifier
import com.formbuddy.android.domain.learning.ProfileLearner
import com.formbuddy.android.domain.speech.SpeechRecognitionService
import com.formbuddy.android.domain.tts.TextToSpeechService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FillingUiState {
    data object Idle : FillingUiState()
    data object Processing : FillingUiState()
    data object Ready : FillingUiState()
    data class Error(val message: String) : FillingUiState()
}

@HiltViewModel
class FillingViewModel @Inject constructor(
    private val formRepository: FormRepository,
    private val profileRepository: ProfileRepository,
    private val formAnalyzer: FormAnalyzer,
    private val conversationManager: ConversationManager,
    private val responseClassifier: ResponseClassifier,
    private val ttsService: TextToSpeechService,
    private val speechService: SpeechRecognitionService,
    private val preferencesManager: PreferencesManager,
    private val timeSavedTracker: TimeSavedTracker,
    private val profileLearner: ProfileLearner,
    private val auditLog: PrivacyAuditLog,
    private val inAppReviewManager: InAppReviewManager,
    private val instantMatchRepository: InstantMatchRepository,
    private val analytics: Analytics,
    private val driveExportService: com.formbuddy.android.data.drive.DriveExportService,
    private val pdfExporter: com.formbuddy.android.domain.analysis.PdfExporter,
    private val billingManager: com.formbuddy.android.data.billing.BillingManager,
    private val context: Context
) : ViewModel() {

    private val _showCelebration = MutableStateFlow(false)
    val showCelebration: StateFlow<Boolean> = _showCelebration

    private val _estimatedSeconds = MutableStateFlow(0L)
    val estimatedSeconds: StateFlow<Long> = _estimatedSeconds

    /** Editor-mode toggle; mirrors iOS `@AppStorage(.isEditorMode)`. */
    val editorMode: StateFlow<Boolean> = preferencesManager.isEditorMode
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), false)

    fun setEditorMode(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setEditorMode(enabled) }
    }

    private val _uiState = MutableStateFlow<FillingUiState>(FillingUiState.Idle)
    val uiState: StateFlow<FillingUiState> = _uiState

    private val _formTemplate = MutableStateFlow<FormTemplate?>(null)
    val formTemplate: StateFlow<FormTemplate?> = _formTemplate

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _formMode = MutableStateFlow(FormMode.CHAT)
    val formMode: StateFlow<FormMode> = _formMode

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _agentProgress = MutableStateFlow(0f)
    val agentProgress: StateFlow<Float> = _agentProgress

    private var documentData: ByteArray? = null
    private var chatSession: ChatSession? = null
    private var existingFormId: String? = null

    fun initialize(source: String, uri: String?, formId: String?) {
        viewModelScope.launch {
            _formMode.value = preferencesManager.defaultFormMode.first()

            // Load profile
            val profiles = profileRepository.getAllProfiles().first()
            _currentProfile.value = profiles.firstOrNull()

            if (formId != null) {
                existingFormId = formId
                loadExistingForm(formId)
            } else if (uri != null) {
                loadNewDocument(uri)
            }
        }
    }

    private suspend fun loadExistingForm(formId: String) {
        val entity = formRepository.getFormById(formId) ?: return
        val template = formRepository.getFormTemplate(entity)
        documentData = formRepository.getDocumentData(formId)
        _formTemplate.value = template
        initChatSession(template)
        _uiState.value = FillingUiState.Ready
    }

    private suspend fun loadNewDocument(uriString: String) {
        _isProcessing.value = true
        _uiState.value = FillingUiState.Processing

        // 1. Try the SHA-of-PDF instant-match first — popular forms get a
        //    sub-second fill instead of running the full pipeline.
        val uri = Uri.parse(uriString)
        val rawBytes = try {
            context.contentResolver.openInputStream(uri)?.readBytes()
        } catch (_: Throwable) { null }

        if (rawBytes != null) {
            documentData = rawBytes
            val instant = runCatching {
                instantMatchRepository.findByPdfBytes(rawBytes)
            }.getOrNull()
            if (instant != null) {
                _formTemplate.value = instant
                initChatSession(instant)
                postProcessTemplate(instant)
                _uiState.value = FillingUiState.Ready
                _isProcessing.value = false
                analytics.logEvent(
                    Events.ANALYSIS_INSTANT_MATCH,
                    mapOf("pages" to instant.pages.size, "fields" to instant.allFields.size)
                )
                return
            }
        }

        // 2. Otherwise enqueue the WorkManager job — it survives
        //    backgrounding, has automatic retries, and writes the
        //    finished form straight into the encrypted Room DB. The
        //    screen observes its WorkInfo state machine.
        val workName = DocumentProcessingWorker.enqueue(context, uri, expedited = true)
        analytics.logEvent(Events.ANALYSIS_STARTED, mapOf("source" to "uri"))

        viewModelScope.launch {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(workName)
                .asFlow()
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            val newId = info.outputData.getString(DocumentProcessingWorker.KEY_FORM_ID)
                            if (newId != null) {
                                existingFormId = newId
                                loadExistingForm(newId)
                                postProcessTemplate(_formTemplate.value)
                                analytics.logEvent(
                                    Events.ANALYSIS_COMPLETED,
                                    mapOf(
                                        "form_id" to newId,
                                        "fields" to (_formTemplate.value?.allFields?.size ?: 0)
                                    )
                                )
                            } else {
                                _uiState.value = FillingUiState.Error("Worker returned no formId")
                            }
                            _isProcessing.value = false
                        }
                        WorkInfo.State.FAILED -> {
                            _uiState.value = FillingUiState.Error("Document analysis failed")
                            analytics.logEvent("analysis_failed")
                            _isProcessing.value = false
                        }
                        else -> Unit
                    }
                }
        }
    }

    /** Runs after the template is loaded to apply one-shot autofill. */
    private suspend fun postProcessTemplate(template: FormTemplate?) {
        if (template == null) return
        val oneShot = preferencesManager.oneShotEnabled.first()
        if (oneShot && _currentProfile.value != null) {
            runAgentSilently(template)
        }
    }

    private suspend fun runAgentSilently(template: FormTemplate) {
        val profile = _currentProfile.value ?: return
        var changed = false
        for (field in template.allFields) {
            if (!field.isEmpty) continue
            val suggested = profile.suggestedValue(field)
                ?: profileLearner.bestSuggestion(field.fieldSubType)
            if (!suggested.isNullOrBlank()) {
                field.userValue = suggested
                field.userInputMethod = UserInputMethod.PROFILE_AUTOFILL
                changed = true
            }
        }
        if (changed) {
            _formTemplate.value = template.copy()
        }
    }

    private fun initChatSession(template: FormTemplate) {
        chatSession = ChatSession(formTemplate = template)
        conversationManager.startSession(template)
        askNextQuestion()
    }

    private fun askNextQuestion() {
        val session = chatSession ?: return
        val field = session.currentField

        if (field == null || session.isComplete) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                sender = ChatMessage.Sender.SYSTEM,
                content = "All fields have been filled! You can review and save your form.",
                associatedValue = ChatMessage.AssociatedValue.ConversationDone
            )
            return
        }

        val question = conversationManager.getQuestionForField(field)
        val message = ChatMessage(
            sender = ChatMessage.Sender.SYSTEM,
            content = question,
            associatedValue = ChatMessage.AssociatedValue.Field(field.id)
        )
        _chatMessages.value = _chatMessages.value + message

        // Check for profile suggestion
        val profile = _currentProfile.value
        if (profile != null) {
            val suggestion = profile.suggestedValue(field)
            if (suggestion != null) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    sender = ChatMessage.Sender.SYSTEM,
                    content = "Suggestion from your profile: $suggestion",
                    associatedValue = ChatMessage.AssociatedValue.SuggestedValue(suggestion, field.id)
                )
            }
        }
    }

    fun sendChatMessage(text: String) {
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = ChatMessage.Sender.USER,
            content = text
        )

        val session = chatSession ?: return
        val field = session.currentField ?: return

        val classification = responseClassifier.classify(text, field)
        if (classification == ResponseClassifier.Classification.SKIP) {
            session.currentFieldIndex++
            askNextQuestion()
            return
        }

        field.userValue = text
        field.userInputMethod = UserInputMethod.VOICE_FILLED
        _formTemplate.value = _formTemplate.value?.copy() // trigger recomposition
        viewModelScope.launch { profileLearner.recordAcceptance(field, text) }
        session.currentFieldIndex++
        askNextQuestion()
    }

    fun acceptSuggestion(value: String, fieldId: String) {
        val template = _formTemplate.value ?: return
        val field = template.allFields.find { it.id == fieldId } ?: return
        field.userValue = value
        field.userInputMethod = UserInputMethod.ACCEPTED_SUGGESTION
        _formTemplate.value = template.copy()
        chatSession?.currentFieldIndex = (chatSession?.currentFieldIndex ?: 0) + 1
        viewModelScope.launch { profileLearner.recordAcceptance(field, value) }

        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = ChatMessage.Sender.USER,
            content = value
        )
        askNextQuestion()
    }

    /** Called from the Form list when the user manually edits a field, so the
     *  learner sees the corrected value win over an earlier suggestion. */
    fun recordManualEdit(field: FormField, newValue: String) {
        viewModelScope.launch { profileLearner.recordAcceptance(field, newValue) }
    }

    /** Called from the chat when the user rejects/edits a suggestion. */
    fun recordRejection(field: FormField, suggestedValue: String) {
        viewModelScope.launch { profileLearner.recordRejection(field, suggestedValue) }
    }

    /**
     * Editor mode — adds a new user-generated TEXT field on [pageIndex] at the
     * given normalized coordinates. iOS does the same on long-press in
     * `DocumentAnalysisView` when `isEditorMode` is on.
     */
    fun addUserGeneratedField(pageIndex: Int, normalizedX: Double, normalizedY: Double) {
        val template = _formTemplate.value ?: return
        val page = template.pages.getOrNull(pageIndex) ?: return
        // 12% wide, 5% tall is a good default field box for portrait pages.
        val width = 0.18
        val height = 0.045
        val newField = FormField(
            label = "New field",
            isUserGenerated = true,
            pageIndex = pageIndex,
            boundingBox = com.formbuddy.android.data.model.BoundingBox(
                x = (normalizedX - width / 2).coerceIn(0.0, 1.0 - width),
                y = (normalizedY - height / 2).coerceIn(0.0, 1.0 - height),
                width = width,
                height = height
            )
        )
        page.fields.add(newField)
        _formTemplate.value = template.copy()
    }

    /** Editor mode — moves [field] up or down within its page. */
    fun moveField(field: FormField, delta: Int) {
        val template = _formTemplate.value ?: return
        val page = template.pages.getOrNull(field.pageIndex) ?: return
        val idx = page.fields.indexOfFirst { it.id == field.id }
        if (idx < 0) return
        val newIdx = (idx + delta).coerceIn(0, page.fields.size - 1)
        if (newIdx == idx) return
        val moved = page.fields.removeAt(idx)
        page.fields.add(newIdx, moved)
        _formTemplate.value = template.copy()
    }

    /** Editor mode — removes a user-generated field. */
    fun removeField(field: FormField) {
        val template = _formTemplate.value ?: return
        val page = template.pages.getOrNull(field.pageIndex) ?: return
        page.fields.removeAll { it.id == field.id }
        _formTemplate.value = template.copy()
    }

    fun undoLastField() {
        val session = chatSession ?: return
        if (session.currentFieldIndex > 0) {
            session.currentFieldIndex--
            val field = session.currentField
            field?.userValue = null
            field?.userInputMethod = null
            _formTemplate.value = _formTemplate.value?.copy()
        }
    }

    fun skipToEnd() {
        val session = chatSession ?: return
        val template = _formTemplate.value ?: return
        session.currentFieldIndex = template.allFields.size
        _chatMessages.value = _chatMessages.value + ChatMessage(
            sender = ChatMessage.Sender.SYSTEM,
            content = "Skipped remaining fields. You can review and save your form.",
            associatedValue = ChatMessage.AssociatedValue.ConversationDone
        )
    }

    // Agent mode
    fun runAgent() {
        viewModelScope.launch {
            val template = _formTemplate.value ?: return@launch
            val profile = _currentProfile.value ?: return@launch
            val emptyFields = template.allFields.filter { it.isEmpty }
            val totalEmpty = emptyFields.size

            emptyFields.forEachIndexed { index, field ->
                val suggestion = profile.suggestedValue(field)
                if (suggestion != null) {
                    field.userValue = suggestion
                    field.userInputMethod = UserInputMethod.PROFILE_AUTOFILL
                }
                _agentProgress.value = (index + 1).toFloat() / totalEmpty
            }

            _formTemplate.value = template.copy()
            initChatSession(template) // reinit for remaining unfilled fields
        }
    }

    // Voice
    fun startRecording() {
        _isRecording.value = true
        speechService.startListening { result ->
            _isRecording.value = false
            if (result.isNotBlank()) {
                sendChatMessage(result)
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        speechService.stopListening()
    }

    fun speakText(text: String) {
        viewModelScope.launch {
            ttsService.speak(text)
        }
    }

    fun saveForm(onSaved: (String) -> Unit) {
        viewModelScope.launch {
            val template = _formTemplate.value ?: return@launch
            val data = documentData ?: return@launch
            val id = formRepository.saveForm(template, data, existingFormId)

            // Track time saved + audit + first-save celebration trigger.
            val filled = template.completedFieldsCount
            timeSavedTracker.recordCompletion(filled)
            auditLog.log(
                PrivacyAuditLog.Category.Storage,
                destination = "room:forms/$id",
                description = "Saved form '${template.documentName}' with $filled filled fields"
            )
            if (existingFormId == null) {
                _showCelebration.value = true
            }

            // Pro: auto-export the rendered, filled PDF to the user's Drive
            // so they don't have to share-to-Drive every time.
            if (billingManager.isPro.value) {
                runCatching {
                    val filledPdf = pdfExporter.exportFilledPdf(data, template)
                    val uploadId = driveExportService.exportPdfIfEnabled(
                        pdfBytes = filledPdf.readBytes(),
                        displayName = template.documentName.ifBlank { "Form $id" }
                    )
                    if (uploadId != null) {
                        analytics.logEvent(
                            Events.FORM_EXPORTED_TO_DRIVE,
                            mapOf("form_id" to id, "drive_id" to uploadId)
                        )
                    }
                }
            }

            analytics.logEvent(
                Events.FORM_SAVED,
                mapOf("form_id" to id, "filled_fields" to filled, "is_first_save" to (existingFormId == null))
            )

            onSaved(id)
        }
    }

    fun dismissCelebration() {
        _showCelebration.value = false
    }

    suspend fun maybeAskForReview(activity: android.app.Activity) {
        inAppReviewManager.maybeRequestReview(activity)
    }

    fun setProfile(profile: Profile) {
        _currentProfile.value = profile
    }

    /** Surface the cached document bytes to UI so the Document tab can render pages. */
    fun documentBytes(): ByteArray? = documentData

    /**
     * The Form/Document tabs mutate fields directly on the [FormTemplate] (which is an
     * `@Observable` data class with mutable members). Compose only recomposes when the
     * outer state-flow value reference changes, so we publish a fresh `.copy()` after
     * any in-place edit.
     */
    fun notifyFieldChanged() {
        _formTemplate.value = _formTemplate.value?.copy()
    }

    override fun onCleared() {
        super.onCleared()
        ttsService.shutdown()
        speechService.stopListening()
    }
}
